package com.pawbites.petshop.service.impl.admin;
import jakarta.annotation.Resource;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pawbites.petshop.service.admin.AdminReviewService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.pawbites.petshop.constant.StatusConstants;
import com.pawbites.petshop.dto.admin.ReviewSaveRequest;
import com.pawbites.petshop.entity.Product;
import com.pawbites.petshop.entity.Review;
import com.pawbites.petshop.exception.BusinessException;
import com.pawbites.petshop.mapper.ProductMapper;
import com.pawbites.petshop.mapper.ReviewMapper;
import com.pawbites.petshop.result.PageResult;
import com.pawbites.petshop.vo.admin.AdminReviewVO;

/**
 * 管理端评价服务实现
 * <p>pageReviews 与管理端商品/用户同款查询套路（页码/条数 clamp、状态取值校验、
 * like 通配符经 escapeLike 转义），关键字以「内容 or 评价人姓名」嵌套 or 匹配，
 * 排序与 C 端展示序一致（sort_order 升序、id 倒序）；列表批量联查商品名装配
 * productName（未关联或商品已删除为 null）。create/update 共用一份表单：关联商品
 * 非空时校验存在且未删除，并随关联同步冗余展示列 purchased_product = 商品名
 * （与 V2 种子数据口径一致，供 C 端评价卡片展示）；update 只 SET 表单覆盖的
 * 可编辑列（沿用日志/商品管理更新写法）；delete 走 @TableLogic 逻辑删除。</p>
 */
@Service
public class AdminReviewServiceImpl extends ServiceImpl<ReviewMapper, Review> implements AdminReviewService {

  /** 评分缺省值（表单未填时） */
  private static final BigDecimal DEFAULT_RATING = new BigDecimal("5.0");

  /** 排序号缺省值（表单未填时） */
  private static final int DEFAULT_SORT_ORDER = 0;

  /** 商品 Mapper（关联商品校验与商品名装配） */
  @Resource
  private ProductMapper productMapper;

  @Override
  public PageResult<AdminReviewVO> pageReviews(long page, long size, String keyword, String status, Long productId) {
    if (StringUtils.hasText(status)
        && !StatusConstants.ACTIVE.equals(status) && !StatusConstants.INACTIVE.equals(status)) {
      throw new BusinessException(400, "状态筛选仅支持 ACTIVE 或 INACTIVE");
    }
    // 页码 clamp 到 [1, 10000]，防止深分页时偏移量溢出或拖垮查询；每页条数 clamp 到 [1, 100]
    long safePage = Math.min(Math.max(page, 1), 10000L);
    long safeSize = Math.min(Math.max(size, 1), 100);
    // 关键字转义 like 通配符后再参与匹配，避免 %、_、\ 被当作通配符解释
    String likeKeyword = escapeLike(StringUtils.hasText(keyword) ? keyword.trim() : null);

    // 后台视角不过滤展示状态（隐藏评价同样可见）；关键字匹配 内容 or 评价人姓名；
    // 排序与 C 端展示序一致：sort_order 升序、id 倒序，便于按实际展示顺序审核
    LambdaQueryWrapper<Review> query = Wrappers.lambdaQuery();
    if (StringUtils.hasText(status)) {
      query.eq(Review::getStatus, status);
    }
    if (productId != null) {
      query.eq(Review::getProductId, productId);
    }
    if (likeKeyword != null) {
      query.and(nested -> nested
          .like(Review::getContent, likeKeyword)
          .or()
          .like(Review::getBuyerName, likeKeyword));
    }
    query.orderByAsc(Review::getSortOrder).orderByDesc(Review::getId);

    IPage<Review> reviewPage = baseMapper.selectPage(Page.of(safePage, safeSize), query);
    return new PageResult<>(
        assembleVOs(reviewPage.getRecords()),
        reviewPage.getTotal(),
        reviewPage.getCurrent(),
        reviewPage.getSize()
    );
  }

  @Override
  public AdminReviewVO getReview(Long id) {
    return assembleVOs(List.of(requireReview(id))).getFirst();
  }

  @Transactional
  @Override
  public AdminReviewVO createReview(ReviewSaveRequest request) {
    // 先完成关联商品校验再落库，避免写入后才因关联数据非法而报错
    Product linkedProduct = resolveLinkedProduct(request.productId());
    Review review = new Review();
    applyEditableFields(review, request, linkedProduct);
    // created_at/updated_at 由 MetaObjectHandler 自动填充，deleted 由数据库默认值 0
    baseMapper.insert(review);
    return getReview(review.getId());
  }

