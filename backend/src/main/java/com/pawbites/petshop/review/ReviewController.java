package com.pawbites.petshop.review;

import com.pawbites.petshop.common.result.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
