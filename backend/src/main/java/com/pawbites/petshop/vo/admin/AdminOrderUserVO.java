package com.pawbites.petshop.vo.admin;

/**
 * 管理端订单下单用户信息视图
 * <p>订单详情的用户区块展示（昵称/用户名/邮箱）；游客单没有用户信息，
 * 订单详情 user 字段整体为 null。</p>
 */
public record AdminOrderUserVO(
    /** 用户名（登录账号） */
    String username,
    /** 昵称（对外展示名） */
    String nickname,
    /** 邮箱 */
    String email
) {
}
