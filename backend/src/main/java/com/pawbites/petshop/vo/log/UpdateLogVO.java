package com.pawbites.petshop.vo.log;

/**
 * 日志列表项
 * <p>后台日志管理列表的单行数据；createTime 由服务端格式化为 yyyy-MM-dd HH:mm 字符串。</p>
 */
public record UpdateLogVO(
    /** 日志 ID */
    Long id,
    /** 日志标题 */
    String title,
    /** 日志类型（如「AI自习室通知」） */
    String type,
    /** 创建时间（格式 yyyy-MM-dd HH:mm） */
    String createTime,
    /** 发布状态（0-未发布 1-已发布） */
    Integer publishStatus,
    /** 点击量（预留字段，默认 0） */
    Integer viewCount
) {
}
