package com.pawbites.petshop.controller;

import jakarta.annotation.Resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.pawbites.petshop.result.ApiResponse;
import com.pawbites.petshop.security.SecurityUtils;
import com.pawbites.petshop.service.CouponService;
import com.pawbites.petshop.vo.coupon.CouponVO;

/**
 * 优惠券控制器
 * <p>路由前缀 /api/coupons，提供「我的优惠券」列表查询。
 * 接口要求登录（SecurityConfig 配置 authenticated），游客不可持券。</p>
 */
@RestController
@RequestMapping("/api/coupons")
public class CouponController {

  /** 优惠券服务 */
  @Resource
  private CouponService couponService;

  /**
   * 查询当前用户的持券列表（GET /api/coupons）
   * <p>按领取时间倒序；status 为展示态（0-未使用 1-已使用 2-已过期，过期按
   * expires_at 懒判定）；available = 未使用且未过期（购物车门槛由前端结合金额判断，
   * 下单以后端校验为准）。</p>
   *
   * @return 持券列表（名称、金额、门槛、状态、发放/过期时间、可用标记）
   * @throws BusinessException 未登录（401 请先登录）时抛出
   */
  @GetMapping
  public ApiResponse<List<CouponVO>> list() {
    return ApiResponse.success(couponService.listCoupons(SecurityUtils.currentUserId()));
  }
}
