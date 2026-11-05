package com.pawbites.petshop.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pawbites.petshop.service.UpdateLogService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.pawbites.petshop.dto.log.CreateLogRequest;
import com.pawbites.petshop.dto.log.UpdateLogRequest;
import com.pawbites.petshop.entity.UpdateLog;
import com.pawbites.petshop.exception.BusinessException;
import com.pawbites.petshop.mapper.UpdateLogMapper;
import com.pawbites.petshop.vo.log.UpdateLogDetailVO;
import com.pawbites.petshop.vo.log.UpdateLogPageVO;
import com.pawbites.petshop.vo.log.UpdateLogVO;

/**
 * 更新日志服务实现
 * <p>pageLogs 按标题关键字（通配符经 escapeLike 转义）模糊分页查询，create_time、id 倒序，
 * 页码 clamp 到 [1, 10000]、每页条数 clamp 到 [1, 100]；
 * create/update 均以 detail VO 回显；updateStatus 走 LambdaUpdateWrapper 只更新
 * publish_status 列（不触碰 create_time）；本表无逻辑删除列，removeById 即物理删除。</p>
 */
@Service
public class UpdateLogServiceImpl extends ServiceImpl<UpdateLogMapper, UpdateLog> implements UpdateLogService {

  /** 日志类型默认值（type 为空时使用） */
  public static final String DEFAULT_TYPE = "AI自习室通知";

  /** 列表/详情时间列的展示格式（无秒） */
  private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  @Override
  public UpdateLogPageVO pageLogs(long page, long pageSize, String keyword) {
    // 页码 clamp 到 [1, 10000] 防深分页；每页条数 clamp 到 [1, 100]（与分页拦截器单页上限一致）
    long safePage = Math.min(Math.max(page, 1), 10000L);
    long safeSize = Math.min(Math.max(pageSize, 1), 100);
    // 关键字转义 like 通配符后再参与匹配，避免 %、_、\ 被当作通配符解释
    String likeKeyword = escapeLike(StringUtils.hasText(keyword) ? keyword.trim() : null);

    IPage<UpdateLog> logPage = baseMapper.selectPage(
        Page.of(safePage, safeSize),
        Wrappers.<UpdateLog>lambdaQuery()
            .like(StringUtils.hasText(likeKeyword), UpdateLog::getTitle, likeKeyword)
            .orderByDesc(UpdateLog::getCreateTime)
            .orderByDesc(UpdateLog::getId)
    );

    return new UpdateLogPageVO(
        logPage.getTotal(),
        logPage.getRecords().stream().map(this::toListItem).toList()
    );
  }

  @Override
  public UpdateLogDetailVO getLog(Long id) {
    return toDetail(getExistingLog(id));
  }

  @Override
  public UpdateLogDetailVO createLog(CreateLogRequest request) {
    UpdateLog log = new UpdateLog();
    applyEditableFields(log, request.title(), request.type(), request.content());
    log.setPublishStatus(request.status() == null ? 0 : request.status());
    // create_time/update_time/view_count 留空，由数据库默认值填充（view_count 默认 0）
    baseMapper.insert(log);
    // 重查一次，让 create_time 等数据库默认值随详情回显
    return toDetail(getExistingLog(log.getId()));
  }

  @Override
  public UpdateLogDetailVO updateLog(Long id, UpdateLogRequest request) {
    UpdateLog log = getExistingLog(id);
    applyEditableFields(log, request.title(), request.type(), request.content());
    // 只 SET 三个可编辑列：避免 updateById 把旧 update_time 显式写回、
    // 覆盖 ON UPDATE CURRENT_TIMESTAMP 的自动维护
    baseMapper.update(null, Wrappers.<UpdateLog>lambdaUpdate()
        .eq(UpdateLog::getId, id)
        .set(UpdateLog::getTitle, log.getTitle())
        .set(UpdateLog::getType, log.getType())
        .set(UpdateLog::getContent, log.getContent()));
    // 重查一次以数据库最新值为准（拿到自动维护的 update_time）
    return toDetail(getExistingLog(id));
  }

  @Override
  public void updateStatus(Long id, Integer status) {
    getExistingLog(id);
    // 只更新 publish_status 列；create_time 由建表语义保证不变，update_time 由 ON UPDATE 自动维护
    baseMapper.update(null, Wrappers.<UpdateLog>lambdaUpdate()
        .eq(UpdateLog::getId, id)
        .set(UpdateLog::getPublishStatus, status));
  }

  @Override
  public void deleteLog(Long id) {
    getExistingLog(id);
    baseMapper.deleteById(id);
  }

  /** 按 id 查询日志，不存在时抛 404 业务异常 */
  private UpdateLog getExistingLog(Long id) {
    UpdateLog log = baseMapper.selectById(id);
    if (log == null) {
      throw new BusinessException(404, "日志不存在");
    }
    return log;
  }

  /** 应用可编辑字段（title/type/content），type 为空时取默认「AI自习室通知」 */
  private void applyEditableFields(UpdateLog log, String title, String type, String content) {
    log.setTitle(title.trim());
    log.setType(StringUtils.hasText(type) ? type.trim() : DEFAULT_TYPE);
    log.setContent(content.trim());
  }

  /** 实体 → 列表项 VO（时间格式化为 yyyy-MM-dd HH:mm） */
  private UpdateLogVO toListItem(UpdateLog log) {
    return new UpdateLogVO(
        log.getId(),
        log.getTitle(),
        log.getType(),
        formatTime(log.getCreateTime()),
        log.getPublishStatus(),
        log.getViewCount()
    );
  }

  /** 实体 → 详情 VO（含 content） */
  private UpdateLogDetailVO toDetail(UpdateLog log) {
    return new UpdateLogDetailVO(
        log.getId(),
        log.getTitle(),
        log.getType(),
        log.getContent(),
        log.getPublishStatus(),
        log.getViewCount(),
        formatTime(log.getCreateTime()),
        formatTime(log.getUpdateTime())
    );
  }

  /** LocalDateTime → yyyy-MM-dd HH:mm；为空时返回 null */
  private String formatTime(LocalDateTime time) {
    return time == null ? null : DISPLAY_TIME.format(time);
  }

  /** 转义 like 通配符：在 \、%、_ 前加反斜杠，防止用户关键字被当作通配符解释 */
  private static String escapeLike(String keyword) {
    if (!StringUtils.hasText(keyword)) {
      return keyword;
    }
    return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
