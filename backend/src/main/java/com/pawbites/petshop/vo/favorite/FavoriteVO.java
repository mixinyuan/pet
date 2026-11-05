package com.pawbites.petshop.vo.favorite;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收藏条目
 * <p>个人中心「我的收藏」列表展示所需信息：收藏关系 + 联查的商品卡片字段
 * （名称/图片/价格/宠物类型/口味/评分），供前端复用商品卡片渲染。</p>
 */
public record FavoriteVO(
    /** 收藏记录 id */
    Long id,
    /** 商品 id（打开详情、加购、取消收藏均以此为准） */
    Long productId,
    /** 商品名称 */
    String productName,
    /** 商品主图地址 */
    String productImage,
    /** 售价（单位：元） */
    BigDecimal price,
    /** 适用宠物类型（如猫、狗） */
    String petType,
    /** 口味/风味 */
    String flavor,
    /** 评分（0.0-5.0） */
    BigDecimal rating,
    /** 收藏时间 */
    LocalDateTime createTime
) {
}
