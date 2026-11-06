package com.pawbites.petshop.service.impl.admin;
import jakarta.annotation.Resource;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pawbites.petshop.service.admin.AdminCategoryService;

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
import com.pawbites.petshop.dto.admin.CategoryCreateRequest;
import com.pawbites.petshop.dto.admin.CategoryUpdateRequest;
import com.pawbites.petshop.entity.Category;
import com.pawbites.petshop.entity.Product;
import com.pawbites.petshop.entity.ProductCategoryRel;
import com.pawbites.petshop.exception.BusinessException;
import com.pawbites.petshop.mapper.CategoryMapper;
import com.pawbites.petshop.mapper.ProductCategoryRelMapper;
import com.pawbites.petshop.mapper.ProductMapper;
import com.pawbites.petshop.vo.admin.AdminCategoryVO;

/**
 * 管理端分类服务实现
 * <p>listCategories 返回全部状态分类（含停用）并附带引用商品数——引用数只统计未删除商品
 * （关联行经 selectBatchIds 反查商品，逻辑删除自动过滤）；
 * create 校验编码唯一且不允许 all；update 不允许改编码（被引用后改码会断数据）；
 * delete 对系统内置 all 与被引用分类做拒绝校验后走逻辑删除。</p>
 */
@Service
public class AdminCategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements AdminCategoryService {

  /** 系统内置分类编码（V1 种子数据中的「全部」分类，不允许作为新编码、不允许删除） */
  private static final String BUILTIN_CODE_ALL = "all";

  /** 商品-分类关联 Mapper（引用商品数统计） */
  @Resource
  private ProductCategoryRelMapper productCategoryRelMapper;
  /** 商品 Mapper（引用数统计时反查未删除商品） */
  @Resource
  private ProductMapper productMapper;

  @Override
  public List<AdminCategoryVO> listCategories() {
    List<Category> categories = baseMapper.selectList(Wrappers.<Category>lambdaQuery()
        .orderByAsc(Category::getSortOrder)
        .orderByAsc(Category::getId));
    Map<Long, Long> productCounts = countLiveProductsByCategory();
    return categories.stream()
        .map(category -> toVO(category, productCounts.getOrDefault(category.getId(), 0L)))
        .toList();
  }

  @Override
  public AdminCategoryVO getCategory(Long id) {
    Category category = requireCategory(id);
    return toVO(category, countLiveProducts(category.getId()));
  }

  @Transactional
  @Override
  public AdminCategoryVO createCategory(CategoryCreateRequest request) {
    String code = request.code().trim();
    if (BUILTIN_CODE_ALL.equalsIgnoreCase(code)) {
      throw new BusinessException(400, "all 为系统内置分类编码，不允许使用");
    }
    ensureCodeUnique(code);
    Category category = new Category();
    category.setCode(code);
    category.setName(request.name().trim());
    category.setDescription(StringUtils.hasText(request.description()) ? request.description().trim() : null);
    category.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
    category.setStatus(StatusConstants.ACTIVE);
    try {
      baseMapper.insert(category);
    } catch (DuplicateKeyException exception) {
      // 预校验之后仍可能撞唯一键（如与已逻辑删除分类的编码冲突——唯一索引不区分 deleted），统一给出明确报错
      throw new BusinessException(409, "分类编码已存在：" + code);
    }
    return toVO(category, 0L);
  }

  @Transactional
  @Override
  public AdminCategoryVO updateCategory(Long id, CategoryUpdateRequest request) {
    Category existing = requireCategory(id);
    // 编码不允许修改：传入且与现值不同即报错，原样传回视为不改
    if (StringUtils.hasText(request.code()) && !existing.getCode().equals(request.code().trim())) {
      throw new BusinessException(400, "分类编码不允许修改（当前编码：" + existing.getCode() + "）");
    }
    if (request.name() != null && !StringUtils.hasText(request.name())) {
      throw new BusinessException(400, "分类名称不能为空");
    }

    // 只 SET 发生了修改的列；updated_at 由数据库 ON UPDATE CURRENT_TIMESTAMP 自动维护
    baseMapper.update(null, Wrappers.<Category>lambdaUpdate()
        .eq(Category::getId, id)
        .set(request.name() != null, Category::getName, request.name().trim())
        .set(request.description() != null, Category::getDescription,
            StringUtils.hasText(request.description()) ? request.description().trim() : null)
        .set(request.sortOrder() != null, Category::getSortOrder, request.sortOrder())
        .set(request.status() != null, Category::getStatus, request.status()));
    return getCategory(id);
  }

