package com.pawbites.petshop.vo.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端评价视图
 * <p>后台评价列表/详情/新增回显/编辑回显共用；含 review 全部字段，
 * 另附 productName（关联商品名，未关联或商品已删除时为 null，前端展示「通用」）。</p>
 */
public record AdminReviewVO(
    /** 评价 ID */
    Long id,
    /** 关联商品 ID（null 表示通用评价） */
    Long productId,
    /** 评价人姓名 */
    String buyerName,
    /** 宠物名字 */
    String petName,
    /** 宠物品种 */
    String petBreed,
    /** 评价内容 */
    String content,
    /** 评价人头像 URL */
    String avatarUrl,
    /** 所购商品名称（冗余展示列，随关联商品名同步维护） */
    String purchasedProduct,
    /** 评分（0.0-5.0，人工维护） */
    BigDecimal rating,
    /** 排序号（越小越靠前，与 C 端展示序一致） */
    Integer sortOrder,
    /** 展示状态（ACTIVE 展示/INACTIVE 隐藏，隐藏后 C 端立即不展示） */
    String status,
    /** 创建时间 */
    LocalDateTime createdAt,
    /** 更新时间 */
    LocalDateTime updatedAt,
    /** 关联商品名（联查 product.name；未关联或商品已删除时为 null） */
    String productName
) {
}
