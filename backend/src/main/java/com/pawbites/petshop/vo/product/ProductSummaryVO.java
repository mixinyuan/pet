package com.pawbites.petshop.vo.product;

import java.math.BigDecimal;

/**
 * 商品摘要
 * <p>套餐等场景下展示的商品精简信息，附带包含件数。</p>
 */
public record ProductSummaryVO(
    /** 商品 ID */
    Long id,
    /** 商品名称 */
    String name,
    /** 单价 */
    BigDecimal price,
    /** 商品主图地址 */
    String imageUrl,
    /** 数量（套餐内包含的该商品件数） */
    Integer quantity,
    /** 置灰标记：商品已下架（INACTIVE）或已逻辑删除时为 true，前端在套餐区将该商品置灰展示 */
    Boolean offShelf
) {
}
