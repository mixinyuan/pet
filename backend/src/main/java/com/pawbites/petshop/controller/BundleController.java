package com.pawbites.petshop.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pawbites.petshop.result.ApiResponse;
import com.pawbites.petshop.service.BundleService;
import com.pawbites.petshop.vo.bundle.BundleVO;

@RestController
@RequestMapping("/api/bundles")
public class BundleController {

  private final BundleService bundleService;

  public BundleController(BundleService bundleService) {
    this.bundleService = bundleService;
  }

  @GetMapping
  public ApiResponse<List<BundleVO>> list() {
    return ApiResponse.success(bundleService.listActiveBundles());
  }
}
