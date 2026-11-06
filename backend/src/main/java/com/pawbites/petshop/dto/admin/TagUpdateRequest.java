package com.pawbites.petshop.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 标签编辑请求
 * <p>标签仅名称一个可编辑字段（改名需保持全局唯一，服务层校验）；状态切换走 /status 接口。</p>
 */
public record TagUpdateRequest(
    /** 标签名称（必填，全局唯一，最长 64 字符） */
    @NotBlank(message = "标签名称不能为空")
    @Size(max = 64, message = "标签名称长度不能超过 64 字符")
    String name
) {
}
