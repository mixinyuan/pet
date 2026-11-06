package com.pawbites.petshop.service.impl.admin;
import jakarta.annotation.Resource;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pawbites.petshop.service.admin.AdminTagService;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.pawbites.petshop.constant.StatusConstants;
import com.pawbites.petshop.dto.admin.TagCreateRequest;
import com.pawbites.petshop.dto.admin.TagUpdateRequest;
import com.pawbites.petshop.entity.Product;
import com.pawbites.petshop.entity.ProductTag;
import com.pawbites.petshop.entity.ProductTagRel;
import com.pawbites.petshop.exception.BusinessException;
import com.pawbites.petshop.mapper.ProductMapper;
import com.pawbites.petshop.mapper.ProductTagMapper;
import com.pawbites.petshop.mapper.ProductTagRelMapper;
import com.pawbites.petshop.vo.admin.AdminTagVO;

/**
 * 管理端商品标签服务实现
 * <p>listTags 返回全部状态标签（含停用）并附带引用商品数——引用数只统计未删除商品
 * （关联行经 selectBatchIds 反查商品，逻辑删除自动过滤）；
 * create/updateTag 校验名称全局唯一；delete 被商品引用时拒绝，否则走逻辑删除。</p>
 */
@Service
public class AdminTagServiceImpl extends ServiceImpl<ProductTagMapper, ProductTag> implements AdminTagService {

  /** 商品-标签关联 Mapper（引用商品数统计） */
  @Resource
  private ProductTagRelMapper productTagRelMapper;
  /** 商品 Mapper（引用数统计时反查未删除商品） */
  @Resource
  private ProductMapper productMapper;

  @Override
  public List<AdminTagVO> listTags() {
    List<ProductTag> tags = baseMapper.selectList(Wrappers.<ProductTag>lambdaQuery()
        .orderByAsc(ProductTag::getId));
    Map<Long, Long> productCounts = countLiveProductsByTag();
    return tags.stream()
        .map(tag -> toVO(tag, productCounts.getOrDefault(tag.getId(), 0L)))
        .toList();
  }

  @Transactional
  @Override
  public AdminTagVO createTag(TagCreateRequest request) {
    String name = request.name().trim();
    ensureNameUnique(name, null);
    ProductTag tag = new ProductTag();
    tag.setName(name);
    tag.setStatus(StatusConstants.ACTIVE);
    try {
      baseMapper.insert(tag);
    } catch (DuplicateKeyException exception) {
      // 预校验之后仍可能撞唯一键（如与已逻辑删除标签的名称冲突——唯一索引不区分 deleted），统一给出明确报错
      throw new BusinessException(409, "标签名称已存在：" + name);
    }
    return toVO(tag, 0L);
  }

  @Transactional
  @Override
  public AdminTagVO updateTag(Long id, TagUpdateRequest request) {
    ProductTag existing = requireTag(id);
    String name = request.name().trim();
    // 名称未变化时跳过唯一性校验与更新，避免无谓写库
    if (!existing.getName().equals(name)) {
      ensureNameUnique(name, id);
      // 只更新名称列；updated_at 由数据库 ON UPDATE CURRENT_TIMESTAMP 自动维护
      baseMapper.update(null, Wrappers.<ProductTag>lambdaUpdate()
          .eq(ProductTag::getId, id)
          .set(ProductTag::getName, name));
    }
    return getTag(id);
  }

  @Transactional
  @Override
  public void updateStatus(Long id, String status) {
    requireTag(id);
    // 只更新状态列；updated_at 由数据库 ON UPDATE CURRENT_TIMESTAMP 自动维护
    baseMapper.update(null, Wrappers.<ProductTag>lambdaUpdate()
        .eq(ProductTag::getId, id)
        .set(ProductTag::getStatus, status));
  }

  @Transactional
  @Override
  public void deleteTag(Long id) {
    requireTag(id);
    List<ProductTagRel> rels = productTagRelMapper.selectList(Wrappers.<ProductTagRel>lambdaQuery()
        .eq(ProductTagRel::getTagId, id));
    if (!rels.isEmpty()) {
      Set<Long> productIds = rels.stream().map(ProductTagRel::getProductId).collect(Collectors.toSet());
      Long liveCount = productMapper.selectCount(Wrappers.<Product>lambdaQuery()
          .in(Product::getId, productIds));
      if (liveCount != null && liveCount > 0) {
        throw new BusinessException(400, "标签已被 " + liveCount + " 个商品引用，请先解除引用后再删除");
      }
    }
    // 主表逻辑删除（deleted=1）；残留关联行由商品 VO 装配逻辑按标签存在性过滤，不影响展示
    baseMapper.deleteById(id);
  }

  /** 按 id 查询标签（含停用），不存在或已删除时抛 404 */
  private ProductTag requireTag(Long id) {
    ProductTag tag = baseMapper.selectById(id);
    if (tag == null) {
      throw new BusinessException(404, "标签不存在");
    }
    return tag;
  }

  /** 按 id 查询标签并带引用商品数（含停用） */
  private AdminTagVO getTag(Long id) {
    ProductTag tag = requireTag(id);
    List<ProductTagRel> rels = productTagRelMapper.selectList(Wrappers.<ProductTagRel>lambdaQuery()
        .eq(ProductTagRel::getTagId, id));
    long productCount = 0L;
    if (!rels.isEmpty()) {
      Set<Long> productIds = rels.stream().map(ProductTagRel::getProductId).collect(Collectors.toSet());
      Long liveCount = productMapper.selectCount(Wrappers.<Product>lambdaQuery()
          .in(Product::getId, productIds));
      productCount = liveCount == null ? 0L : liveCount;
    }
    return toVO(tag, productCount);
  }

  /** 标签名称唯一性预校验（excludeId 用于改名时排除自身），重复抛 409 */
  private void ensureNameUnique(String name, Long excludeId) {
    Long count = baseMapper.selectCount(Wrappers.<ProductTag>lambdaQuery()
        .eq(ProductTag::getName, name)
        .ne(excludeId != null, ProductTag::getId, excludeId));
    if (count != null && count > 0) {
      throw new BusinessException(409, "标签名称已存在：" + name);
    }
  }

  /** 统计每个标签的引用商品数（仅统计未删除商品；关联表与商品表的逻辑删除条件均由 MP 自动追加） */
  private Map<Long, Long> countLiveProductsByTag() {
    List<ProductTagRel> rels = productTagRelMapper.selectList(Wrappers.emptyWrapper());
    if (rels.isEmpty()) {
      return Collections.emptyMap();
    }
    Set<Long> productIds = rels.stream().map(ProductTagRel::getProductId).collect(Collectors.toSet());
    Set<Long> liveProductIds = productMapper.selectBatchIds(productIds).stream()
        .map(Product::getId)
        .collect(Collectors.toSet());
    return rels.stream()
        .filter(rel -> liveProductIds.contains(rel.getProductId()))
        .collect(Collectors.groupingBy(ProductTagRel::getTagId, Collectors.counting()));
  }

  /** 标签实体 → 管理端视图 */
  private AdminTagVO toVO(ProductTag tag, long productCount) {
    return new AdminTagVO(tag.getId(), tag.getName(), tag.getStatus(), productCount);
  }
}
