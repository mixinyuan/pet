package com.pawbites.petshop.review;

import java.math.BigDecimal;

public record ReviewVO(
    Long id,
    Long productId,
    String buyerName,
    String petName,
    String petBreed,
    String content,
    String avatarUrl,
    String purchasedProduct,
    BigDecimal rating
) {
}
