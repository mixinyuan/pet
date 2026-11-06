package com.pawbites.petshop.controller.admin;
import jakarta.annotation.Resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pawbites.petshop.result.ApiResponse;
import com.pawbites.petshop.service.admin.AdminDashboardService;
import com.pawbites.petshop.vo.admin.AdminDashboardVO;

/**
 * 管理端数据看板控制器
 * <p>路由前缀 /api/admin/dashboard，提供后台首页的单个聚合只读接口：
 * 一次响应返回看板全部区块（统计卡片、最近订单、热销 TOP5、库存预警），
 * 避免前端拆成多次请求；由 SecurityConfig 统一约束 /api/admin/** 仅 ADMIN
 * 角色可访问；接口无查询参数，故无需参数校验。</p>
 */
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

  /** 管理端数据看板服务 */
  @Resource
  private AdminDashboardService adminDashboardService;

  /**
   * 查询数据看板聚合数据（GET /api/admin/dashboard）
   * <p>返回四个区块：summary（今日订单数/销售额与较昨日增减、用户总数、
   * 管理员人数、库存预警数；订单类指标只统计 CREATED 状态，按服务器本地
   * 日历日闭区间计算）、recentOrders（最近 10 条订单，含已取消，按订单 id
   * 倒序）、topProducts（未取消订单条目聚合的销量 TOP5，销量并列时按销售额
   * 降序；商品已逻辑删除的聚合行跳过）、lowStockProducts（未删除且库存低于
   * 预警线 20 的商品，含已下架，按库存升序）。纯只读，无写操作。</p>
   *
   * @return 数据看板聚合视图（无数据时各区块为空集合/零值，不报错）
   */
  @GetMapping
  public ApiResponse<AdminDashboardVO> dashboard() {
    return ApiResponse.success(adminDashboardService.getDashboard());
  }
}
