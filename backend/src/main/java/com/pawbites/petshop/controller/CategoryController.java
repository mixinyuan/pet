package com.pawbites.petshop.controller;
import jakarta.annotation.Resource;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pawbites.petshop.result.ApiResponse;
import com.pawbites.petshop.service.CategoryService;
import com.pawbites.petshop.vo.category.CategoryVO;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

  @Resource
  private CategoryService categoryService;

  @GetMapping
  public ApiResponse<List<CategoryVO>> list() {
    return ApiResponse.success(categoryService.listActiveCategories());
  }
}
