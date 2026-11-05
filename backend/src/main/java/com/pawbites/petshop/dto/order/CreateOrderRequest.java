package com.pawbites.petshop.dto.order;

/**
 * 创建订单请求
 * <p>全部字段可选：不传 body 或传空对象表示不使用优惠券（与既有无 body 下单行为兼容）。</p>
 *
 * @param couponId 用户持券实例 id（user_coupon.id，非模板 id）；为 null 表示不使用优惠券。
 *                 仅登录用户可用券，游客携带该字段将被拒绝
 */
public record CreateOrderRequest(Long couponId) {
}
