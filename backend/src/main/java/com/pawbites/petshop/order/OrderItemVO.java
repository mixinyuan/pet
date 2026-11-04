package com.pawbites.petshop.order;

import java.math.BigDecimal;

public record OrderItemVO(
    Long productId,
    String productName,
    String productImage,
    BigDecimal price,
    Integer quantity,
    BigDecimal subtotal
) {
}
