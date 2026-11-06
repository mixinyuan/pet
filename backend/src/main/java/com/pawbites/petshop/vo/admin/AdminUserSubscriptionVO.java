package com.pawbites.petshop.vo.admin;

/**
 * 管理端用户详情订阅状态视图
 * <p>按用户邮箱匹配 subscription 表：存在状态 ACTIVE 的订阅记录即视为已订阅。</p>
 */
public record AdminUserSubscriptionVO(
    /** 是否已订阅邮件资讯（邮箱存在 ACTIVE 订阅记录） */
    boolean subscribed
) {
}
