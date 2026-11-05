package com.pawbites.petshop.vo.coupon;

import java.time.LocalDateTime;

/**
 * 我的优惠券条目
 * <p>个人中心「我的优惠券」与购物车结算选券展示所需信息。
 * status 为懒判定后的展示态：0-未使用 1-已使用 2-已过期
 * （数据库仅存 0/1，2 由 expires_at 判定后输出，不回写）。
 * available = 未使用且未过期（不含门槛判断：购物车金额门槛由前端按 cart 总额预判，
 * 最终以后端下单校验为准）。</p>
 */
public record CouponVO(
    /** 持券实例 id（user_coupon.id，下单接口 couponId 传此 id） */
    Long id,
    /** 券模板 id */
    Long couponId,
    /** 券名称（如：新人专享券） */
    String name,
    /** 抵扣金额（单位：元） */
    Integer amount,
    /** 使用门槛（单位：元，0-无门槛） */
    Integer minSpend,
    /** 展示状态（0-未使用 1-已使用 2-已过期，过期为懒判定展示态） */
    Integer status,
    /** 发放时间 */
    LocalDateTime receivedAt,
    /** 过期时间 */
    LocalDateTime expiresAt,
    /** 是否可用（未使用且未过期；门槛是否满足由前端结合购物车金额判断） */
    Boolean available
) {
}
