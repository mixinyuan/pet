package com.pawbites.petshop.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 优惠券模板新增/编辑请求
 * <p>新增与编辑共用同一份全量表单（编辑为整体替换语义）；校验规则对齐后台规格 §5.7：
 * 名称全局唯一（唯一性在服务层校验）、面额大于 0、门槛 ≥ 0（0 表示无门槛）、有效天数大于 0。
 * 注意：user_coupon 未快照金额/门槛，名称、金额、门槛在展示与核销时随模板实时读取，
 * 编辑模板会影响存量持券实例（见服务实现说明）。</p>
 */
public record CouponTemplateSaveRequest(
    /** 券名称（必填，最长 64 字符，全局唯一；自动发券按名称「新人专享券」定位模板） */
    @NotBlank(message = "券名称不能为空")
    @Size(max = 64, message = "券名称长度不能超过 64 字符")
    String name,
    /** 抵扣面额（必填，大于 0 的整数，单位：元） */
    @NotNull(message = "面额不能为空")
    @Min(value = 1, message = "面额必须大于 0")
    Integer amount,
    /** 使用门槛（必填，≥ 0 的整数，单位：元；0 表示无门槛） */
    @NotNull(message = "使用门槛不能为空")
    @Min(value = 0, message = "使用门槛不能为负数")
    Integer minSpend,
    /** 有效天数（必填，大于 0 的整数；发放时按 发放时间 + valid_days 计算过期时间） */
    @NotNull(message = "有效天数不能为空")
    @Min(value = 1, message = "有效天数必须大于 0")
    Integer validDays,
    /** 备注说明（可选，最长 255 字符，发放场景与规则文案） */
    @Size(max = 255, message = "备注长度不能超过 255 字符")
    String remark
) {
}
