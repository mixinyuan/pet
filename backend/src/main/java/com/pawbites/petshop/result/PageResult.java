package com.pawbites.petshop.result;

import java.util.List;

/**
 * 分页结果：包装分页查询返回的数据列表与分页信息。
 *
 * @param records 当前页数据列表
 * @param total   总记录数
 * @param page    当前页码（从 1 开始）
 * @param size    每页条数
 */
public record PageResult<T>(List<T> records, long total, long page, long size) {
}
