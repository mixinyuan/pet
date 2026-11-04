package com.pawbites.petshop.common.config;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public abstract class BaseEntity {

  @TableId(type = IdType.AUTO)
  private Long id;

  @TableField("created_at")
  private LocalDateTime createdAt;

  @TableField("updated_at")
  private LocalDateTime updatedAt;

  @TableLogic
  @TableField("deleted")
  private Boolean deleted;
}
