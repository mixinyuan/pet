package com.pawbites.petshop.vo.admin;

/**
 * 管理端分类视图
 * <p>后台分类列表用；与 C 端 CategoryVO 的差异：包含全部状态（含停用）、带排序号与
 * 引用商品数（productCount，仅统计未删除商品，供删除前引用校验与列表展示）。</p>
 */
public record AdminCategoryVO(
    /** 分类 ID */
    Long id,
    /** 分类编码（全局唯一，创建后不可修改） */
    String code,
    /** 分类名称 */
    String name,
    /** 分类描述 */
    String description,
    /** 排序号（数值越小越靠前） */
    Integer sortOrder,
    /** 分类状态（ACTIVE 启用/INACTIVE 停用，停用后不出现在 C 端导航） */
    String status,
    /** 引用该分类的商品数（仅统计未删除商品） */
    long productCount
) {
}
