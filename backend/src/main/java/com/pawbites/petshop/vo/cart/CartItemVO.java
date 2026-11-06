package com.pawbites.petshop.vo.cart;

import java.math.BigDecimal;

/**
 * 购物车条目
 * <p>对应购物车中的一件单品或一份套餐，携带数量、小计与勾选状态；
 * itemType 区分两类条目，BUNDLE 行的 productId 为套餐 id、price 为套餐价。</p>
 */
public record CartItemVO(
    /** 条目类型：PRODUCT-单品（productId 为商品 id） BUNDLE-套餐（productId 为套餐 id） */
    String itemType,
    /** 关联 ID：单品为商品 id，套餐为套餐 id */
    Long productId,
    /** 名称：单品为商品名，套餐为套餐名 */
    String productName,
    /** 图片：单品为商品主图，套餐取首个组成商品图片 */
    String productImage,
    /** 单价（单品为商品价、套餐为套餐价，结算时按此价格计） */
    BigDecimal price,
    /** 购买数量 */
    Integer quantity,
    /** 小计金额（单价 × 数量） */
    BigDecimal subtotal,
    /** 勾选状态（是否参与结算，加入购物车时默认勾选） */
    Boolean checked
) {
}
