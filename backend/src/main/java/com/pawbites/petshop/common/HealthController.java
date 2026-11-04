package com.pawbites.petshop.common;

import com.pawbites.petshop.common.result.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

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

  @GetMapping("/health")
  public ApiResponse<Map<String, String>> health() {
    return ApiResponse.success(Map.of("status", "UP"));
  }
}
