package com.pawbites.petshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("review")
@EqualsAndHashCode(callSuper = true)
public class Review extends BaseEntity {

  private Long productId;
  private String buyerName;
  private String petName;
  private String petBreed;
  private String content;
  private String avatarUrl;
  private String purchasedProduct;
  private BigDecimal rating;
  private Integer sortOrder;
  private String status;
}
