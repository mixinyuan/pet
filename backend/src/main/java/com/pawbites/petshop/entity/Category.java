package com.pawbites.petshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品分类实体
 * <p>对应数据表 category，存储商城的商品分类目录。</p>
 */
@Data
@TableName("category")
@EqualsAndHashCode(callSuper = true)
public class Category extends BaseEntity {

  /** 分类编码（全局唯一） */
  private String code;
  /** 分类名称 */
  private String name;
  /** 分类描述 */
  private String description;
  /** 排序序号（数值越小越靠前，默认 0） */
  private Integer sortOrder;
  /** 分类状态（默认 ACTIVE） */
  private String status;
}
