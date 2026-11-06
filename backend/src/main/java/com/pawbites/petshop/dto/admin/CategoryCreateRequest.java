package com.pawbites.petshop.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 分类新增请求
 * <p>编码仅在新增时填写（小写字母开头，可含小写字母/数字/连字符），创建后不允许修改；
 * 系统内置编码 all 不允许使用（服务层校验）；编码唯一性由唯一键保证、服务层预校验给出明确报错。</p>
 */
public record CategoryCreateRequest(
    /** 分类编码（必填，全局唯一，仅允许小写字母开头 + 小写字母/数字/连字符，最长 40 字符） */
    @NotBlank(message = "分类编码不能为空")
    @Pattern(regexp = "^[a-z][a-z0-9-]{0,39}$",
        message = "分类编码需以小写字母开头，仅可含小写字母、数字与连字符，最长 40 字符")
    String code,
    /** 分类名称（必填，最长 64 字符） */
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 64, message = "分类名称长度不能超过 64 字符")
    String name,
    /** 分类描述（可选，最长 255 字符） */
    @Size(max = 255, message = "分类描述长度不能超过 255 字符")
    String description,
    /** 排序号（可选，数值越小越靠前，缺省 0） */
    Integer sortOrder
) {
}
