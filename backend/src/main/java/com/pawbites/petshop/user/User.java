package com.pawbites.petshop.user;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pawbites.petshop.common.config.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("users")
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {

  private String username;
  private String email;
  private String passwordHash;
  private String nickname;
  private String status;
}
