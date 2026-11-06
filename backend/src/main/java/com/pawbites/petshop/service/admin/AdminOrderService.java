package com.pawbites.petshop.service.admin;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pawbites.petshop.entity.OrderEntity;
import com.pawbites.petshop.result.PageResult;
import com.pawbites.petshop.vo.admin.AdminOrderDetailVO;
import com.pawbites.petshop.vo.admin.AdminOrderVO;

import java.time.LocalDate;

/**
 * 管理端订单服务
 * <p>提供后台订单查阅与管理员兜底取消：多条件分页查询（订单号精确/下单人模糊/
 * 状态/下单时间范围）、订单详情（商品快照 + 用券信息 + 下单用户信息）、
 * 管理员取消（复用 C 端同一套库存回补与退券逻辑）。</p>
 */
public interface AdminOrderService extends IService<OrderEntity> {

  /**
   * 分页查询订单（后台视角，用户单与游客单均可见）
   * <p>支持订单号精确匹配、关键字模糊匹配下单用户（用户名/昵称）或游客单 guestId 前缀、
   * 状态筛选（CREATED/CANCELLED）与下单日期闭区间范围；结果按订单 id 倒序。
   * records 附带下单人展示名、商品件数汇总与用券信息（经 user_coupon.order_id 反查模板）。</p>
   *
   * @param page    页码（从 1 开始，小于 1 时按 1 处理，大于 10000 时按 10000 处理）
   * @param size    每页数量（clamp 到 [1, 100]）
   * @param orderNo 订单号精确查询（null 或空白表示不过滤）
   * @param keyword 关键字（null 或空白表示不过滤；匹配下单用户名/昵称 like，游客单匹配 guestId 前缀）
   * @param status  订单状态筛选（CREATED/CANCELLED，null 或空白表示不过滤）
   * @param from    下单日期起（含当天，格式 yyyy-MM-dd；null 表示不限）
   * @param to      下单日期止（含当天，格式 yyyy-MM-dd；null 表示不限）
   * @return 管理端订单视图分页结果
   * @throws com.pawbites.petshop.exception.BusinessException status 取值非法、from 晚于 to（400）时抛出
   */
  PageResult<AdminOrderVO> pageOrders(long page, long size, String orderNo, String keyword, String status,
      LocalDate from, LocalDate to);

  /**
   * 查询订单详情（后台视角）
   * <p>在列表行字段之上补充商品快照列表、商品合计（itemsSubtotal）与下单用户信息
   * （username/nickname/email，游客单为 null）。</p>
   *
   * @param id 订单 id
   * @return 管理端订单详情视图
   * @throws com.pawbites.petshop.exception.BusinessException 订单不存在（404）时抛出
   */
  AdminOrderDetailVO getOrderDetail(Long id);

  /**
   * 管理员取消订单
   * <p>仅 CREATED 状态可取消；内部委托 {@link com.pawbites.petshop.service.OrderService#cancelOrderByAdmin}
   * 与 C 端取消共用同一套原子逻辑（条件更新抢占 + 回补库存 + 返还优惠券），
   * 并写入 cancel_time=当前时间、cancel_by=ADMIN。取消不可逆。</p>
   *
   * @param id 订单 id
   * @return 取消后的订单详情（状态已置为 CANCELLED）
   * @throws com.pawbites.petshop.exception.BusinessException 订单不存在（404）、订单状态不允许取消（400）时抛出
   */
  AdminOrderDetailVO cancelOrder(Long id);
}
