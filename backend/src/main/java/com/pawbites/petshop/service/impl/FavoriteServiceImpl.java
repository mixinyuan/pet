package com.pawbites.petshop.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pawbites.petshop.service.FavoriteService;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawbites.petshop.entity.Product;
import com.pawbites.petshop.entity.UserFavorite;
import com.pawbites.petshop.exception.BusinessException;
import com.pawbites.petshop.mapper.ProductMapper;
import com.pawbites.petshop.mapper.UserFavoriteMapper;
import com.pawbites.petshop.vo.favorite.FavoriteVO;

/**
 * 收藏服务实现
 * <p>listFavorites 先取收藏关系（id 倒序）再批量联查商品（消除 N+1），商品已被删除的
 * 收藏条目跳过；addFavorite 校验商品存在后插入，uk_user_favorite 唯一键 + 先查后插
 * 保证重复收藏幂等；removeFavorite 按幂等语义静默删除。所有入口先校验登录（401）。</p>
 */
@Service
public class FavoriteServiceImpl extends ServiceImpl<UserFavoriteMapper, UserFavorite> implements FavoriteService {

  /** 商品 Mapper（收藏时校验商品存在、列表时联查商品卡片信息） */
  private final ProductMapper productMapper;

  /** 构造注入商品 Mapper */
  public FavoriteServiceImpl(ProductMapper productMapper) {
    this.productMapper = productMapper;
  }

  @Override
  public List<FavoriteVO> listFavorites(Long userId) {
    requireLogin(userId);
    List<UserFavorite> favorites = baseMapper.selectList(Wrappers.<UserFavorite>lambdaQuery()
        .eq(UserFavorite::getUserId, userId)
        .orderByDesc(UserFavorite::getId));
    if (favorites.isEmpty()) {
      return List.of();
    }

    Map<Long, Product> productMap = productMapper.selectBatchIds(
            favorites.stream().map(UserFavorite::getProductId).distinct().toList()
        )
        .stream()
        .collect(Collectors.toMap(Product::getId, Function.identity()));
    // 商品已被删除（逻辑删除后 selectBatchIds 查不到）的收藏条目跳过，不向前端输出空卡片
    return favorites.stream()
        .filter(favorite -> productMap.containsKey(favorite.getProductId()))
        .map(favorite -> toFavoriteVO(favorite, productMap.get(favorite.getProductId())))
        .toList();
  }

  @Transactional
  @Override
  public void addFavorite(Long userId, Long productId) {
    requireLogin(userId);
    if (productMapper.selectById(productId) == null) {
      throw new BusinessException(404, "商品不存在");
    }

    // 幂等：已收藏直接返回成功，不重复插入
    Long existing = baseMapper.selectCount(Wrappers.<UserFavorite>lambdaQuery()
        .eq(UserFavorite::getUserId, userId)
        .eq(UserFavorite::getProductId, productId));
    if (existing != null && existing > 0) {
      return;
    }

    UserFavorite favorite = new UserFavorite();
    favorite.setUserId(userId);
    favorite.setProductId(productId);
    // create_time 留空，由数据库 DEFAULT CURRENT_TIMESTAMP 填充
    try {
      baseMapper.insert(favorite);
    } catch (DuplicateKeyException exception) {
      // 并发双击撞 uk_user_favorite 唯一键：同样视为收藏成功，保持幂等语义
      return;
    }
  }

  @Transactional
  @Override
  public void removeFavorite(Long userId, Long productId) {
    requireLogin(userId);
    // 幂等：未收藏时删除影响 0 行，同样视为成功
    baseMapper.delete(Wrappers.<UserFavorite>lambdaQuery()
        .eq(UserFavorite::getUserId, userId)
        .eq(UserFavorite::getProductId, productId));
  }

  /** 校验登录：未登录抛 401 业务异常（SecurityConfig 已拦截，此处防御性兜底） */
  private void requireLogin(Long userId) {
    if (userId == null) {
      throw new BusinessException(401, "请先登录");
    }
  }

  /** 收藏关系 + 商品实体组装收藏视图 */
  private FavoriteVO toFavoriteVO(UserFavorite favorite, Product product) {
    return new FavoriteVO(
        favorite.getId(),
        product.getId(),
        product.getName(),
        product.getImageUrl(),
        product.getPrice(),
        product.getPetType(),
        product.getFlavor(),
        product.getRating(),
        favorite.getCreateTime()
    );
  }
}
