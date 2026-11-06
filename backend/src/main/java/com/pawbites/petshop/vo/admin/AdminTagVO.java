package com.pawbites.petshop.vo.admin;

/**
 * 管理端标签视图
 * <p>后台标签列表用；带引用商品数（productCount，仅统计未删除商品，供删除前引用校验与列表展示）。</p>
 */
public record AdminTagVO(
    /** 标签 ID */
    Long id,
    /** 标签名称（全局唯一） */
    String name,
    /** 标签状态（ACTIVE 启用/INACTIVE 停用） */
    String status,
    /** 引用该标签的商品数（仅统计未删除商品） */
    long productCount
) {
}
