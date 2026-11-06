package com.pawbites.petshop.dto.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;

/**
 * 套餐新增/编辑请求
 * <p>新增与编辑共用同一份全量表单（编辑为整体替换语义：主表全量更新、bundle_item 先删后插）；
 * 校验规则对齐后台规格 §5.3：原价 ≥ 现价（服务层跨字段校验）、排序号缺省 0、上架状态缺省 ACTIVE、
 * 明细至少 1 行且同一商品不可重复（服务层校验）。</p>
 */
public record BundleSaveRequest(
    /** 套餐名称（必填，最长 100 字符） */
    @NotBlank(message = "套餐名称不能为空")
    @Size(max = 100, message = "套餐名称长度不能超过 100 字符")
    String name,
    /** 套餐现价（必填，大于 0，最多 8 位整数、2 位小数） */
    @NotNull(message = "现价不能为空")
    @DecimalMin(value = "0", inclusive = false, message = "现价必须大于 0")
    @Digits(integer = 8, fraction = 2, message = "现价最多 8 位整数、2 位小数")
    BigDecimal price,
    /** 原价（必填，大于 0，最多 8 位整数、2 位小数；须 ≥ 现价，服务层校验） */
    @NotNull(message = "原价不能为空")
    @DecimalMin(value = "0", inclusive = false, message = "原价必须大于 0")
    @Digits(integer = 8, fraction = 2, message = "原价最多 8 位整数、2 位小数")
    BigDecimal originalPrice,
    /** 折扣文案（可选，最长 20 字符，如「省 18%」；留空则 C 端不显示角标） */
    @Size(max = 20, message = "折扣文案长度不能超过 20 字符")
    String discountLabel,
    /** 套餐描述（可选，最长 200 字符） */
    @Size(max = 200, message = "描述长度不能超过 200 字符")
    String description,
    /** 排序序号（可选，整数，缺省 0；数值越小越靠前） */
    Integer sortOrder,
    /** 商品明细列表（必填，至少 1 行；行内及跨行校验见 {@link BundleItemRequest} 与服务层） */
    @NotEmpty(message = "套餐至少包含 1 个商品")
    @Valid
    List<BundleItemRequest> items,
    /** 上架状态（可选，ACTIVE 上架/INACTIVE 下架；缺省 ACTIVE） */
    @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "上架状态取值不合法（仅支持 ACTIVE/INACTIVE）")
    String status
) {
}
