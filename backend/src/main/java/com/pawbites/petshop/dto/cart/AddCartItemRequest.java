package com.pawbites.petshop.dto.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 添加购物车商品请求
 * <p>数量可选，未传或为空时按 1 件处理；同一商品重复添加会合并数量。</p>
 */
public record AddCartItemRequest(
    /** 商品 ID */
    @NotNull
    Long productId,
    /** 购买数量（最小为 1、最大 999，可为空，为空时按 1 件计） */
    @Min(1)
    @Max(value = 999, message = "单次购买数量不能超过 999")
    Integer quantity
) {
  public int normalizedQuantity() {
    return quantity == null ? 1 : quantity;
  }
}
