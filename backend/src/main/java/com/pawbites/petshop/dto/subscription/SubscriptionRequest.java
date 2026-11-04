package com.pawbites.petshop.dto.subscription;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 订阅优惠资讯请求
 * <p>同一邮箱重复提交视为重新激活订阅，不会重复建档。</p>
 */
public record SubscriptionRequest(
    /** 订阅邮箱（必填且格式须合法，最长 128 字符，服务端去除首尾空白并统一转小写） */
    @NotBlank
    @Email
    @Size(max = 128, message = "邮箱长度不能超过 128 字符")
    String email
) {
}
