package com.pawbites.petshop.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 标签新增请求
 * <p>标签名全局唯一（唯一键 uk_product_tag_name 保证，服务层预校验给出明确报错）；
 * 新建标签默认启用（ACTIVE）。</p>
 */
public record TagCreateRequest(
    /** 标签名称（必填，全局唯一，最长 64 字符） */
    @NotBlank(message = "标签名称不能为空")
    @Size(max = 64, message = "标签名称长度不能超过 64 字符")
    String name
) {
}
