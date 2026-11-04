package com.pawbites.petshop.controller;
import jakarta.annotation.Resource;

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

  @Resource
  private BundleService bundleService;

  @GetMapping
  public ApiResponse<List<BundleVO>> list() {
    return ApiResponse.success(bundleService.listActiveBundles());
  }
}
