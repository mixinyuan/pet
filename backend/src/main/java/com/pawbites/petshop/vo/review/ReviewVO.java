package com.pawbites.petshop.vo.review;

import java.math.BigDecimal;

/**
 * 商品评价
 * <p>买家带宠物维度的晒单评价，用于商品页口碑展示。</p>
 */
public record ReviewVO(
    /** 评价 ID */
    Long id,
    /** 所属商品 ID */
    Long productId,
    /** 买家名称 */
    String buyerName,
    /** 宠物名称 */
    String petName,
    /** 宠物品种 */
    String petBreed,
    /** 评价内容 */
    String content,
    /** 买家头像地址 */
    String avatarUrl,
    /** 所购商品名称 */
    String purchasedProduct,
    /** 星级评分（1 位小数，满分 5.0） */
    BigDecimal rating
) {
}
