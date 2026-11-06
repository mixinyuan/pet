package com.pawbites.petshop.vo.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端看板最近订单简表视图
 * <p>看板「最近订单」区块行数据，为订单列表（AdminOrderVO）的精简版：
 * 仅保留简表展示所需的单号/下单人/件数/金额/状态/时间，点击后跳转订单管理
 * 并定位。ownerName 口径与 AdminOrderVO 一致：用户单取昵称（缺省回退用户名），
 * 游客单为「游客 + guestId 前 6 位」。</p>
 */
public record AdminDashboardRecentOrderVO(
    /** 订单 ID */
    Long id,
    /** 订单号（全局唯一） */
    String orderNo,
    /** 下单人展示名：用户单取昵称（缺省回退用户名），游客单为「游客 + guestId 前 6 位」 */
    String ownerName,
    /** 商品件数（各订单条目数量之和） */
    int itemCount,
    /** 订单实付总额（单位：元） */
    BigDecimal totalAmount,
    /** 订单状态（CREATED-已创建 / CANCELLED-已取消） */
    String status,
    /** 下单时间 */
    LocalDateTime createdAt
) {
}
