-- V7：后台管理 P0 地基——users 加角色列、orders 加取消审计列
-- 说明：注释策略对齐 V4/V5/V6（中文 COMMENT）；项目 Flyway 为关闭状态，本脚本手工执行。
-- users.role 存量行由 DEFAULT 'USER' 自动回填为普通用户，管理员由 SQL 手工指定
-- （UPDATE users SET role='ADMIN' WHERE username='xxx'，本期不开放注册管理员）；
-- orders 两列仅在取消订单时写入，本期只落表结构与实体映射，业务写入在后续阶段实现。

-- ============ users 用户表：新增角色列 ============
ALTER TABLE users
  ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT 'USER' COMMENT '角色：USER-普通用户 / ADMIN-管理员';

-- ============ orders 订单表：新增取消审计列 ============
ALTER TABLE orders
  ADD COLUMN cancel_time DATETIME NULL COMMENT '订单取消时间',
  ADD COLUMN cancel_by VARCHAR(16) NULL COMMENT '取消方：USER-用户 / ADMIN-管理员';
