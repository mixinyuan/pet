package com.pawbites.petshop.dto.admin;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 评价新增/编辑请求
 * <p>新增与编辑共用同一份全量表单（编辑为整体替换语义）；校验规则对齐后台规格 §5.6：
 * 评分缺省 5.0（0.0-5.0 一位小数）、排序号缺省 0、展示状态缺省 ACTIVE；
 * productId 可空（空表示通用评价，不关联商品），非空时须存在且未删除（服务层校验）。</p>
 */
public record ReviewSaveRequest(
    /** 评价人姓名（必填，最长 64 字符，对应 buyer_name） */
    @NotBlank(message = "评价人姓名不能为空")
    @Size(max = 64, message = "评价人姓名长度不能超过 64 字符")
    String buyerName,
    /** 宠物名字（可选，最长 64 字符） */
    @Size(max = 64, message = "宠物名字长度不能超过 64 字符")
    String petName,
    /** 宠物品种（可选，最长 64 字符） */
    @Size(max = 64, message = "宠物品种长度不能超过 64 字符")
    String petBreed,
    /** 评价内容（必填，最长 1000 字符） */
    @NotBlank(message = "评价内容不能为空")
    @Size(max = 1000, message = "评价内容长度不能超过 1000 字符")
    String content,
    /** 评价人头像 URL（可选，最长 500 字符） */
    @Size(max = 500, message = "头像 URL 长度不能超过 500 字符")
    String avatarUrl,
    /** 评分（可选，0.0-5.0 一位小数；缺省 5.0，人工维护，不与 C 端联动） */
    @DecimalMin(value = "0.0", message = "评分不能低于 0.0")
    @DecimalMax(value = "5.0", message = "评分不能高于 5.0")
    @Digits(integer = 1, fraction = 1, message = "评分最多 1 位整数、1 位小数")
    BigDecimal rating,
    /** 排序号（可选，≥ 0 的整数，越小越靠前；缺省 0） */
    @Min(value = 0, message = "排序号不能为负数")
    @Max(value = 999999, message = "排序号不能超过 999999")
    Integer sortOrder,
    /** 关联商品 id（可选，空表示通用评价；非空时须存在且未删除，服务层校验） */
    Long productId,
    /** 展示状态（可选，ACTIVE 展示/INACTIVE 隐藏；缺省 ACTIVE） */
    @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "展示状态取值不合法（仅支持 ACTIVE/INACTIVE）")
    String status
) {
}
