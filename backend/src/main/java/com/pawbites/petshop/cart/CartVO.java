package com.pawbites.petshop.cart;

import java.math.BigDecimal;
import java.util.List;

public record CartVO(
    List<CartItemVO> items,
    Integer count,
    BigDecimal totalAmount
) {
}
