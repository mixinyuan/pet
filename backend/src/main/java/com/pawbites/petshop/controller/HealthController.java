package com.pawbites.petshop.controller;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pawbites.petshop.result.ApiResponse;

/**
 * 应用信息与健康探查控制器
 * <p>路由前缀 /api，提供根路径的应用信息接口与 /health 健康探查接口。
 * 健康状态为静态 UP，未实际探活数据库等下游依赖。</p>
 */
@RestController
@RequestMapping("/api")
public class HealthController {

  /**
   * 返回应用信息（GET /api）
   *
   * @return 应用名称、状态（UP）与主要接口清单
   */
  @GetMapping
  public ApiResponse<Map<String, Object>> apiInfo() {
    return ApiResponse.success(Map.of(
        "name", "PawBites Backend",
        "status", "UP",
        "docs", List.of(
            "GET /api/categories",
            "GET /api/products",
            "GET /api/bundles",
            "GET /api/reviews",
            "POST /api/auth/login",
            "POST /api/cart/items",
            "POST /api/orders"
        )
    ));
  }

  /**
   * 健康探查（GET /api/health）
   * <p>静态返回 status=UP，不检查数据库等依赖。</p>
   *
   * @return 健康状态（status=UP）
   */
  @GetMapping("/health")
  public ApiResponse<Map<String, String>> health() {
    return ApiResponse.success(Map.of("status", "UP"));
  }
}
