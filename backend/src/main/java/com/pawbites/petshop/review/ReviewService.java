package com.pawbites.petshop.review;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pawbites.petshop.common.config.StatusConstants;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReviewService {

  private final ReviewMapper reviewMapper;

  public ReviewService(ReviewMapper reviewMapper) {
    this.reviewMapper = reviewMapper;
  }

  public List<ReviewVO> listActiveReviews() {
    return reviewMapper.selectList(Wrappers.<Review>lambdaQuery()
            .eq(Review::getStatus, StatusConstants.ACTIVE)
            .orderByAsc(Review::getSortOrder)
            .orderByDesc(Review::getId))
        .stream()
        .map(review -> new ReviewVO(
            review.getId(),
            review.getProductId(),
            review.getBuyerName(),
            review.getPetName(),
            review.getPetBreed(),
            review.getContent(),
            review.getAvatarUrl(),
            review.getPurchasedProduct(),
            review.getRating()
        ))
        .toList();
  }
}
