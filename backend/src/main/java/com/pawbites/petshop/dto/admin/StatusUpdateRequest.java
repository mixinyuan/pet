package com.pawbites.petshop.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 状态切换请求
 * <p>商品/分类/标签上下架（启用/停用）接口共用的请求体，status 只接受 ACTIVE 或 INACTIVE。</p>
 */
public record StatusUpdateRequest(
    /** 目标状态（必填，ACTIVE 启用/上架，INACTIVE 停用/下架） */
    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "状态取值不合法（仅支持 ACTIVE/INACTIVE）")
    String status
) {
}
