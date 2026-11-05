package com.pawbites.petshop.vo.log;

import java.util.List;

/**
 * 日志分页结果
 * <p>日志管理列表接口的响应结构：{total, list}，list 为当前页数据。</p>
 *
 * @param total 关键字过滤后的总记录数
 * @param list  当前页日志列表
 */
public record UpdateLogPageVO(
    long total,
    List<UpdateLogVO> list
) {
}
