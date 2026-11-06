package com.pawbites.petshop.vo.admin;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端用户详情视图
 * <p>用户详情抽屉：基本资料（含角色）+ 四个聚合区块——全部订单（orders）、
 * 收藏（favorites）、持券（coupons）、邮箱订阅状态（subscription）。</p>
 */
public record AdminUserDetailVO(
    /** 用户 ID */
    Long id,
    /** 用户名（登录账号） */
    String username,
    /** 昵称（对外展示名，可能为空） */
    String nickname,
    /** 邮箱 */
    String email,
    /** 账号状态（ACTIVE 正常 / INACTIVE 已禁用） */
    String status,
    /** 注册时间 */
    LocalDateTime createdAt,
    /** 角色（USER-普通用户 / ADMIN-管理员；管理员账号不允许禁用） */
    String role,
    /** 订单记录：该用户全部订单（含已取消，按订单 id 倒序） */
    List<AdminUserOrderVO> orders,
    /** 收藏：总数 + 最近收藏 5 个商品 */
    AdminUserFavoriteVO favorites,
    /** 持券列表（券名/展示状态/过期时间，按持券 id 倒序） */
    List<AdminUserCouponVO> coupons,
    /** 邮箱订阅状态 */
    AdminUserSubscriptionVO subscription
) {
}
