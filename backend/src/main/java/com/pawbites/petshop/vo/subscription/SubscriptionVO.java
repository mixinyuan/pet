package com.pawbites.petshop.vo.subscription;

/**
 * 订阅信息
 * <p>邮箱订阅结果，前端据此提示订阅成功或已重新激活。</p>
 */
public record SubscriptionVO(
    /** 订阅记录 ID */
    Long id,
    /** 订阅邮箱（全局唯一，统一小写存储） */
    String email,
    /** 订阅状态（ACTIVE-已订阅） */
    String status
) {
}
