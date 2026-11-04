package com.pawbites.petshop.vo.bundle;

import java.math.BigDecimal;
import java.util.List;

import com.pawbites.petshop.vo.product.ProductSummaryVO;

/**
 * 优惠套餐
 * <p>组合多个商品打包售卖，前端展示套餐价、划线原价与折扣角标。</p>
 */
public record BundleVO(
    /** 套餐 ID */
    Long id,
    /** 套餐名称 */
    String name,
    /** 套餐描述 */
    String description,
    /** 套餐现价（促销价，前端作为实际售价展示） */
    BigDecimal price,
    /** 原价（与现价对比，前端以划线价展示） */
    BigDecimal originalPrice,
    /** 折扣标签（前端角标展示，如“立减”类文案） */
    String discountLabel,
    /** 套餐内商品摘要列表（含各商品包含件数） */
    List<ProductSummaryVO> items
) {
}
