-- V5：新增更新日志表 update_log（后台「日志管理」页面数据表）
-- 说明：建表细节对齐 V1（InnoDB + utf8mb4/utf8mb4_unicode_ci + DATETIME 时间戳默认值），
-- 注释策略对齐 V4（表注释 + 全部字段中文 COMMENT）。
-- 与其余业务表的差异：管理端数据无逻辑删除列（删除为物理删除），时间列命名为
-- create_time/update_time（接口契约要求），列表时间列展示 create_time，
-- 切换发布状态只改 publish_status、不改动 create_time。

CREATE TABLE update_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  title VARCHAR(128) NOT NULL COMMENT '日志标题（列表展示与关键字模糊搜索的匹配字段）',
  type VARCHAR(64) NOT NULL DEFAULT 'AI自习室通知' COMMENT '日志类型（varchar 预留扩展，默认「AI自习室通知」，后续可新增「产品更新」「公告」等）',
  content TEXT NOT NULL COMMENT '日志正文内容（长文本，详情接口返回）',
  publish_status TINYINT NOT NULL DEFAULT 0 COMMENT '发布状态（0-未发布 1-已发布；切换不影响 create_time）',
  view_count INT NOT NULL DEFAULT 0 COMMENT '点击量（预留字段，本期无 C 端埋点，恒为 0）',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（列表时间列取此字段，创建后不再变动）',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（自动维护）',
  KEY idx_update_log_title (title),
  KEY idx_update_log_publish_status (publish_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='更新日志表（后台日志管理：通知/更新日志的新增、编辑、发布与删除）';

-- 种子数据：便于日志管理页面联调（含一条「冒烟测试」标题，类型统一为「AI自习室通知」）
INSERT INTO update_log (title, type, content, publish_status, view_count, create_time, update_time) VALUES
('冒烟测试：日志管理功能上线', 'AI自习室通知', '用于日志管理页面联调的冒烟测试数据：验证列表分页、标题关键字搜索、发布状态 Switch 切换、编辑回填与删除等整条链路。', 1, 0, '2026-11-01 09:30:00', '2026-11-01 09:30:00'),
('AI自习室本周开放时间调整', 'AI自习室通知', 'AI自习室本周四 18:00-21:00 例行维护，期间座位暂停预约，请合理安排自习时间。', 1, 12, '2026-11-02 18:00:00', '2026-11-02 18:00:00'),
('AI自习室新增资料打印服务预告', 'AI自习室通知', '下周起各自习室将陆续上线资料打印服务，具体开放时间以后续通知为准。', 0, 0, '2026-11-03 21:15:00', '2026-11-03 21:15:00');
