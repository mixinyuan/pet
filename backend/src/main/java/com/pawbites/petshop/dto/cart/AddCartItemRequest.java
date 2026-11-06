package com.pawbites.petshop.dto.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 添加购物车条目请求
 * <p>单品与套餐二选一：productId 加购单品、bundleId 加购套餐，两者恰好传其一；
 * 数量可选，未传或为空时按 1 件处理；同一对象重复添加会合并数量。</p>
 */
public record AddCartItemRequest(
    /** 商品 ID（与 bundleId 恰好传其一） */
    Long productId,
    /** 套餐 ID（与 productId 恰好传其一） */
    Long bundleId,
    /** 购买数量（最小为 1、最大 999，可为空，为空时按 1 件计） */
    @Min(1)
    @Max(value = 999, message = "单次购买数量不能超过 999")
    Integer quantity
) {
  public int normalizedQuantity() {
    return quantity == null ? 1 : quantity;
  }
}
