package com.pawbites.petshop.vo.cart;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车
 * <p>聚合当前用户（或游客）购物车的条目与合计信息，用于购物车页展示。</p>
 */
public record CartVO(
    /** 购物车条目列表 */
    List<CartItemVO> items,
    /** 购物车商品总件数（各条目数量之和） */
    Integer count,
    /** 购物车总金额（各条目小计之和） */
    BigDecimal totalAmount
) {
}
