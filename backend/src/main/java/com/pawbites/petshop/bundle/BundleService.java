package com.pawbites.petshop.bundle;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pawbites.petshop.common.config.StatusConstants;
import com.pawbites.petshop.product.Product;
import com.pawbites.petshop.product.ProductMapper;
import com.pawbites.petshop.product.ProductSummaryVO;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class BundleService {

  private final BundleMapper bundleMapper;
  private final BundleItemMapper bundleItemMapper;
  private final ProductMapper productMapper;

  public BundleService(BundleMapper bundleMapper, BundleItemMapper bundleItemMapper, ProductMapper productMapper) {
    this.bundleMapper = bundleMapper;
    this.bundleItemMapper = bundleItemMapper;
    this.productMapper = productMapper;
  }

  public List<BundleVO> listActiveBundles() {
    List<Bundle> bundles = bundleMapper.selectList(Wrappers.<Bundle>lambdaQuery()
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

  private ProductSummaryVO toProductSummary(BundleItem item, Product product) {
    if (product == null) {
      return null;
    }
    return new ProductSummaryVO(product.getId(), product.getName(), product.getPrice(), product.getImageUrl(), item.getQuantity());
  }
}
