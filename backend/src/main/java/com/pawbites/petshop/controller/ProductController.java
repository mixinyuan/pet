package com.pawbites.petshop.controller;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Size;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pawbites.petshop.result.ApiResponse;
import com.pawbites.petshop.result.PageResult;
import com.pawbites.petshop.service.ProductService;
import com.pawbites.petshop.vo.product.ProductVO;

/**
 * 商品控制器
 * <p>路由前缀 /api/products，提供在售商品的分页查询与商品详情接口；
 * 类级 @Validated 开启查询参数校验（如关键字长度限制）。</p>
 */
@Validated
@RestController
@RequestMapping("/api/products")
public class ProductController {

  /** 商品服务 */
  @Resource
  private ProductService productService;

  /**
   * 分页查询在售商品（GET /api/products）
   * <p>结果按评分、id 倒序排列；页码超出上限 10000 时按 10000 处理。</p>
   *
   * @param page 页码，从 1 开始，默认 1，上限 10000（防深分页）
   * @param size 每页条数，1 到 100，默认 20
   * @param category 分类 code，all 表示全部（默认 all）
   * @param keyword 名称/口味/宠物类型关键字模糊搜索，可选，最长 64 字符
   * @return 商品分页结果（商品列表、总数、当前页码、每页条数）
   */
  @GetMapping
  public ApiResponse<PageResult<ProductVO>> list(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size,
      @RequestParam(defaultValue = "all") String category,
      @RequestParam(required = false)
      @Size(max = 64, message = "关键字最长 64 字符")
      String keyword
  ) {
    return ApiResponse.success(productService.listProducts(page, size, category, keyword));
  }

  /**
   * 查询指定商品详情（GET /api/products/{id}）
   *
   * @param id 商品 id
   * @return 商品详情（价格、评分、库存、分类、标签等）
   * @throws BusinessException 商品不存在或已下架（404）时抛出
   */
  @GetMapping("/{id}")
  public ApiResponse<ProductVO> detail(@PathVariable Long id) {
    return ApiResponse.success(productService.getProduct(id));
  }
}
