package com.pawbites.petshop.vo.admin;

import java.math.BigDecimal;

/**
 * 管理端订单条目视图
 * <p>订单商品快照（名称/图片/单价均为下单时快照，不随商品后续改名改价变化），
 * 供订单详情的商品快照列表展示。</p>
 */
public record AdminOrderItemVO(
    /** 商品名称（下单时快照） */
    String productName,
    /** 商品图片地址（下单时快照） */
    String productImage,
    /** 成交单价（下单时快照，单位：元） */
    BigDecimal price,
    /** 购买数量 */
    Integer quantity,
    /** 小计金额（单价 × 数量，单位：元） */
    BigDecimal subtotal
) {
}
