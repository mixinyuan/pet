package com.pawbites.petshop.dto.log;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 切换日志发布状态请求
 * <p>status 只接受 0（未发布）或 1（已发布）。</p>
 */
public record UpdateLogStatusRequest(
    /** 目标发布状态（必填，0-未发布 1-已发布） */
    @NotNull(message = "发布状态不能为空")
    @Min(value = 0, message = "发布状态只能为 0 或 1")
    @Max(value = 1, message = "发布状态只能为 0 或 1")
    Integer status
) {
}
