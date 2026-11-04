package com.pawbites.petshop.subscription;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pawbites.petshop.common.config.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("subscription")
@EqualsAndHashCode(callSuper = true)
public class Subscription extends BaseEntity {

  private String email;
  private String status;
}
