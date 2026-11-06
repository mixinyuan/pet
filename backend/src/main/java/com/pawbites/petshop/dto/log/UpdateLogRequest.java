package com.pawbites.petshop.dto.log;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 编辑日志请求
 * <p>仅编辑 title/type/content 三个字段；发布状态走独立的
 * PUT /api/admin/logs/{id}/status 接口，切换状态不更新 create_time。</p>
 */
public record UpdateLogRequest(
    /** 日志标题（必填，去除首尾空白后存储，最长 128 字符） */
    @NotBlank(message = "标题不能为空")
    @Size(max = 128, message = "标题长度不能超过 128 字符")
    String title,
    /** 日志类型（可选，最长 64 字符；为空时默认「AI自习室通知」） */
    @Size(max = 64, message = "类型长度不能超过 64 字符")
    String type,
    /** 日志正文（必填，去除首尾空白后存储） */
    @NotBlank(message = "内容不能为空")
    String content
) {
}
