package com.pawbites.petshop.vo.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端用户视图
 * <p>后台用户列表行：基本信息之外补充四个汇总字段——orderCount/totalSpend 仅统计
 * 有效订单（status=CREATED，即未取消的实付消费），couponTotal/couponAvailable 统计
 * 持券情况（可用 = 未使用且未过期，与 C 端我的券 available 口径一致）。</p>
 */
public record AdminUserVO(
    /** 用户 ID */
    Long id,
    /** 用户名（登录账号） */
    String username,
    /** 昵称（对外展示名，可能为空） */
    String nickname,
    /** 邮箱 */
    String email,
    /** 账号状态（ACTIVE 正常 / INACTIVE 已禁用；禁用后该用户 token 即时失效） */
    String status,
    /** 注册时间 */
    LocalDateTime createdAt,
    /** 有效订单数（仅状态为 CREATED 的订单） */
    long orderCount,
    /** 有效订单实付总额（仅状态为 CREATED 的订单合计，单位：元） */
    BigDecimal totalSpend,
    /** 持券总数 */
    long couponTotal,
    /** 可用券数（状态未使用且未过期） */
    long couponAvailable
) {
}
