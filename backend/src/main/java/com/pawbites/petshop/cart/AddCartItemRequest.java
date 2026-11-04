package com.pawbites.petshop.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddCartItemRequest(
    @NotNull Long productId,
    @Min(1) Integer quantity
) {
  public int normalizedQuantity() {
    return quantity == null ? 1 : quantity;
  }
}
