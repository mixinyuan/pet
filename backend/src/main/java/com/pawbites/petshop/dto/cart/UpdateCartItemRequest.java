package com.pawbites.petshop.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 更新购物车商品数量请求
 * <p>对应条目的数量以本次提交值覆盖更新。</p>
 */
public record UpdateCartItemRequest(
    /** 更新后的购买数量（必填，最小为 1） */
    @NotNull @Min(1) Integer quantity
) {
}
