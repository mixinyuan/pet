package com.pawbites.petshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 邮件订阅实体
 * <p>对应数据表 subscription，存储订阅商城资讯的邮箱地址。</p>
 */
@Data
@TableName("subscription")
@EqualsAndHashCode(callSuper = true)
public class Subscription extends BaseEntity {

  /** 订阅邮箱（全局唯一） */
  private String email;
  /** 订阅状态（默认 ACTIVE） */
  private String status;
}
