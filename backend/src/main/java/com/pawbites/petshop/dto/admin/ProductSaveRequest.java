package com.pawbites.petshop.dto.admin;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * 商品新增/编辑请求
 * <p>新增与编辑共用同一份全量表单（编辑为整体替换语义）；校验规则对齐后台规格 §5.2：
 * SKU 全局唯一（唯一性在服务层校验）、评分缺省 5.0、上架状态缺省 ACTIVE。</p>
 */
public record ProductSaveRequest(
    /** 商品 SKU（必填，最长 64 字符，全局唯一；建议格式 PB-XXX-XXX，大写转换由前端负责） */
    @NotBlank(message = "SKU 不能为空")
    @Size(max = 64, message = "SKU 长度不能超过 64 字符")
    String sku,
    /** 商品名称（必填，最长 100 字符） */
    @NotBlank(message = "商品名称不能为空")
    @Size(max = 100, message = "商品名称长度不能超过 100 字符")
    String name,
    /** 适用宠物（必填，单选：取值与现有商品数据一致） */
    @NotBlank(message = "适用宠物不能为空")
    @Pattern(regexp = "^(猫咪适用|狗狗适用|猫狗通用|小型犬适用)$",
        message = "适用宠物取值不合法（仅支持：猫咪适用/狗狗适用/猫狗通用/小型犬适用）")
    String petType,
    /** 口味/风味（必填，最长 50 字符） */
    @NotBlank(message = "口味不能为空")
    @Size(max = 50, message = "口味长度不能超过 50 字符")
    String flavor,
    /** 售价（必填，大于 0，最多 8 位整数、2 位小数） */
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0", inclusive = false, message = "价格必须大于 0")
    @Digits(integer = 8, fraction = 2, message = "价格最多 8 位整数、2 位小数")
    BigDecimal price,
    /** 库存（必填，≥ 0 的整数） */
    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能为负数")
    Integer stock,
    /** 评分（可选，0.0-5.0，1 位小数；缺省 5.0，人工维护，不与 C 端评价联动） */
    @DecimalMin(value = "0.0", message = "评分不能低于 0.0")
    @DecimalMax(value = "5.0", message = "评分不能高于 5.0")
    @Digits(integer = 1, fraction = 1, message = "评分最多 1 位整数、1 位小数")
    BigDecimal rating,
    /** 商品图片 URL（必填，合法 http/https 链接，最长 500 字符） */
    @NotBlank(message = "图片 URL 不能为空")
    @Size(max = 500, message = "图片 URL 长度不能超过 500 字符")
    @Pattern(regexp = "^https?://\\S+$", message = "图片 URL 不合法，需以 http:// 或 https:// 开头")
    String imageUrl,
    /** 所属分类 code 列表（必填，至少 1 个；有效性在服务层校验，不允许包含系统内置 all） */
    @NotEmpty(message = "至少选择 1 个分类")
    List<String> categories,
    /** 标签 id 列表（可选；有效性在服务层校验） */
    List<Long> tags,
    /** 商品描述（可选，最长 500 字符） */
    @Size(max = 500, message = "描述长度不能超过 500 字符")
    String description,
    /** 上架状态（可选，ACTIVE 上架/INACTIVE 下架；缺省 ACTIVE） */
    @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "上架状态取值不合法（仅支持 ACTIVE/INACTIVE）")
    String status
) {
}
