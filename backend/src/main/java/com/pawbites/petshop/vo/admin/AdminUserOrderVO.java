package com.pawbites.petshop.vo.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端用户详情订单简表视图
 * <p>用户详情抽屉的订单记录区块：该用户的全部订单（含已取消），按订单 id 倒序。</p>
 */
public record AdminUserOrderVO(
    /** 订单号 */
    String orderNo,
    /** 订单实付总额（单位：元） */
    BigDecimal totalAmount,
    /** 订单状态（CREATED-已创建 / CANCELLED-已取消） */
    String status,
    /** 下单时间 */
    LocalDateTime createdAt
) {
}
