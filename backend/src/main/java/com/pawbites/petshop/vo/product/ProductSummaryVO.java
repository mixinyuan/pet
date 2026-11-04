package com.pawbites.petshop.vo.product;

import java.math.BigDecimal;

public record ProductSummaryVO(
    Long id,
    String name,
    BigDecimal price,
    String imageUrl,
    Integer quantity
) {
}
