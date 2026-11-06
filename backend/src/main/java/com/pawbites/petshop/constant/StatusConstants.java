package com.pawbites.petshop.constant;

/**
 * 状态常量：实体与订单状态字段的取值，与数据库中存储的字符串字面量一一对应。
 */
public final class StatusConstants {

  /** 实体状态：启用/有效。 */
  public static final String ACTIVE = "ACTIVE";
  /** 实体状态：停用/无效。 */
  public static final String INACTIVE = "INACTIVE";
  /** 订单状态：已创建（待处理）。 */
  public static final String CREATED = "CREATED";
  /** 订单状态：已取消。 */
  public static final String CANCELLED = "CANCELLED";
  /** 用户角色：管理员（可访问后台 /api/admin/** 接口）。 */
  public static final String ROLE_ADMIN = "ADMIN";
  /** 用户角色：普通用户（默认角色，仅可访问 C 端接口）。 */
  public static final String ROLE_USER = "USER";
  /** 订单取消方：用户本人（C 端取消链路写入 orders.cancel_by）。 */
  public static final String CANCEL_BY_USER = "USER";
  /** 订单取消方：管理员（管理端兜底取消链路写入 orders.cancel_by）。 */
  public static final String CANCEL_BY_ADMIN = "ADMIN";

  /** 常量类，禁止实例化。 */
  private StatusConstants() {
  }
}
