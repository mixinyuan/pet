package com.pawbites.petshop.service.admin;

import com.pawbites.petshop.vo.admin.AdminDashboardVO;

/**
 * 管理端数据看板服务
 * <p>后台首页的只读聚合查询：统计卡片（今日订单数/销售额、较昨日增减、
 * 用户总数、管理员人数、库存预警数）、最近订单简表（10 条）、热销商品 TOP5
 * （按未取消订单条目聚合）、库存预警商品列表（库存 &lt; 20）。纯只读，无写操作。</p>
 */
public interface AdminDashboardService {

  /**
   * 查询数据看板聚合数据
   * <p>一次返回看板全部四个区块：summary（今日订单类指标只统计 CREATED 状态、
   * 日期按服务器本地日历日的闭区间 [00:00:00, 23:59:59] 计算）、recentOrders
   * （最近 10 条订单，含已取消，按订单 id 倒序，ownerName 与订单列表口径一致）、
   * topProducts（未取消订单条目聚合，销量降序、销售额降序，商品已逻辑删除的
   * 聚合行跳过）、lowStockProducts（未删除且库存低于预警线 20 的商品，
   * 含已下架，按库存升序）。</p>
   *
   * @return 数据看板聚合视图（四个区块均非 null，无数据时为空集合/零值）
   */
  AdminDashboardVO getDashboard();
}
