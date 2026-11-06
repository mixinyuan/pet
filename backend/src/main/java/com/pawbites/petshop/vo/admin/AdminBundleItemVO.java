package com.pawbites.petshop.vo.admin;

import java.math.BigDecimal;

/**
 * 管理端套餐明细商品视图
 * <p>套餐列表/详情里明细子表的行结构：商品摘要 + 数量（后台允许套餐含下架商品，
 * 故仅凭商品存在性装配，不要求 ACTIVE）。</p>
 */
public record AdminBundleItemVO(
    /** 商品 ID */
    Long productId,
    /** 商品名称 */
    String productName,
    /** 商品单价（当前售价） */
    BigDecimal price,
    /** 商品主图 URL */
    String imageUrl,
    /** 该商品在套餐中的数量 */
    Integer quantity
) {
}
