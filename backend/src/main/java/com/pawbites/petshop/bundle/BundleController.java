package com.pawbites.petshop.bundle;

import com.pawbites.petshop.common.result.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
