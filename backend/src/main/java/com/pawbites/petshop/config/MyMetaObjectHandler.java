package com.pawbites.petshop.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import java.time.LocalDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

/**
 * MyBatis-Plus 字段自动填充处理器：配合 {@link com.pawbites.petshop.entity.BaseEntity} 中
 * {@code @TableField(fill = ...)} 注解，在插入/更新时自动填充 createdAt、updatedAt 时间字段。
 *
 * <p>解决此前依赖数据库 DEFAULT CURRENT_TIMESTAMP 导致 insert 后实体字段为 null、
 * 接口（如下单）返回 createdAt=null 的问题：填充发生在写库前，实体与数据库行保持一致。
 * 数据库列 created_at/updated_at 通过 map-underscore-to-camel-case 与实体驼峰字段映射。
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

  /**
   * 插入填充：createdAt 与 updatedAt 均填入当前时间。
   *
   * @param metaObject 待填充的实体元对象
   */
  @Override
  public void insertFill(MetaObject metaObject) {
    this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
    this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
  }

  /**
   * 更新填充：仅填入 updatedAt 为当前时间。
   *
   * @param metaObject 待填充的实体元对象
   */
  @Override
  public void updateFill(MetaObject metaObject) {
    this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
  }
}
