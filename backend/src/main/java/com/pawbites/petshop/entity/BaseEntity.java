package com.pawbites.petshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 实体公共基类
 * <p>不对应独立数据表，定义各业务表共有的公共字段：自增主键、创建/更新时间与逻辑删除标记。</p>
 */
@Data
public abstract class BaseEntity {

  /** 主键 ID（数据库自增） */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 创建时间（数据库默认 CURRENT_TIMESTAMP） */
  @TableField("created_at")
  private LocalDateTime createdAt;

  /** 更新时间（行数据变更时由数据库自动刷新） */
  @TableField("updated_at")
  private LocalDateTime updatedAt;

  /** 逻辑删除标记（0-未删除，1-已删除） */
  @TableLogic
  @TableField("deleted")
  private Boolean deleted;
}
