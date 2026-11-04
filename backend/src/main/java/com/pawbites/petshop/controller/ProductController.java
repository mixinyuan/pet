package com.pawbites.petshop.controller;
import jakarta.annotation.Resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pawbites.petshop.result.ApiResponse;
import com.pawbites.petshop.result.PageResult;
import com.pawbites.petshop.service.ProductService;
import com.pawbites.petshop.vo.product.ProductVO;

@RestController
@RequestMapping("/api/products")
public class ProductController {

  @Resource
  private ProductService productService;

  @GetMapping
  public ApiResponse<PageResult<ProductVO>> list(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size,
      @RequestParam(defaultValue = "all") String category,
      @RequestParam(required = false) String keyword
  ) {
    return ApiResponse.success(productService.listProducts(page, size, category, keyword));
  }

  @GetMapping("/{id}")
  public ApiResponse<ProductVO> detail(@PathVariable Long id) {
    return ApiResponse.success(productService.getProduct(id));
  }
}