  @Transactional
  @Override
  public AdminReviewVO updateReview(Long id, ReviewSaveRequest request) {
    requireReview(id);
    Product linkedProduct = resolveLinkedProduct(request.productId());
    Review review = new Review();
    applyEditableFields(review, request, linkedProduct);

    // 只 SET 表单覆盖的可编辑列（沿用日志/商品管理更新写法），
    // created_at 不触碰、updated_at 由数据库 ON UPDATE CURRENT_TIMESTAMP 自动维护
    baseMapper.update(null, Wrappers.<Review>lambdaUpdate()
        .eq(Review::getId, id)
        .set(Review::getProductId, review.getProductId())
        .set(Review::getBuyerName, review.getBuyerName())
        .set(Review::getPetName, review.getPetName())
        .set(Review::getPetBreed, review.getPetBreed())
        .set(Review::getContent, review.getContent())
        .set(Review::getAvatarUrl, review.getAvatarUrl())
        .set(Review::getPurchasedProduct, review.getPurchasedProduct())
        .set(Review::getRating, review.getRating())
        .set(Review::getSortOrder, review.getSortOrder())
        .set(Review::getStatus, review.getStatus()));
    return getReview(id);
  }

  @Transactional
  @Override
  public void updateStatus(Long id, String status) {
    requireReview(id);
    // 只更新状态列；updated_at 由数据库 ON UPDATE CURRENT_TIMESTAMP 自动维护。
    // INACTIVE（隐藏）后 C 端评价查询按 status=ACTIVE 过滤，立即不展示
    baseMapper.update(null, Wrappers.<Review>lambdaUpdate()
        .eq(Review::getId, id)
        .set(Review::getStatus, status));
  }

  @Transactional
  @Override
  public void deleteReview(Long id) {
    requireReview(id);
    // 逻辑删除（deleted=1）；C 端与管理端查询均按 deleted=0 过滤，自然不可见
    baseMapper.deleteById(id);
  }

  /** 按 id 查询评价（含隐藏评价），不存在或已删除时抛 404 */
  private Review requireReview(Long id) {
    Review review = baseMapper.selectById(id);
    if (review == null) {
      throw new BusinessException(404, "评价不存在");
    }
    return review;
  }

  /** 解析关联商品：productId 为空返回 null（通用评价）；非空时须存在且未删除，否则抛 400 */
  private Product resolveLinkedProduct(Long productId) {
    if (productId == null) {
      return null;
    }
    // product 带逻辑删除列，MP 查询自动过滤已删除商品
    Product product = productMapper.selectById(productId);
    if (product == null) {
      throw new BusinessException(400, "关联商品不存在");
    }
    return product;
  }

  /** 应用表单可编辑字段（去空白、补默认值；随关联同步冗余展示列 purchased_product = 商品名） */
  private void applyEditableFields(Review review, ReviewSaveRequest request, Product linkedProduct) {
    review.setBuyerName(request.buyerName().trim());
    review.setPetName(StringUtils.hasText(request.petName()) ? request.petName().trim() : null);
    review.setPetBreed(StringUtils.hasText(request.petBreed()) ? request.petBreed().trim() : null);
    review.setContent(request.content().trim());
    review.setAvatarUrl(StringUtils.hasText(request.avatarUrl()) ? request.avatarUrl().trim() : null);
    // 关联商品与冗余展示列同步维护：未关联时两列均置空（与 V2 种子数据口径一致）
    review.setProductId(linkedProduct == null ? null : linkedProduct.getId());
    review.setPurchasedProduct(linkedProduct == null ? null : linkedProduct.getName());
    review.setRating(request.rating() == null ? DEFAULT_RATING : request.rating());
    review.setSortOrder(request.sortOrder() == null ? DEFAULT_SORT_ORDER : request.sortOrder());
    review.setStatus(StringUtils.hasText(request.status()) ? request.status() : StatusConstants.ACTIVE);
  }

  /** 批量装配管理端评价视图：联查商品表补 productName（未关联或商品已删除为 null） */
  private List<AdminReviewVO> assembleVOs(List<Review> reviews) {
    if (reviews == null || reviews.isEmpty()) {
      return List.of();
    }
    List<Long> productIds = reviews.stream()
        .map(Review::getProductId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    Map<Long, String> productNames = productIds.isEmpty() ? Collections.emptyMap()
        : productMapper.selectBatchIds(productIds).stream()
            .collect(Collectors.toMap(Product::getId, Product::getName));
    return reviews.stream()
        .map(review -> new AdminReviewVO(
            review.getId(),
            review.getProductId(),
            review.getBuyerName(),
            review.getPetName(),
            review.getPetBreed(),
            review.getContent(),
            review.getAvatarUrl(),
            review.getPurchasedProduct(),
            review.getRating(),
            review.getSortOrder(),
            review.getStatus(),
            review.getCreatedAt(),
            review.getUpdatedAt(),
            // 未关联（productId 为空）或商品已被逻辑删除时为 null，前端展示「通用」
            review.getProductId() == null ? null : productNames.get(review.getProductId())
        ))
        .toList();
  }

  /** 转义 like 通配符：在 \、%、_ 前加反斜杠，防止用户关键字被当作通配符解释 */
  private static String escapeLike(String keyword) {
    if (!StringUtils.hasText(keyword)) {
      return keyword;
    }
    return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
