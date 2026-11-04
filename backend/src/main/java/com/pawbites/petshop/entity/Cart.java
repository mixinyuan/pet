package com.pawbites.petshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 购物车实体
 * <p>对应数据表 cart，每位注册用户或游客各对应一辆购物车。</p>
 */
@Data
@TableName("cart")
@EqualsAndHashCode(callSuper = true)
public class Cart extends BaseEntity {

  /** 所属用户 ID（外键关联 users.id，游客购物车为空） */
  private Long userId;
  /** 游客标识（游客购物车的身份凭据，登录用户购物车为空） */
  private String guestId;
}
