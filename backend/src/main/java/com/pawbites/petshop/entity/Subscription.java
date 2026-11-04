package com.pawbites.petshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("subscription")
@EqualsAndHashCode(callSuper = true)
public class Subscription extends BaseEntity {

  private String email;
  private String status;
}
