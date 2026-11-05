package com.pawbites.petshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pawbites.petshop.entity.UserFavorite;

import java.util.List;

import com.pawbites.petshop.vo.favorite.FavoriteVO;

/**
 * 收藏服务
 * <p>管理登录用户的商品收藏：查询收藏列表（联查商品卡片信息）、收藏与取消收藏。
 * 游客不可收藏，未登录调用一律返回 401 语义。</p>
 */
public interface FavoriteService extends IService<UserFavorite> {

  /**
   * 查询当前用户的收藏列表
   * <p>按收藏 id 倒序（最新收藏在前），联查 product 返回商品卡片字段；
   * 商品已被删除的收藏条目跳过不返回。</p>
   *
   * @param userId 登录用户 id
   * @return 收藏列表（含商品 id/名称/图片/价格/宠物类型/口味/评分）
   * @throws BusinessException 未登录（401）时抛出
   */
  List<FavoriteVO> listFavorites(Long userId);

  /**
   * 收藏指定商品
   * <p>重复收藏幂等：已收藏时直接返回成功，不产生新记录。</p>
   *
   * @param userId    登录用户 id
   * @param productId 商品 id
   * @throws BusinessException 未登录（401）、商品不存在（404）时抛出
   */
  void addFavorite(Long userId, Long productId);

  /**
   * 取消收藏指定商品
   * <p>幂等：未收藏时同样返回成功。</p>
   *
   * @param userId    登录用户 id
   * @param productId 商品 id
   * @throws BusinessException 未登录（401）时抛出
   */
  void removeFavorite(Long userId, Long productId);
}
