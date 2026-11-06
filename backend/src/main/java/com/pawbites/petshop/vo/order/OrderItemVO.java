package com.pawbites.petshop.vo.order;

import java.math.BigDecimal;

/**
 * 订单明细
 * <p>名称、图片、单价均为下单时的快照，不随商品/套餐后续修改变化；
 * itemType 区分单品与套餐条目，BUNDLE 行的 productId 为套餐 id、price 为套餐价。</p>
 */
public record OrderItemVO(
    /** 条目类型：PRODUCT-单品 BUNDLE-套餐 */
    String itemType,
    /** 关联 ID：单品为商品 id，套餐为套餐 id */
    Long productId,
    /** 名称快照（单品为商品名，套餐为套餐名） */
    String productName,
    /** 图片地址（下单时快照，套餐行取首个组成商品图片） */
    String productImage,
    /** 成交单价（下单时快照，套餐行即套餐价） */
    BigDecimal price,
    /** 购买数量 */
    Integer quantity,
    /** 小计金额（单价 × 数量） */
    BigDecimal subtotal
) {
}
