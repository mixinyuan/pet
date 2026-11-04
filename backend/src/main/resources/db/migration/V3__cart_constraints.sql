-- V3：购物车唯一约束 + 存量重复数据清理
-- 目的：为 cart / cart_item 补建唯一索引，支撑服务层“原子 upsert 加购”与
-- “并发首建购物车”的并发正确性（先清理存量重复，再建索引，否则索引创建失败）。
--
-- 清理策略说明：采用【物理 DELETE】而非逻辑删除——逻辑删除行（deleted = 1）仍保留在表中，
-- 会继续占用唯一键，导致唯一索引无法建立、且后续 upsert 命中死行。学习项目无审计要求，物理删除最干净。

-- 1. 先清理挂在“待删除重复购物车”上的条目（满足外键 fk_cart_item_cart，购物车须先于其条目删除）
--    待删购物车 = 同一 user_id 或同一 guest_id 下存在更大 id 的行（保留最大 id）。
--    服务层保证 user_id 与 guest_id 互斥（登录车只填 user_id，游客车只填 guest_id），两组规则互不干扰。
DELETE FROM cart_item
WHERE cart_id IN (
  SELECT doomed.id
  FROM cart doomed
  JOIN cart newer
    ON doomed.id < newer.id
   AND (
        (doomed.user_id IS NOT NULL AND doomed.user_id = newer.user_id)
     OR (doomed.guest_id IS NOT NULL AND doomed.guest_id = newer.guest_id)
   )
);

-- 2. 清理 cart 存量重复：同一 user_id（或 guest_id）仅保留最大 id 的行，其余物理删除
--    同一用户重复购物车：
DELETE c1 FROM cart c1
JOIN cart c2 ON c1.user_id IS NOT NULL AND c1.user_id = c2.user_id AND c1.id < c2.id;

--    同一游客重复购物车：
DELETE c1 FROM cart c1
JOIN cart c2 ON c1.guest_id IS NOT NULL AND c1.guest_id = c2.guest_id AND c1.id < c2.id;

-- 3. 清理 cart_item 存量重复：同一购物车内同一商品仅保留最大 id 的行，其余物理删除
DELETE i1 FROM cart_item i1
JOIN cart_item i2 ON i1.cart_id = i2.cart_id AND i1.product_id = i2.product_id AND i1.id < i2.id;

-- 4. 建唯一索引
--    user_id / guest_id 均可 NULL：MySQL 唯一索引对 NULL 不判重（允许多个 NULL 并存），
--    因此登录用户行（guest_id 为 NULL）之间、游客行（user_id 为 NULL）之间互不冲突，
--    索引只会约束“同一用户 / 同一游客至多一辆购物车”。
--    同时删除 V1 的同列普通索引 idx_cart_user / idx_cart_guest：唯一索引最左前缀已完全覆盖
--    其查询能力，保留双份同列索引只会造成写放大。
ALTER TABLE cart
  DROP INDEX idx_cart_user,
  DROP INDEX idx_cart_guest,
  ADD UNIQUE KEY uk_cart_user (user_id),
  ADD UNIQUE KEY uk_cart_guest (guest_id);

--    同一购物车内同一商品至多一条有效条目，支撑加购 ON DUPLICATE KEY UPDATE 原子合并。
--    注：uk_cart_item 的最左前缀覆盖原 idx_cart_item_cart (cart_id)，外键仍可使用，原索引保留不影响正确性。
ALTER TABLE cart_item
  ADD UNIQUE KEY uk_cart_item (cart_id, product_id);
