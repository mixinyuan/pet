package com.pawbites.petshop.product;

import java.math.BigDecimal;
import java.util.List;

public record ProductVO(
    Long id,
    String sku,
    String name,
    String petType,
    String flavor,
    BigDecimal price,
    BigDecimal rating,
    Integer stock,
    String imageUrl,
    String description,
    List<String> categories,
    List<String> tags
) {
}
