package com.pawbites.petshop.vo.admin;

import java.math.BigDecimal;

/**
 * 管理端看板统计卡片视图
 * <p>今日经营指标与存量计数。订单类指标统一只统计 CREATED 状态（已取消订单
 * 不计入今日订单数与今日销售额，与订单列表「有效订单」口径一致）；用户计数
 * 均为未删除用户（逻辑删除自动过滤）；库存预警计数与 lowStockProducts 列表
 * 同口径（含已下架商品）。</p>
 */
public record AdminDashboardSummaryVO(
    /** 今日创建的订单数（服务器本地日期，仅 CREATED 状态，含用户单与游客单） */
    long todayOrderCount,
    /** 今日 CREATED 订单实付总额（total_amount 之和，单位：元） */
    BigDecimal todaySalesAmount,
    /** 今日订单数 - 昨日订单数（带符号；正数增长、负数回落、0 持平） */
    int todayOrderDelta,
    /** 未删除用户总数（含普通用户与管理员，含已禁用账号） */
    long userTotal,
    /** ADMIN 角色人数（未删除） */
    long adminCount,
    /** 库存预警商品数（未删除且 stock &lt; 20，含已下架商品） */
    long lowStockCount
) {
}
