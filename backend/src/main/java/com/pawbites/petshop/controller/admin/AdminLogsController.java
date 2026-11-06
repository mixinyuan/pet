package com.pawbites.petshop.controller.admin;
import jakarta.annotation.Resource;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pawbites.petshop.dto.log.CreateLogRequest;
import com.pawbites.petshop.dto.log.UpdateLogRequest;
import com.pawbites.petshop.dto.log.UpdateLogStatusRequest;
import com.pawbites.petshop.result.ApiResponse;
import com.pawbites.petshop.service.UpdateLogService;
import com.pawbites.petshop.vo.log.UpdateLogDetailVO;
import com.pawbites.petshop.vo.log.UpdateLogPageVO;

/**
 * 管理端日志控制器
 * <p>路由前缀 /api/admin/logs，由旧 LogsController（/api/logs）整体迁移而来：
 * 六个接口（分页、详情、新增、编辑、发布状态切换、删除）的行为与响应结构完全一致，
 * 直接复用原 UpdateLogService 与 dto/log、vo/log，不重复造轮子；差异仅两处——
 * 路径迁入 /api/admin 前缀、由 SecurityConfig 统一约束 /api/admin/** 仅 ADMIN 角色可访问
 * （修复旧 /api/logs 仅要求登录、任何用户可改日志的越权隐患）；
 * 类级 @Validated 开启查询参数校验（如关键字长度限制）。</p>
 */
@Validated
@RestController
@RequestMapping("/api/admin/logs")
public class AdminLogsController {

  /** 更新日志服务（沿用原日志管理链路的服务与 DTO/VO） */
  @Resource
  private UpdateLogService updateLogService;

  /**
   * 分页查询日志（GET /api/admin/logs）
   * <p>按标题关键字模糊匹配，create_time 倒序；页码下限 1、pageSize 上限 100。</p>
   *
   * @param page     页码，从 1 开始，默认 1
   * @param pageSize 每页条数，默认 10，上限 100
   * @param keyword  标题关键字模糊搜索，可选，最长 64 字符
   * @return 分页结果（total 与当前页列表，时间格式 yyyy-MM-dd HH:mm）
   */
  @GetMapping
  public ApiResponse<UpdateLogPageVO> list(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "10") long pageSize,
      @RequestParam(required = false)
      @Size(max = 64, message = "关键字最长 64 字符")
      String keyword
  ) {
    return ApiResponse.success(updateLogService.pageLogs(page, pageSize, keyword));
  }

  /**
   * 查询日志详情（GET /api/admin/logs/{id}）
   *
   * @param id 日志 id
   * @return 日志详情（含 content 正文）
   * @throws com.pawbites.petshop.exception.BusinessException 日志不存在（404）时抛出
   */
  @GetMapping("/{id}")
  public ApiResponse<UpdateLogDetailVO> detail(@PathVariable Long id) {
    return ApiResponse.success(updateLogService.getLog(id));
  }

  /**
   * 新增日志（POST /api/admin/logs）
   *
   * @param request 新增请求（title/content 必填；type 为空默认「AI自习室通知」；status 缺省 0）
   * @return 新增后的日志详情
   */
  @PostMapping
  public ApiResponse<UpdateLogDetailVO> create(@Valid @RequestBody CreateLogRequest request) {
    return ApiResponse.success(updateLogService.createLog(request));
  }

  /**
   * 编辑日志（PUT /api/admin/logs/{id}）
   * <p>仅编辑 title/type/content；发布状态请走 /status 接口。</p>
   *
   * @param id      日志 id
   * @param request 编辑请求（title/type/content，title/content 必填）
   * @return 编辑后的日志详情
   * @throws com.pawbites.petshop.exception.BusinessException 日志不存在（404）时抛出
   */
  @PutMapping("/{id}")
  public ApiResponse<UpdateLogDetailVO> update(@PathVariable Long id, @Valid @RequestBody UpdateLogRequest request) {
    return ApiResponse.success(updateLogService.updateLog(id, request));
  }

  /**
   * 切换发布状态（PUT /api/admin/logs/{id}/status）
   * <p>只更新发布状态，不改动 create_time。</p>
   *
   * @param id      日志 id
   * @param request 状态请求（status：0-未发布 1-已发布）
   * @return 无数据的成功响应
   * @throws com.pawbites.petshop.exception.BusinessException 日志不存在（404）时抛出
   */
  @PutMapping("/{id}/status")
  public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateLogStatusRequest request) {
    updateLogService.updateStatus(id, request.status());
    return ApiResponse.success();
  }

  /**
   * 删除日志（DELETE /api/admin/logs/{id}）
   * <p>物理删除，不可恢复。</p>
   *
   * @param id 日志 id
   * @return 无数据的成功响应
   * @throws com.pawbites.petshop.exception.BusinessException 日志不存在（404）时抛出
   */
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    updateLogService.deleteLog(id);
    return ApiResponse.success();
  }
}
