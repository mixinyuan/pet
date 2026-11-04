package com.pawbites.petshop.vo.cart;

import java.math.BigDecimal;

/**
 * 购物车条目
 * <p>对应购物车中的一件商品，携带数量、小计与勾选状态。</p>
 */
public record CartItemVO(
    /** 商品 ID */
    Long productId,
    /** 商品名称 */
    String productName,
    /** 商品主图地址 */
    String productImage,
    /** 商品单价（结算时按此价格计） */
    BigDecimal price,
    /** 购买数量 */
    Integer quantity,
    /** 小计金额（单价 × 数量） */
    BigDecimal subtotal,
    /** 勾选状态（是否参与结算，加入购物车时默认勾选） */
    Boolean checked
) {
}
