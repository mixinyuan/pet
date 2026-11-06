package com.pawbites.petshop.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 套餐明细行请求（BundleSaveRequest.items 的元素）
 * <p>对应明细子表编辑器的一行：选择一个商品并给定数量；商品存在性、同一商品不可重复
 * 等跨行校验在服务层完成（需联查商品名称组织报错文案）。</p>
 */
public record BundleItemRequest(
    /** 商品 id（必填；必须存在且未逻辑删除，不要求上架状态——允许套餐含下架商品） */
    @NotNull(message = "商品 id 不能为空")
    Long productId,
    /** 数量（必填，1-99 的整数） */
    @NotNull(message = "商品数量不能为空")
    @Min(value = 1, message = "商品数量最少为 1")
    @Max(value = 99, message = "商品数量最多为 99")
    Integer quantity
) {
}
