package com.pawbites.petshop.vo.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端订单视图
 * <p>后台订单列表行：在订单主表字段之外，补充下单人展示名（ownerName，用户昵称或
 * 「游客 + guestId 前 6 位」）、商品件数汇总（itemCount，各条目数量之和）与
 * 用券信息（couponName/couponAmount，经 user_coupon.order_id 反查模板；
 * 未用券或取消订单券已返还时为 null）、取消审计列（cancelTime/cancelBy）。</p>
 */
public record AdminOrderVO(
    /** 订单 ID */
    Long id,
    /** 订单号（全局唯一） */
    String orderNo,
    /** 是否游客单（userId 为空即游客下单） */
    boolean isGuest,
    /** 下单用户 ID（游客单为 null） */
    Long userId,
    /** 游客标识（登录用户下单为 null） */
    String guestId,
    /** 下单人展示名：用户单取昵称（缺省回退用户名），游客单为「游客 + guestId 前 6 位」 */
    String ownerName,
    /** 商品件数（各订单条目数量之和） */
    int itemCount,
    /** 订单实付总额（商品合计 - 券抵扣，单位：元） */
    BigDecimal totalAmount,
    /** 使用的券名称（未用券或券已随取消返还时为 null） */
    String couponName,
    /** 券抵扣金额（单位：元；未用券或券已随取消返还时为 null） */
    Integer couponAmount,
    /** 订单状态（CREATED-已创建 / CANCELLED-已取消） */
    String status,
    /** 下单时间 */
    LocalDateTime createdAt,
    /** 取消时间（未取消为 null） */
    LocalDateTime cancelTime,
    /** 取消方（USER-用户取消 / ADMIN-管理员取消；未取消为 null） */
    String cancelBy
) {
}
