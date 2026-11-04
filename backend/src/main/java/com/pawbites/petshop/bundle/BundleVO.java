package com.pawbites.petshop.bundle;

import com.pawbites.petshop.product.ProductSummaryVO;
import java.math.BigDecimal;
import java.util.List;

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
