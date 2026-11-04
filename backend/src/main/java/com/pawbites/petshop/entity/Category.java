package com.pawbites.petshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("category")
@EqualsAndHashCode(callSuper = true)
public class Category extends BaseEntity {

  private String code;
  private String name;
  private String description;
  private Integer sortOrder;
  private String status;
}
