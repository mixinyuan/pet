package com.pawbites.petshop.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pawbites.petshop.result.ApiResponse;
import com.pawbites.petshop.service.ReviewService;
import com.pawbites.petshop.vo.review.ReviewVO;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

  private final ReviewService reviewService;

  public ReviewController(ReviewService reviewService) {
    this.reviewService = reviewService;
  }

  @GetMapping
  public ApiResponse<List<ReviewVO>> list() {
    return ApiResponse.success(reviewService.listActiveReviews());
  }
}
