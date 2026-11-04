package com.pawbites.petshop.controller;
import jakarta.annotation.Resource;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pawbites.petshop.result.ApiResponse;
import com.pawbites.petshop.service.BundleService;
import com.pawbites.petshop.vo.bundle.BundleVO;

/**
 * 优惠套装控制器
 * <p>路由前缀 /api/bundles，提供在售优惠套装的查询接口。</p>
 */
@RestController
@RequestMapping("/api/bundles")
public class BundleController {

  /** 优惠套装服务 */
  @Resource
  private BundleService bundleService;

  /**
   * 查询全部在售优惠套装（GET /api/bundles）
   *
   * @return 套装列表（套装价、原价、优惠标签及包含的商品清单）
   */
  @GetMapping
  public ApiResponse<List<BundleVO>> list() {
    return ApiResponse.success(bundleService.listActiveBundles());
  }
}
