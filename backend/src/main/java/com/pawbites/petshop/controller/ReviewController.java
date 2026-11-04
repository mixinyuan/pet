package com.pawbites.petshop.controller;
import jakarta.annotation.Resource;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pawbites.petshop.result.ApiResponse;
import com.pawbites.petshop.service.ReviewService;
import com.pawbites.petshop.vo.review.ReviewVO;

/**
 * 用户评价控制器
 * <p>路由前缀 /api/reviews，提供展示中的用户评价查询接口。</p>
 */
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

  /** 用户评价服务 */
  @Resource
  private ReviewService reviewService;

  /**
   * 查询全部展示中的用户评价（GET /api/reviews）
   *
   * @return 评价列表（买家与宠物信息、评价内容、头像、所购商品）
   */
  @GetMapping
  public ApiResponse<List<ReviewVO>> list() {
    return ApiResponse.success(reviewService.listActiveReviews());
  }
}
