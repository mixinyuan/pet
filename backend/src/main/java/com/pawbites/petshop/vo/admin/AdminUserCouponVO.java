package com.pawbites.petshop.vo.admin;

import java.time.LocalDateTime;

/**
 * 管理端用户详情持券视图
 * <p>status 为懒判定展示态（0-未使用 1-已使用 2-已过期），与 C 端「我的优惠券」
 * 口径一致：数据库仅存 0/1，过期按 expires_at 判定输出为 2、不回写。</p>
 */
public record AdminUserCouponVO(
    /** 券名称（取自券模板） */
    String name,
    /** 持券展示状态（0-未使用 1-已使用 2-已过期〔懒判定〕） */
    int status,
    /** 过期时间 */
    LocalDateTime expiresAt
) {
}
