package com.pawbites.petshop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 更新日志实体
 * <p>对应数据表 update_log，后台「日志管理」页面的数据来源。
 * 不继承 {@link BaseEntity}：本表时间列命名为 create_time/update_time（经驼峰映射对应
 * createTime/updateTime），且无逻辑删除列——删除为物理删除；create_time/update_time
 * 由数据库 DEFAULT CURRENT_TIMESTAMP / ON UPDATE CURRENT_TIMESTAMP 维护。</p>
 */
@Data
@TableName("update_log")
public class UpdateLog {

  /** 主键 ID（数据库自增） */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 日志标题（列表展示与关键字模糊搜索字段） */
  private String title;

  /** 日志类型（varchar 预留扩展，默认「AI自习室通知」） */
  private String type;

  /** 日志正文内容（长文本，仅详情返回） */
  private String content;

  /** 发布状态（0-未发布 1-已发布） */
  private Integer publishStatus;

  /** 点击量（预留字段，本期无 C 端埋点，默认 0） */
  private Integer viewCount;

  /** 创建时间（列表时间列取此字段，创建后不再变动；数据库自动填充） */
  private LocalDateTime createTime;

  /** 更新时间（数据库自动维护） */
  private LocalDateTime updateTime;
}
