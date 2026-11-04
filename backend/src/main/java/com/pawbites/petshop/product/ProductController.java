package com.pawbites.petshop.product;

import com.pawbites.petshop.common.result.ApiResponse;
import com.pawbites.petshop.common.result.PageResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

  private final ProductService productService;

  public ProductController(ProductService productService) {
    this.productService = productService;
  }

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
