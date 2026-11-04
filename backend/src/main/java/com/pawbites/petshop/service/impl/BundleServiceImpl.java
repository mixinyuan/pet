package com.pawbites.petshop.service.impl;
import jakarta.annotation.Resource;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pawbites.petshop.service.BundleService;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

import com.pawbites.petshop.constant.StatusConstants;
import com.pawbites.petshop.entity.Bundle;
import com.pawbites.petshop.entity.BundleItem;
import com.pawbites.petshop.entity.Product;
import com.pawbites.petshop.mapper.BundleItemMapper;
import com.pawbites.petshop.mapper.BundleMapper;
import com.pawbites.petshop.mapper.ProductMapper;
import com.pawbites.petshop.vo.bundle.BundleVO;
import com.pawbites.petshop.vo.product.ProductSummaryVO;

/**
 * 套餐服务实现
 * <p>一次性批量读取全部有效套餐、套餐条目与内含商品，再按套餐分组装配，
 * 避免逐套餐查询；商品已不存在的条目在装配时被过滤。</p>
 */
@Service
public class BundleServiceImpl extends ServiceImpl<BundleMapper, Bundle> implements BundleService {

  /** 套餐条目 Mapper */
  @Resource
  private BundleItemMapper bundleItemMapper;
  /** 商品 Mapper（汇总套餐内含商品） */
  @Resource
  private ProductMapper productMapper;

  @Override
  public List<BundleVO> listActiveBundles() {
    List<Bundle> bundles = baseMapper.selectList(Wrappers.<Bundle>lambdaQuery()
        .eq(Bundle::getStatus, StatusConstants.ACTIVE)
        .orderByAsc(Bundle::getSortOrder)
        .orderByAsc(Bundle::getId));
    if (bundles.isEmpty()) {
      return List.of();
    }

    List<Long> bundleIds = bundles.stream().map(Bundle::getId).toList();
    List<BundleItem> items = bundleItemMapper.selectList(Wrappers.<BundleItem>lambdaQuery()
        .in(BundleItem::getBundleId, bundleIds));
    if (items.isEmpty()) {
      return bundles.stream()
          .map(bundle -> new BundleVO(
              bundle.getId(),
              bundle.getName(),
              bundle.getDescription(),
              bundle.getPrice(),
              bundle.getOriginalPrice(),
              bundle.getDiscountLabel(),
              List.of()
          ))
          .toList();
    }

    Map<Long, Product> productMap = productMapper.selectBatchIds(
            items.stream().map(BundleItem::getProductId).distinct().toList()
        )
        .stream()
        .collect(Collectors.toMap(Product::getId, product -> product));
    Map<Long, List<BundleItem>> itemMap = items.stream()
        .collect(Collectors.groupingBy(BundleItem::getBundleId));

    return bundles.stream()
        .map(bundle -> new BundleVO(
            bundle.getId(),
            bundle.getName(),
            bundle.getDescription(),
            bundle.getPrice(),
            bundle.getOriginalPrice(),
            bundle.getDiscountLabel(),
            itemMap.getOrDefault(bundle.getId(), List.of()).stream()
                .map(item -> toProductSummary(item, productMap.get(item.getProductId())))
                .filter(Objects::nonNull)
                .toList()
        ))
        .toList();
  }

  /** 套餐条目 + 商品 → 内含商品汇总视图（商品不存在时返回 null 以便过滤） */
  private ProductSummaryVO toProductSummary(BundleItem item, Product product) {
    if (product == null) {
      return null;
    }
    return new ProductSummaryVO(product.getId(), product.getName(), product.getPrice(), product.getImageUrl(), item.getQuantity());
  }
}
