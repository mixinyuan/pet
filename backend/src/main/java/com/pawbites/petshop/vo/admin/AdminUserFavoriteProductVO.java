package com.pawbites.petshop.vo.admin;

import java.math.BigDecimal;

/**
 * 管理端用户详情收藏商品视图
 * <p>用户详情抽屉收藏区块的最近收藏商品卡片（最多 5 个）。</p>
 */
public record AdminUserFavoriteProductVO(
    /** 商品 ID */
    Long productId,
    /** 商品名称 */
    String name,
    /** 商品图片地址 */
    String imageUrl,
    /** 商品价格（单位：元） */
    BigDecimal price
) {
}
