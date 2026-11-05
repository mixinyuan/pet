package com.pawbites.petshop.dto.log;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 新增日志请求
 * <p>title 与 content 必填；type 留空时服务端默认「AI自习室通知」；status 可选（0/1），缺省为未发布。</p>
 */
public record CreateLogRequest(
    /** 日志标题（必填，去除首尾空白后存储，最长 128 字符） */
    @NotBlank(message = "标题不能为空")
    @Size(max = 128, message = "标题长度不能超过 128 字符")
    String title,
    /** 日志类型（可选，最长 64 字符；为空时默认「AI自习室通知」） */
    @Size(max = 64, message = "类型长度不能超过 64 字符")
    String type,
    /** 日志正文（必填，去除首尾空白后存储） */
    @NotBlank(message = "内容不能为空")
    String content,
    /** 发布状态（可选，0-未发布 1-已发布；缺省为 0 未发布） */
    @Min(value = 0, message = "发布状态只能为 0 或 1")
    @Max(value = 1, message = "发布状态只能为 0 或 1")
    Integer status
) {
}
