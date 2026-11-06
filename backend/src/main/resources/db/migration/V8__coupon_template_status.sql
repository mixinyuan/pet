-- V8：后台管理 P4——优惠券模板表新增状态列
-- 说明：注释策略对齐 V4/V5/V6/V7（中文 COMMENT）；项目 Flyway 为关闭状态，本脚本手工执行。
-- status 存量行由 DEFAULT 'ACTIVE' 自动回填为启用；停用（INACTIVE）后注册/订阅不再自动发放该模板，
-- 存量已发放的持券实例不受影响（照常展示与核销）。

-- ============ coupon 优惠券模板表：新增状态列 ============
ALTER TABLE coupon
  ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-启用（可被注册/订阅自动发放）/ INACTIVE-停用（不再自动发放，存量持券不受影响）';
