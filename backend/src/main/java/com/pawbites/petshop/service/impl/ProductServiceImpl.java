package com.pawbites.petshop.service.impl;
import jakarta.annotation.Resource;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pawbites.petshop.service.ProductService;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.pawbites.petshop.constant.StatusConstants;
import com.pawbites.petshop.entity.Category;
import com.pawbites.petshop.entity.Product;
import com.pawbites.petshop.entity.ProductCategoryRel;
import com.pawbites.petshop.entity.ProductTag;
import com.pawbites.petshop.entity.ProductTagRel;
import com.pawbites.petshop.exception.BusinessException;
import com.pawbites.petshop.mapper.CategoryMapper;
import com.pawbites.petshop.mapper.ProductCategoryRelMapper;
import com.pawbites.petshop.mapper.ProductMapper;
import com.pawbites.petshop.mapper.ProductTagMapper;
import com.pawbites.petshop.mapper.ProductTagRelMapper;
import com.pawbites.petshop.result.PageResult;
import com.pawbites.petshop.vo.product.ProductVO;

/**
 * 商品服务实现
 * <p>listProducts 先按分类 code 查有效分类并经 product_category_rel 解析出商品 id 集合，
 * 再组合状态/分类/关键字（名称、口味、宠物类型 like，通配符经 escapeLike 转义）条件分页查询，按评分降序，
 * 页码 clamp 到 [1, 10000]（防深分页）、size 上限 100；enrichProducts 批量装载分类 code 与标签名，避免逐条目查询。</p>
 */
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

  /** 分类 Mapper（按 code 解析分类） */
  @Resource
  private CategoryMapper categoryMapper;
  /** 商品-分类关联 Mapper（分类过滤与分类 code 装配） */
  @Resource
  private ProductCategoryRelMapper productCategoryRelMapper;
  /** 商品标签 Mapper（标签名装配） */
  @Resource
  private ProductTagMapper productTagMapper;
  /** 商品-标签关联 Mapper（标签名装配） */
  @Resource
  private ProductTagRelMapper productTagRelMapper;

  @Override
  public PageResult<ProductVO> listProducts(long page, long size, String categoryCode, String keyword) {
    // 页码 clamp 到 [1, 10000]，防止深分页时偏移量溢出或拖垮查询
    long safePage = Math.min(Math.max(page, 1), 10000L);
    // 关键字转义 like 通配符后再参与匹配，避免 %、_、\ 被当作通配符解释
    String likeKeyword = escapeLike(keyword);
    List<Long> categoryProductIds = null;
    if (StringUtils.hasText(categoryCode) && !"all".equalsIgnoreCase(categoryCode)) {
      Category category = categoryMapper.selectOne(Wrappers.<Category>lambdaQuery()
          .eq(Category::getCode, categoryCode)
          .eq(Category::getStatus, StatusConstants.ACTIVE)
          .last("LIMIT 1"));
      if (category == null) {
        return new PageResult<>(List.of(), 0, safePage, size);
      }
      categoryProductIds = productCategoryRelMapper.selectList(Wrappers.<ProductCategoryRel>lambdaQuery()
              .eq(ProductCategoryRel::getCategoryId, category.getId()))
          .stream()
          .map(ProductCategoryRel::getProductId)
          .distinct()
          .toList();
      if (categoryProductIds.isEmpty()) {
        return new PageResult<>(List.of(), 0, safePage, size);
      }
    }

    IPage<Product> productPage = baseMapper.selectPage(
        Page.of(safePage, Math.min(Math.max(size, 1), 100)),
        Wrappers.<Product>lambdaQuery()
            .eq(Product::getStatus, StatusConstants.ACTIVE)
            .in(categoryProductIds != null, Product::getId, categoryProductIds)
            .and(StringUtils.hasText(likeKeyword), wrapper -> wrapper
                .like(Product::getName, likeKeyword)
                .or()
                .like(Product::getFlavor, likeKeyword)
                .or()
                .like(Product::getPetType, likeKeyword))
            .orderByDesc(Product::getRating)
            .orderByDesc(Product::getId)
    );

    return new PageResult<>(
        enrichProducts(productPage.getRecords()),
        productPage.getTotal(),
        productPage.getCurrent(),
        productPage.getSize()
    );
  }

  @Override
  public ProductVO getProduct(Long id) {
    Product product = baseMapper.selectById(id);
    if (product == null || !StatusConstants.ACTIVE.equals(product.getStatus())) {
      throw new BusinessException(404, "商品不存在或已下架");
    }
    return enrichProducts(List.of(product)).getFirst();
  }

  @Override
  public List<ProductVO> enrichProducts(List<Product> products) {
    if (products == null || products.isEmpty()) {
      return List.of();
    }

    List<Long> productIds = products.stream().map(Product::getId).toList();
    Map<Long, List<String>> categoryMap = loadCategoryCodes(productIds);
    Map<Long, List<String>> tagMap = loadTagNames(productIds);

    return products.stream()
        .map(product -> new ProductVO(
            product.getId(),
            product.getSku(),
            product.getName(),
            product.getPetType(),
            product.getFlavor(),
            product.getPrice(),
            product.getRating(),
            product.getStock(),
            product.getImageUrl(),
            product.getDescription(),
            categoryMap.getOrDefault(product.getId(), List.of()),
            tagMap.getOrDefault(product.getId(), List.of())
        ))
        .toList();
  }

  /** 批量装载商品 → 分类 code 列表映射（经关联表分组，过滤无效分类） */
  private Map<Long, List<String>> loadCategoryCodes(List<Long> productIds) {
    List<ProductCategoryRel> rels = productCategoryRelMapper.selectList(Wrappers.<ProductCategoryRel>lambdaQuery()
        .in(ProductCategoryRel::getProductId, productIds));
    if (rels.isEmpty()) {
      return Collections.emptyMap();
    }

    Set<Long> categoryIds = rels.stream().map(ProductCategoryRel::getCategoryId).collect(Collectors.toSet());
    Map<Long, String> categories = categoryMapper.selectBatchIds(categoryIds).stream()
        .collect(Collectors.toMap(Category::getId, Category::getCode));
    return rels.stream()
        .collect(Collectors.groupingBy(
            ProductCategoryRel::getProductId,
            Collectors.mapping(rel -> categories.get(rel.getCategoryId()), Collectors.filtering(StringUtils::hasText, Collectors.toList()))
        ));
  }

  /** 批量装载商品 → 标签名列表映射（经关联表分组，过滤无效标签） */
  private Map<Long, List<String>> loadTagNames(List<Long> productIds) {
    List<ProductTagRel> rels = productTagRelMapper.selectList(Wrappers.<ProductTagRel>lambdaQuery()
        .in(ProductTagRel::getProductId, productIds));
    if (rels.isEmpty()) {
      return Collections.emptyMap();
    }

    Set<Long> tagIds = rels.stream().map(ProductTagRel::getTagId).collect(Collectors.toSet());
    Map<Long, String> tags = productTagMapper.selectBatchIds(tagIds).stream()
        .collect(Collectors.toMap(ProductTag::getId, ProductTag::getName));
    return rels.stream()
        .collect(Collectors.groupingBy(
            ProductTagRel::getProductId,
            Collectors.mapping(rel -> tags.get(rel.getTagId()), Collectors.filtering(StringUtils::hasText, Collectors.toList()))
        ));
  }

  /** 转义 like 通配符：在 \、%、_ 前加反斜杠，防止用户关键字被当作通配符解释 */
  private static String escapeLike(String keyword) {
    if (!StringUtils.hasText(keyword)) {
      return keyword;
    }
    return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
