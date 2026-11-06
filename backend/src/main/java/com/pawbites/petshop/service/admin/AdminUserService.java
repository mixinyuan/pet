package com.pawbites.petshop.service.admin;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pawbites.petshop.entity.User;
import com.pawbites.petshop.result.PageResult;
import com.pawbites.petshop.vo.admin.AdminUserDetailVO;
import com.pawbites.petshop.vo.admin.AdminUserVO;

/**
 * 管理端用户服务
 * <p>提供后台用户查阅与账号禁用/启用：关键字/状态分页查询（附订单数、消费金额、持券数汇总）、
 * 用户详情聚合（订单/收藏/持券/订阅）、账号状态切换（管理员账号不允许禁用）。</p>
 */
public interface AdminUserService extends IService<User> {

  /**
   * 分页查询用户（后台视角）
   * <p>关键字匹配用户名/昵称/邮箱（三字段 like），状态筛选 ACTIVE/INACTIVE；
   * 结果按用户 id 倒序。records 附带汇总字段：orderCount/totalSpend 仅统计有效订单
   * （status=CREATED），couponTotal 为持券总数、couponAvailable 为可用券数
   * （未使用且未过期，与 C 端我的券口径一致）。</p>
   *
   * @param page    页码（从 1 开始，小于 1 时按 1 处理，大于 10000 时按 10000 处理）
   * @param size    每页数量（clamp 到 [1, 100]）
   * @param keyword 关键字（null 或空白表示不过滤，匹配用户名/昵称/邮箱）
   * @param status  账号状态筛选（ACTIVE/INACTIVE，null 或空白表示不过滤）
   * @return 管理端用户视图分页结果（含订单/消费/持券汇总）
   * @throws com.pawbites.petshop.exception.BusinessException status 取值非法（400）时抛出
   */
  PageResult<AdminUserVO> pageUsers(long page, long size, String keyword, String status);

  /**
   * 查询用户详情（聚合视图）
   * <p>基本资料（含角色）+ 全部订单（按订单 id 倒序，含已取消）+ 收藏汇总（总数与最近 5 个商品）
   * + 持券列表（券名/展示状态/过期时间）+ 邮箱订阅状态。</p>
   *
   * @param id 用户 id
   * @return 管理端用户详情视图
   * @throws com.pawbites.petshop.exception.BusinessException 用户不存在（404）时抛出
   */
  AdminUserDetailVO getUserDetail(Long id);

  /**
   * 切换账号状态（禁用/启用）
   * <p>禁用（INACTIVE）不允许作用于 role=ADMIN 的账号（防止自锁）；
   * 启用（ACTIVE）无限制。禁用后该用户已签发的 token 会被 JwtAuthenticationFilter
   * 的 status 回查即时拒绝（P0 已实现，本方法无需额外处理）。</p>
   *
   * @param id     用户 id
   * @param status 目标状态（ACTIVE 启用 / INACTIVE 禁用）
   * @throws com.pawbites.petshop.exception.BusinessException 用户不存在（404）、
   *                                                          管理员账号不允许禁用（400）时抛出
   */
  void updateStatus(Long id, String status);
}
