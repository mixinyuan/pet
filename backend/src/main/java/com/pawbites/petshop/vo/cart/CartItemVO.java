package com.pawbites.petshop.vo.cart;

import java.math.BigDecimal;

public record CartItemVO(
    Long productId,
    String productName,
    String productImage,
    BigDecimal price,
    Integer quantity,
    BigDecimal subtotal,
    Boolean checked
) {
}
