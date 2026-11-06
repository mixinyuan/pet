package com.pawbites.petshop.vo.admin;

import java.util.List;

/**
 * 管理端用户详情收藏汇总视图
 * <p>收藏总数 + 最近收藏的 5 个商品卡片。</p>
 */
public record AdminUserFavoriteVO(
    /** 收藏商品总数 */
    long total,
    /** 最近收藏的商品（按收藏时间倒序，最多 5 个；商品已被逻辑删除的不回显） */
    List<AdminUserFavoriteProductVO> products
) {
}
