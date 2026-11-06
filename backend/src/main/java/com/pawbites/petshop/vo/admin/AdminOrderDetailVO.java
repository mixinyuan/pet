package com.pawbites.petshop.vo.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端订单详情视图
 * <p>在列表行全部字段之上补充：商品快照列表（items）、商品合计（itemsSubtotal，
 * 各条目小计之和，与实付总额的差即券抵扣）与下单用户信息（user，游客单为 null）。</p>
 */
public record AdminOrderDetailVO(
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
    String cancelBy,
    /** 商品快照列表（按条目 id 升序） */
    List<AdminOrderItemVO> items,
    /** 商品合计（各条目小计之和，单位：元；实付总额 = 商品合计 - 券抵扣） */
    BigDecimal itemsSubtotal,
    /** 下单用户信息（username/nickname/email；游客单为 null） */
    AdminOrderUserVO user
) {
}
