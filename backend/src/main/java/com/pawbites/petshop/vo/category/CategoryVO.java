package com.pawbites.petshop.vo.category;

/**
 * 商品分类
 * <p>用于首页分类导航与商品筛选，按排序字段有序返回。</p>
 */
public record CategoryVO(
    /** 分类 ID */
    Long id,
    /** 分类编码（全局唯一） */
    String code,
    /** 分类名称（前端导航展示） */
    String name,
    /** 分类描述 */
    String description
) {
}
