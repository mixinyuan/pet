package com.pawbites.petshop.vo.admin;

/**
 * 管理端看板库存预警商品视图
 * <p>看板「库存预警」区块行数据：未删除且库存低于预警线（20 件，写死常量，
 * 对齐业务梳理文档 Q6）的商品，按库存升序排列。与商品列表的差异：
 * 不过滤上架状态（已下架商品同样需要补库存），点击后打开商品编辑弹窗。</p>
 */
public record AdminDashboardLowStockVO(
    /** 商品 ID */
    Long productId,
    /** 商品名称 */
    String name,
    /** 商品 SKU 编码（全局唯一） */
    String sku,
    /** 当前库存数量（&lt; 20） */
    Integer stock,
    /** 上架状态（ACTIVE-上架 / INACTIVE-下架，预警不过滤下架商品） */
    String status
) {
}
