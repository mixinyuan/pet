package com.pawbites.petshop.service.admin;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pawbites.petshop.dto.admin.ReviewSaveRequest;
import com.pawbites.petshop.entity.Review;
import com.pawbites.petshop.result.PageResult;
import com.pawbites.petshop.vo.admin.AdminReviewVO;

/**
 * 管理端评价服务
 * <p>提供后台评价维护：不过滤展示状态的分页查询（含隐藏评价，按排序号升序与 C 端展示序一致）、
 * 新增/编辑（关联商品校验）、展示状态切换与删除（逻辑删除）。</p>
 */
public interface AdminReviewService extends IService<Review> {

  /**
   * 分页查询评价（后台视角，不过滤展示状态）
   * <p>关键字匹配评价内容或评价人姓名（like，通配符转义），结果按 sort_order 升序、
   * id 倒序（同 C 端展示序，便于后台按实际展示顺序审核）；records 含 review 全部字段
   * 与关联商品名 productName（未关联或商品已删除为 null）。</p>
   *
   * @param page      页码（从 1 开始，小于 1 时按 1 处理，大于 10000 时按 10000 处理）
   * @param size      每页数量（clamp 到 [1, 100]）
   * @param keyword   关键字（null 或空白表示不过滤，匹配评价内容/评价人姓名）
   * @param status    展示状态筛选（ACTIVE/INACTIVE，null 或空白表示不过滤）
   * @param productId 关联商品 id 筛选（null 表示不过滤）
   * @return 管理端评价视图分页结果（records 含关联商品名）
   * @throws com.pawbites.petshop.exception.BusinessException status 取值非法（400）时抛出
   */
  PageResult<AdminReviewVO> pageReviews(long page, long size, String keyword, String status, Long productId);

  /**
   * 查询单条评价（后台视角，隐藏评价也可查看）
   *
   * @param id 评价 id
   * @return 管理端评价视图（含关联商品名）
   * @throws com.pawbites.petshop.exception.BusinessException 评价不存在（404）时抛出
   */
  AdminReviewVO getReview(Long id);

  /**
   * 新增评价
   *
   * @param request 评价表单（关联商品非空时须存在且未删除）
   * @return 新增后的评价视图
   * @throws com.pawbites.petshop.exception.BusinessException 关联商品不存在（400）时抛出
   */
  AdminReviewVO createReview(ReviewSaveRequest request);

  /**
   * 编辑评价（整体替换语义，主表可编辑列全量更新）
   *
   * @param id      评价 id
   * @param request 评价表单（与新增同一份校验）
   * @return 编辑后的评价视图
   * @throws com.pawbites.petshop.exception.BusinessException 评价不存在（404）、
   *                                                          关联商品不存在（400）时抛出
   */
  AdminReviewVO updateReview(Long id, ReviewSaveRequest request);

  /**
   * 切换评价展示状态
   * <p>INACTIVE（隐藏）后 C 端评价列表立即不展示；ACTIVE（展示）恢复。</p>
   *
   * @param id     评价 id
   * @param status 目标状态（ACTIVE/INACTIVE）
   * @throws com.pawbites.petshop.exception.BusinessException 评价不存在（404）时抛出
   */
  void updateStatus(Long id, String status);

  /**
   * 删除评价（逻辑删除主表，deleted=1）
   *
   * @param id 评价 id
   * @throws com.pawbites.petshop.exception.BusinessException 评价不存在（404）时抛出
   */
  void deleteReview(Long id);
}
