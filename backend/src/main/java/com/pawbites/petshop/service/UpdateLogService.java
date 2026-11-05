package com.pawbites.petshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pawbites.petshop.dto.log.CreateLogRequest;
import com.pawbites.petshop.dto.log.UpdateLogRequest;
import com.pawbites.petshop.entity.UpdateLog;
import com.pawbites.petshop.vo.log.UpdateLogDetailVO;
import com.pawbites.petshop.vo.log.UpdateLogPageVO;

/**
 * 更新日志服务
 * <p>提供后台日志管理页面的分页查询、详情、新增、编辑、发布状态切换与删除能力。</p>
 */
public interface UpdateLogService extends IService<UpdateLog> {

  /**
   * 分页查询日志
   * <p>按标题关键字模糊匹配（LIKE %keyword%），create_time、id 倒序（新日志在前）；
   * 页码下限 1、每页条数 clamp 到 [1, 100]。</p>
   *
   * @param page     页码，从 1 开始
   * @param pageSize 每页条数，上限 100
   * @param keyword  标题关键字，可选
   * @return 分页结果（total + 当前页列表）
   */
  UpdateLogPageVO pageLogs(long page, long pageSize, String keyword);

  /**
   * 查询日志详情
   *
   * @param id 日志 id
   * @return 日志详情（含 content）
   * @throws com.pawbites.petshop.exception.BusinessException 日志不存在（404）时抛出
   */
  UpdateLogDetailVO getLog(Long id);

  /**
   * 新增日志
   *
   * @param request 新增请求（title/type/content/status，type 为空默认「AI自习室通知」，status 缺省 0）
   * @return 新增后的日志详情
   */
  UpdateLogDetailVO createLog(CreateLogRequest request);

  /**
   * 编辑日志（仅 title/type/content）
   *
   * @param id      日志 id
   * @param request 编辑请求（title/type/content）
   * @return 编辑后的日志详情
   * @throws com.pawbites.petshop.exception.BusinessException 日志不存在（404）时抛出
   */
  UpdateLogDetailVO updateLog(Long id, UpdateLogRequest request);

  /**
   * 切换发布状态
   * <p>只更新 publish_status 列，不改动 create_time。</p>
   *
   * @param id     日志 id
   * @param status 目标状态（0-未发布 1-已发布）
   * @throws com.pawbites.petshop.exception.BusinessException 日志不存在（404）时抛出
   */
  void updateStatus(Long id, Integer status);

  /**
   * 删除日志（物理删除）
   *
   * @param id 日志 id
   * @throws com.pawbites.petshop.exception.BusinessException 日志不存在（404）时抛出
   */
  void deleteLog(Long id);
}
