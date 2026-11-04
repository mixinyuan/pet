package com.pawbites.petshop.service.impl;
import com.pawbites.petshop.service.ReviewService;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.List;
import org.springframework.stereotype.Service;

import com.pawbites.petshop.constant.StatusConstants;
import com.pawbites.petshop.entity.Review;
import com.pawbites.petshop.mapper.ReviewMapper;
import com.pawbites.petshop.vo.review.ReviewVO;

@Service
public class ReviewServiceImpl implements ReviewService {

  private final ReviewMapper reviewMapper;

  public ReviewServiceImpl(ReviewMapper reviewMapper) {
    this.reviewMapper = reviewMapper;
  }

  @Override
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
