package com.pawbites.petshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pawbites.petshop.entity.Review;

import com.pawbites.petshop.vo.review.ReviewVO;
import java.util.List;

public interface ReviewService extends IService<Review> {

  List<ReviewVO> listActiveReviews();
}
