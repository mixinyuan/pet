package com.pawbites.petshop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体
 * <p>对应数据表 users，存储商城注册用户的账号信息。</p>
 */
@Data
@TableName("users")
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {

  /** 用户名（登录账号，全局唯一） */
  private String username;
  /** 邮箱（全局唯一） */
  private String email;
  /** 密码哈希（加密存储的登录密码） */
  private String passwordHash;
  /** 昵称（对外展示的名称） */
  private String nickname;
  /** 账号状态（默认 ACTIVE） */
  private String status;
  /** 角色（USER-普通用户 ADMIN-管理员，默认 USER，管理员由 SQL 手工指定） */
  private String role;
}
