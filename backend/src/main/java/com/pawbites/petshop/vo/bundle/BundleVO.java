package com.pawbites.petshop.vo.bundle;

import java.math.BigDecimal;
import java.util.List;

import com.pawbites.petshop.vo.product.ProductSummaryVO;

public record BundleVO(
    Long id,
    String name,
    String description,
    BigDecimal price,
    BigDecimal originalPrice,
    String discountLabel,
    List<ProductSummaryVO> items
) {
}
