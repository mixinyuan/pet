package com.pawbites.petshop.service.impl;
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

@Service
public class ProductServiceImpl implements ProductService {

  private final ProductMapper productMapper;
  private final CategoryMapper categoryMapper;
  private final ProductCategoryRelMapper productCategoryRelMapper;
  private final ProductTagMapper productTagMapper;
  private final ProductTagRelMapper productTagRelMapper;

  public ProductServiceImpl(
      ProductMapper productMapper,
      CategoryMapper categoryMapper,
      ProductCategoryRelMapper productCategoryRelMapper,
      ProductTagMapper productTagMapper,
      ProductTagRelMapper productTagRelMapper
  ) {
    this.productMapper = productMapper;
    this.categoryMapper = categoryMapper;
    this.productCategoryRelMapper = productCategoryRelMapper;
    this.productTagMapper = productTagMapper;
    this.productTagRelMapper = productTagRelMapper;
  }

  @Override
  public PageResult<ProductVO> listProducts(long page, long size, String categoryCode, String keyword) {
    List<Long> categoryProductIds = null;
    if (StringUtils.hasText(categoryCode) && !"all".equalsIgnoreCase(categoryCode)) {
      Category category = categoryMapper.selectOne(Wrappers.<Category>lambdaQuery()
          .eq(Category::getCode, categoryCode)
          .eq(Category::getStatus, StatusConstants.ACTIVE)
          .last("LIMIT 1"));
      if (category == null) {
        return new PageResult<>(List.of(), 0, page, size);
      }
      categoryProductIds = productCategoryRelMapper.selectList(Wrappers.<ProductCategoryRel>lambdaQuery()
              .eq(ProductCategoryRel::getCategoryId, category.getId()))
          .stream()
          .map(ProductCategoryRel::getProductId)
          .distinct()
          .toList();
      if (categoryProductIds.isEmpty()) {
        return new PageResult<>(List.of(), 0, page, size);
      }
    }

    IPage<Product> productPage = productMapper.selectPage(
        Page.of(Math.max(page, 1), Math.min(Math.max(size, 1), 100)),
        Wrappers.<Product>lambdaQuery()
            .eq(Product::getStatus, StatusConstants.ACTIVE)
            .in(categoryProductIds != null, Product::getId, categoryProductIds)
            .and(StringUtils.hasText(keyword), wrapper -> wrapper
                .like(Product::getName, keyword)
                .or()
                .like(Product::getFlavor, keyword)
                .or()
                .like(Product::getPetType, keyword))
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
    Product product = productMapper.selectById(id);
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
}
