-- V9：场景套餐支持加购与下单（cart_item / order_item 引入条目类型）
--
-- 背景：购物车与订单条目此前只认商品（product_id 外键指向 product）。
-- 套餐要按套餐价结算，需要让两张表能承载套餐条目。
--
-- 设计：新增 item_type 区分条目类型（PRODUCT-单品 / BUNDLE-套餐）；
-- product_id 字段语义放宽为"关联 id"（单品存商品 id、套餐存套餐 id）。
-- 由于套餐 id 与商品 id 分属两套自增序列、可能撞号，原有外键必须移除
-- （完整性由应用层保证，与本工程逻辑删除/应用层校验的既有风格一致）；
-- cart_item 的原子 upsert 依赖的唯一索引 uk_cart_item 同步把 item_type 纳入，
-- 保证"同一购物车内同类型同关联 id"仍唯一（商品 #5 与套餐 #5 不再冲突）。
-- 存量行 item_type 落默认值 'PRODUCT'，历史数据无需回填。

-- ---------------- cart_item ----------------
ALTER TABLE cart_item
  ADD COLUMN item_type VARCHAR(16) NOT NULL DEFAULT 'PRODUCT'
    COMMENT '条目类型：PRODUCT-单品 BUNDLE-套餐';

ALTER TABLE cart_item DROP FOREIGN KEY fk_cart_item_product;

ALTER TABLE cart_item
  MODIFY product_id BIGINT NOT NULL COMMENT '关联 id：PRODUCT 行为商品 id，BUNDLE 行为套餐 id';

ALTER TABLE cart_item
  DROP INDEX uk_cart_item,
  ADD UNIQUE KEY uk_cart_item (cart_id, item_type, product_id);

-- ---------------- order_item ----------------
-- 订单条目同样支持套餐快照行：product_name/price 存套餐名与套餐价（快照语义不变）。
ALTER TABLE order_item
  ADD COLUMN item_type VARCHAR(16) NOT NULL DEFAULT 'PRODUCT'
    COMMENT '条目类型：PRODUCT-单品 BUNDLE-套餐';

ALTER TABLE order_item DROP FOREIGN KEY fk_order_item_product;

ALTER TABLE order_item
  MODIFY product_id BIGINT NOT NULL COMMENT '关联 id：PRODUCT 行为商品 id，BUNDLE 行为套餐 id';
