package com.pawbites.petshop.vo.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单
 * <p>由购物车勾选商品结算生成，含订单号、状态、金额与明细，用于订单列表展示。</p>
 */
public record OrderVO(
    /** 订单 ID */
    Long id,
    /** 订单号（全局唯一，前端展示及取消订单的凭证） */
    String orderNo,
    /** 订单总金额（各明细小计之和） */
    BigDecimal totalAmount,
    /** 订单状态（CREATED-已创建，CANCELLED-已取消） */
    String status,
    /** 下单时间 */
    LocalDateTime createdAt,
    /** 订单明细列表 */
    List<OrderItemVO> items
) {
}
