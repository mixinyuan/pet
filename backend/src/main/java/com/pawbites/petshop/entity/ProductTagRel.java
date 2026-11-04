package com.pawbites.petshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品-标签关联实体
 * <p>对应数据表 product_tag_rel，维护商品与标签的多对多关联关系。</p>
 */
@Data
@TableName("product_tag_rel")
@EqualsAndHashCode(callSuper = true)
public class ProductTagRel extends BaseEntity {

  /** 商品 ID（外键关联 product.id） */
  private Long productId;
  /** 标签 ID（外键关联 product_tag.id） */
  private Long tagId;
}
