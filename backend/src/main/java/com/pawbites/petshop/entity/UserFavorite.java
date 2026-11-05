package com.pawbites.petshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户收藏实体
 * <p>对应数据表 user_favorite，存储登录用户对商品的收藏关系。
 * 不继承 {@link BaseEntity}：本表时间列命名为 create_time（驼峰映射 createTime），
 * 且无逻辑删除列——取消收藏为物理删除（uk_user_favorite 唯一键保证重复收藏幂等），
 * create_time 由数据库 DEFAULT CURRENT_TIMESTAMP 维护。</p>
 */
@Data
@TableName("user_favorite")
public class UserFavorite {

  /** 主键 ID（数据库自增） */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 用户 id（收藏需登录，游客不可收藏） */
  private Long userId;

  /** 商品 id */
  private Long productId;

  /** 收藏时间（数据库自动填充） */
  private LocalDateTime createTime;
}
