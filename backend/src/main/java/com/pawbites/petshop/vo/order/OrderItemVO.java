package com.pawbites.petshop.vo.order;

import java.math.BigDecimal;

/**
 * 订单明细
 * <p>商品名称、图片、单价均为下单时的快照，不随商品后续修改变化。</p>
 */
public record OrderItemVO(
    /** 商品 ID */
    Long productId,
    /** 商品名称（下单时快照） */
    String productName,
    /** 商品图片地址（下单时快照） */
    String productImage,
    /** 成交单价（下单时快照） */
    BigDecimal price,
    /** 购买数量 */
    Integer quantity,
    /** 小计金额（单价 × 数量） */
    BigDecimal subtotal
) {
}
