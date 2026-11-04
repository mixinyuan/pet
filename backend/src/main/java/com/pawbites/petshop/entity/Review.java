package com.pawbites.petshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品评价实体
 * <p>对应数据表 review，存储买家对商品的评价及展示信息。</p>
 */
@Data
@TableName("review")
@EqualsAndHashCode(callSuper = true)
public class Review extends BaseEntity {

  /** 关联商品 ID（外键关联 product.id） */
  private Long productId;
  /** 买家名称 */
  private String buyerName;
  /** 宠物名字 */
  private String petName;
  /** 宠物品种 */
  private String petBreed;
  /** 评价内容 */
  private String content;
  /** 买家头像 URL */
  private String avatarUrl;
  /** 所购商品名称（评价展示用） */
  private String purchasedProduct;
  /** 评分（0.0-5.0，默认 5.0） */
  private BigDecimal rating;
  /** 排序序号（数值越小越靠前，默认 0） */
  private Integer sortOrder;
  /** 评价状态（默认 ACTIVE） */
  private String status;
}
