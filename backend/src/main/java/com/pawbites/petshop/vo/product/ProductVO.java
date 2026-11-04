package com.pawbites.petshop.vo.product;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品详情
 * <p>商品卡片/详情页展示所需字段，含所属分类与标签，供筛选与展示使用。</p>
 */
public record ProductVO(
    /** 商品 ID */
    Long id,
    /** 商品 SKU 编码（全局唯一） */
    String sku,
    /** 商品名称 */
    String name,
    /** 适用宠物类型（如猫、狗，前端展示于商品信息行） */
    String petType,
    /** 口味/风味（前端与宠物类型一起展示） */
    String flavor,
    /** 售价 */
    BigDecimal price,
    /** 平均评分（1 位小数，前端以“★ x.x / 5.0”形式展示星级） */
    BigDecimal rating,
    /** 库存数量（加购时按此校验） */
    Integer stock,
    /** 商品主图地址 */
    String imageUrl,
    /** 商品描述 */
    String description,
    /** 所属分类名称列表 */
    List<String> categories,
    /** 标签名称列表 */
    List<String> tags
) {
}
