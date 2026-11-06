package com.pawbites.petshop.vo.admin;

import java.math.BigDecimal;

/**
 * 管理端看板热销商品视图
 * <p>看板「热销 TOP5」区块行数据：按未取消（CREATED）订单的 order_item
 * 条目聚合——soldQuantity 为各条目数量之和、salesAmount 为各条目小计之和
 * （订单为快照，取下单时价格，商品后续改价不影响历史聚合）；商品名与主图
 * 实时取自商品表（商品已逻辑删除的聚合行直接跳过，不出现在榜单中）。</p>
 */
public record AdminDashboardTopProductVO(
    /** 商品 ID */
    Long productId,
    /** 商品名称（实时取自商品表） */
    String productName,
    /** 商品主图 URL（实时取自商品表） */
    String imageUrl,
    /** 累计销量（未取消订单条目数量之和） */
    int soldQuantity,
    /** 累计销售额（未取消订单条目小计之和，单位：元） */
    BigDecimal salesAmount
) {
}
