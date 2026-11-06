package com.pawbites.petshop.vo.admin;

import java.util.List;

/**
 * 管理端数据看板视图
 * <p>后台首页（/admin）一次性聚合返回的只读看板数据：统计卡片（summary）、
 * 最近订单简表（recentOrders，最近 10 条）、热销商品 TOP5（topProducts，
 * 按未取消订单条目销量聚合）与库存预警列表（lowStockProducts，库存低于预警线
 * 20 件且未删除的商品）。看板无任何写操作，各区块点击后跳转对应管理模块。</p>
 */
public record AdminDashboardVO(
    /** 统计卡片汇总（今日订单/销售额、用户与库存预警计数） */
    AdminDashboardSummaryVO summary,
    /** 最近订单简表（按订单 id 倒序，最多 10 条） */
    List<AdminDashboardRecentOrderVO> recentOrders,
    /** 热销商品 TOP5（按未取消订单条目销量聚合，销量并列时按销售额降序） */
    List<AdminDashboardTopProductVO> topProducts,
    /** 库存预警商品列表（未删除且 stock &lt; 20，按库存升序，含已下架商品） */
    List<AdminDashboardLowStockVO> lowStockProducts
) {
}
