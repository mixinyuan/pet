package com.pawbites.petshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pawbites.petshop.entity.Review;

import com.pawbites.petshop.vo.review.ReviewVO;
import java.util.List;

/**
 * 用户评价服务
 * <p>提供有效评价列表查询（可按商品过滤），用于商城评价展示。</p>
 */
public interface ReviewService extends IService<Review> {

  /**
   * 查询有效评价列表
   * <p>按排序值升序、id 倒序返回；productId 为 null 时返回全部，否则仅返回该商品的评价。</p>
   *
   * @param productId 商品 id（可空，null 时返回全部有效评价）
   * @return 评价视图列表
   */
  List<ReviewVO> listActiveReviews(Long productId);
}
