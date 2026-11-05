package com.pawbites.petshop.vo.log;

/**
 * 日志详情
 * <p>编辑弹窗回填使用，较列表项多返回正文 content 与更新时间。</p>
 */
public record UpdateLogDetailVO(
    /** 日志 ID */
    Long id,
    /** 日志标题 */
    String title,
    /** 日志类型（如「AI自习室通知」） */
    String type,
    /** 日志正文内容 */
    String content,
    /** 发布状态（0-未发布 1-已发布） */
    Integer publishStatus,
    /** 点击量（预留字段，默认 0） */
    Integer viewCount,
    /** 创建时间（格式 yyyy-MM-dd HH:mm） */
    String createTime,
    /** 更新时间（格式 yyyy-MM-dd HH:mm） */
    String updateTime
) {
}
