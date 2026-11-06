package com.pawbites.petshop.dto.admin;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 分类编辑请求
 * <p>除编码外的字段均为可选修改（null/缺省表示不变）；编码不允许修改——被商品引用后改码会断数据，
 * 服务层比对请求编码与现值，不同即报错，相同（原样传回）视为不改。</p>
 */
public record CategoryUpdateRequest(
    /** 分类编码（不允许修改；传入且与现值不同时报错，原样传回则忽略） */
    String code,
    /** 分类名称（可选修改，最长 64 字符；传入空白视为非法，由服务层校验） */
    @Size(max = 64, message = "分类名称长度不能超过 64 字符")
    String name,
    /** 分类描述（可选修改，最长 255 字符；传入空白按清空处理） */
    @Size(max = 255, message = "分类描述长度不能超过 255 字符")
    String description,
    /** 排序号（可选修改，数值越小越靠前） */
    Integer sortOrder,
    /** 分类状态（可选修改，ACTIVE/INACTIVE） */
    @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "状态取值不合法（仅支持 ACTIVE/INACTIVE）")
    String status
) {
}
