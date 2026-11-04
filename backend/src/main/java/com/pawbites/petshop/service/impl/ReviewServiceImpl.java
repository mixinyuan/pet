package com.pawbites.petshop.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pawbites.petshop.service.ReviewService;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.List;
import org.springframework.stereotype.Service;

import com.pawbites.petshop.constant.StatusConstants;
import com.pawbites.petshop.entity.Review;
import com.pawbites.petshop.mapper.ReviewMapper;
import com.pawbites.petshop.vo.review.ReviewVO;

/**
 * 评价服务实现
 * <p>查询有效状态的评价，按排序值升序、id 倒序返回，逐条映射为评价视图。</p>
 */
@Service
public class ReviewServiceImpl extends ServiceImpl<ReviewMapper, Review> implements ReviewService {

  @Override
  public List<ReviewVO> listActiveReviews() {
    return baseMapper.selectList(Wrappers.<Review>lambdaQuery()
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
