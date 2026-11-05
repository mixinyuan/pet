-- V6：新增收藏与优惠券三张表（user_favorite / coupon / user_coupon）
-- 说明：建表细节对齐 V1/V5（InnoDB + utf8mb4/utf8mb4_unicode_ci + DATETIME 默认值），
-- 注释策略对齐 V4/V5（表注释 + 全部字段中文 COMMENT）。
-- 时间列命名对齐 update_log（create_time），均为运营/用户侧数据，无逻辑删除列（删除为物理删除）。
-- 金额单位全链路一致：product.price 与 orders.total_amount 为 DECIMAL(10,2)、单位元，
-- 因此 coupon.amount / coupon.min_spend 采用 INT、单位同为元（amount=15 即立减 15 元，
-- min_spend=99 即满 99 元可用），后端计算时以 BigDecimal.valueOf 换算比较，不引入分单位。

-- ============ user_favorite 用户收藏表 ============
CREATE TABLE user_favorite (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '用户 id（外键 → users.id，收藏需登录，游客不可收藏）',
  product_id BIGINT NOT NULL COMMENT '商品 id（外键 → product.id）',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间（数据库自动填充）',
  UNIQUE KEY uk_user_favorite (user_id, product_id),
  KEY idx_user_favorite_product (product_id),
  CONSTRAINT fk_user_favorite_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_user_favorite_product FOREIGN KEY (product_id) REFERENCES product (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收藏表（uk_user_favorite 保证一人一商品一条，重复收藏幂等）';

-- ============ coupon 优惠券模板表 ============
CREATE TABLE coupon (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  name VARCHAR(64) NOT NULL COMMENT '券名称（如：新人专享券；发放时按名称定位模板）',
  discount_type VARCHAR(32) NOT NULL DEFAULT 'FIXED_AMOUNT' COMMENT '优惠类型（FIXED_AMOUNT-固定满减：满 min_spend 减 amount；预留折扣率等扩展）',
  amount INT NOT NULL COMMENT '抵扣金额（单位：元，与 product.price 单位一致；下单时从商品总额中一次性扣减）',
  min_spend INT NOT NULL DEFAULT 0 COMMENT '使用门槛（单位：元；订单商品总额满该值才可用，0-无门槛）',
  valid_days INT NOT NULL DEFAULT 30 COMMENT '有效天数（发放时按 发放时间 + valid_days 计算 user_coupon.expires_at）',
  remark VARCHAR(255) NULL COMMENT '备注说明（发放场景与规则文案）',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（数据库自动填充）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券模板表（本期仅固定满减券：满 min_spend 元减 amount 元）';

-- ============ user_coupon 用户持券表 ============
CREATE TABLE user_coupon (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键（用户持券实例 id，下单接口 couponId 传此 id 而非模板 id）',
  user_id BIGINT NOT NULL COMMENT '持有用户 id（外键 → users.id，游客不可持券、下单不可用券）',
  coupon_id BIGINT NOT NULL COMMENT '券模板 id（外键 → coupon.id，名称/金额/门槛随模板读取）',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0-未使用 1-已使用；2-已过期为展示态：按 expires_at 懒判定输出，不回写本列）',
  received_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发放时间（received_at + 模板 valid_days = expires_at）',
  expires_at DATETIME NOT NULL COMMENT '过期时间（发放时按模板 valid_days 计算写入）',
  used_at DATETIME NULL COMMENT '核销时间（下单用券时写入；取消订单返还券时清空）',
  order_id BIGINT NULL COMMENT '核销关联订单 id（外键 → orders.id；下单用券时写入，取消订单返还时清空）',
  KEY idx_user_coupon_user_status (user_id, status),
  KEY idx_user_coupon_order (order_id),
  CONSTRAINT fk_user_coupon_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_user_coupon_coupon FOREIGN KEY (coupon_id) REFERENCES coupon (id),
  CONSTRAINT fk_user_coupon_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户持券表（发放/核销/返还生命周期；取消订单时 status 改回 0 并清空 used_at/order_id，即使已过期也按未使用返还）';

-- 种子数据：新人专享券模板（注册成功自动发放一张；登录态下提交订阅也发放一张；
-- 满 99 元减 15 元，自发放起 30 天有效）
INSERT INTO coupon (name, discount_type, amount, min_spend, valid_days, remark) VALUES
('新人专享券', 'FIXED_AMOUNT', 15, 99, 30, '注册或订阅发放：满99减15，自发放起30天内有效');
