package com.pawbites.petshop.controller;
import jakarta.annotation.Resource;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pawbites.petshop.result.ApiResponse;
import com.pawbites.petshop.service.ReviewService;
import com.pawbites.petshop.vo.review.ReviewVO;

/**
 * 用户评价控制器
 * <p>路由前缀 /api/reviews，提供展示中的用户评价查询接口，支持按商品过滤。</p>
 */
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

  /** 用户评价服务 */
  @Resource
  private ReviewService reviewService;

  /**
   * 查询展示中的用户评价（GET /api/reviews）
   * <p>不传 productId 时返回全部展示中的评价，传 productId 时仅返回对应商品的评价。</p>
   *
   * @param productId 商品 id（可选，缺省时查询全部）
   * @return 评价列表（买家与宠物信息、评价内容、头像、所购商品）
   */
  @GetMapping
  public ApiResponse<List<ReviewVO>> list(@RequestParam(required = false) Long productId) {
    return ApiResponse.success(reviewService.listActiveReviews(productId));
  }
}