  @Transactional
  @Override
  public void updateStatus(Long id, String status) {
    requireCategory(id);
    // 只更新状态列；updated_at 由数据库 ON UPDATE CURRENT_TIMESTAMP 自动维护
    baseMapper.update(null, Wrappers.<Category>lambdaUpdate()
        .eq(Category::getId, id)
        .set(Category::getStatus, status));
  }

  @Transactional
  @Override
  public void deleteCategory(Long id) {
    Category category = requireCategory(id);
    if (BUILTIN_CODE_ALL.equals(category.getCode())) {
      throw new BusinessException(400, "all 为系统内置分类，不允许删除");
    }
    long productCount = countLiveProducts(id);
    if (productCount > 0) {
      throw new BusinessException(400, "分类已被 " + productCount + " 个商品引用，请先解除引用后再删除");
    }
    // 主表逻辑删除（deleted=1）；残留关联行由 C 端装配逻辑按分类存在性过滤，不影响展示
    baseMapper.deleteById(id);
  }

  /** 按 id 查询分类（含停用），不存在或已删除时抛 404 */
  private Category requireCategory(Long id) {
    Category category = baseMapper.selectById(id);
    if (category == null) {
      throw new BusinessException(404, "分类不存在");
    }
    return category;
  }

  /** 分类编码唯一性预校验，重复抛 409 */
  private void ensureCodeUnique(String code) {
    Long count = baseMapper.selectCount(Wrappers.<Category>lambdaQuery()
        .eq(Category::getCode, code));
    if (count != null && count > 0) {
      throw new BusinessException(409, "分类编码已存在：" + code);
    }
  }

  /** 统计每个分类的引用商品数（仅统计未删除商品；关联表与商品表的逻辑删除条件均由 MP 自动追加） */
  private Map<Long, Long> countLiveProductsByCategory() {
    List<ProductCategoryRel> rels = productCategoryRelMapper.selectList(Wrappers.emptyWrapper());
    if (rels.isEmpty()) {
      return Collections.emptyMap();
    }
    Set<Long> productIds = rels.stream().map(ProductCategoryRel::getProductId).collect(Collectors.toSet());
    Set<Long> liveProductIds = productMapper.selectBatchIds(productIds).stream()
        .map(Product::getId)
        .collect(Collectors.toSet());
    return rels.stream()
        .filter(rel -> liveProductIds.contains(rel.getProductId()))
        .collect(Collectors.groupingBy(ProductCategoryRel::getCategoryId, Collectors.counting()));
  }

  /** 统计单个分类的引用商品数（仅统计未删除商品） */
  private long countLiveProducts(Long categoryId) {
    List<ProductCategoryRel> rels = productCategoryRelMapper.selectList(Wrappers.<ProductCategoryRel>lambdaQuery()
        .eq(ProductCategoryRel::getCategoryId, categoryId));
    if (rels.isEmpty()) {
      return 0L;
    }
    Set<Long> productIds = rels.stream().map(ProductCategoryRel::getProductId).collect(Collectors.toSet());
    Long liveCount = productMapper.selectCount(Wrappers.<Product>lambdaQuery()
        .in(Product::getId, productIds));
    return liveCount == null ? 0L : liveCount;
  }

  /** 分类实体 → 管理端视图 */
  private AdminCategoryVO toVO(Category category, long productCount) {
    return new AdminCategoryVO(
        category.getId(),
        category.getCode(),
        category.getName(),
        category.getDescription(),
        category.getSortOrder(),
        category.getStatus(),
        productCount
    );
  }
}
