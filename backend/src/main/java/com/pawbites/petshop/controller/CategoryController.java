package com.pawbites.petshop.controller;
import jakarta.annotation.Resource;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pawbites.petshop.result.ApiResponse;
import com.pawbites.petshop.service.CategoryService;
import com.pawbites.petshop.vo.category.CategoryVO;

/**
 * 商品分类控制器
 * <p>路由前缀 /api/categories，提供启用状态商品分类的查询接口。</p>
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

  /** 商品分类服务 */
  @Resource
  private CategoryService categoryService;

  /**
   * 查询全部启用的商品分类（GET /api/categories）
   *
   * @return 分类列表（id、code、名称、描述）
   */
  @GetMapping
  public ApiResponse<List<CategoryVO>> list() {
    return ApiResponse.success(categoryService.listActiveCategories());
  }
}
