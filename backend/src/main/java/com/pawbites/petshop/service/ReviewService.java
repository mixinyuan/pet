package com.pawbites.petshop.service;

import com.pawbites.petshop.vo.review.ReviewVO;
import java.util.List;

public interface ReviewService {

  List<ReviewVO> listActiveReviews();
}
