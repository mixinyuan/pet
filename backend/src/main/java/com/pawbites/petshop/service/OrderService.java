package com.pawbites.petshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pawbites.petshop.entity.OrderEntity;

import com.pawbites.petshop.result.PageResult;
import com.pawbites.petshop.vo.order.OrderVO;

/**
 * 订单服务
 * <p>面向登录用户与游客两类身份：从购物车勾选项创建订单、分页查询订单列表/查询订单详情/取消订单，均按 userId/guestId 归属过滤。</p>
 */
public interface OrderService extends IService<OrderEntity> {

  /**
   * 从购物车勾选项创建订单
   * <p>原子扣减库存防止超卖，订单条目保存商品价格快照，下单成功后清空已勾选的购物车条目。</p>
   *
   * @param userId   登录用户 id（游客身份为 null）
   * @param guestId  游客标识（登录身份为 null）
   * @return 订单视图（含条目明细）
   * @throws BusinessException 购物车为空（无勾选项）、部分商品已下架或库存不足
   */
  OrderVO createFromCart(Long userId, String guestId);

  /**
   * 分页查询当前身份的订单列表
   * <p>按 userId/guestId 归属过滤，按 id 倒序分页返回；页码从 1 起，每页条数上限 100，越界自动收敛。</p>
   *
   * @param userId   登录用户 id（游客身份为 null）
   * @param guestId  游客标识（登录身份为 null）
   * @param page     页码（从 1 开始，小于 1 时按 1 处理）
   * @param size     每页条数（1~100，越界时自动收敛）
   * @return 订单分页结果（当前页数据列表与总记录数、分页信息）
   * @throws BusinessException 既无 userId 也无 guestId（401）
   */
  PageResult<OrderVO> listOrders(Long userId, String guestId, long page, long size);

  /**
   * 查询订单详情
   * <p>仅允许查询归属当前身份的订单，否则视为不存在。</p>
   *
   * @param userId   登录用户 id（游客身份为 null）
   * @param guestId  游客标识（登录身份为 null）
   * @param orderId  订单 id
   * @return 订单视图（含条目明细）
   * @throws BusinessException 订单不存在或不归属当前身份（404）
   */
  OrderVO getOrder(Long userId, String guestId, Long orderId);

  /**
   * 取消订单
   * <p>仅已创建（CREATED）状态的订单可取消，取消后按订单条目恢复对应商品库存。</p>
   *
   * @param userId   登录用户 id（游客身份为 null）
   * @param guestId  游客标识（登录身份为 null）
   * @param orderId  订单 id
   * @return 取消后的订单视图
   * @throws BusinessException 订单不存在/不归属当前身份（404）或当前状态不可取消
   */
  OrderVO cancelOrder(Long userId, String guestId, Long orderId);
}
