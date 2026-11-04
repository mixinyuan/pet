package com.pawbites.petshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pawbites.petshop.entity.Cart;

import com.pawbites.petshop.dto.cart.AddCartItemRequest;
import com.pawbites.petshop.dto.cart.UpdateCartItemRequest;
import com.pawbites.petshop.vo.cart.CartVO;

/**
 * 购物车服务
 * <p>管理登录用户与游客两类身份的购物车：定位/创建购物车、加购、改量、删除与清空。</p>
 */
public interface CartService extends IService<Cart> {

  /**
   * 查询购物车视图
   * <p>购物车不存在时按当前身份自动创建空购物车。</p>
   *
   * @param userId   登录用户 id（游客身份为 null）
   * @param guestId  游客标识（登录身份为 null）
   * @return 购物车视图（条目列表、总件数、总金额）
   */
  CartVO getCart(Long userId, String guestId);

  /**
   * 将商品加入购物车
   * <p>商品不存在、已下架或库存不足时抛出业务异常。</p>
   *
   * @param userId   登录用户 id（游客身份为 null）
   * @param guestId  游客标识（登录身份为 null）
   * @param request  加购请求（商品 id 与数量）
   * @return 最新的购物车视图
   * @throws BusinessException 商品不存在/已下架（404）、数量非法或库存不足
   */
  CartVO addItem(Long userId, String guestId, AddCartItemRequest request);

  /**
   * 修改购物车中指定商品的数量
   * <p>购物车为空、商品不在购物车中、商品已下架或库存不足时抛出业务异常。</p>
   *
   * @param userId    登录用户 id（游客身份为 null）
   * @param guestId   游客标识（登录身份为 null）
   * @param productId 商品 id
   * @param request   改量请求（目标数量）
   * @return 最新的购物车视图
   * @throws BusinessException 购物车为空、无该商品、商品不存在/已下架、数量非法或库存不足
   */
  CartVO updateItem(Long userId, String guestId, Long productId, UpdateCartItemRequest request);

  /**
   * 从购物车移除指定商品
   * <p>购物车或对应条目不存在时静默忽略。</p>
   *
   * @param userId    登录用户 id（游客身份为 null）
   * @param guestId   游客标识（登录身份为 null）
   * @param productId 商品 id
   * @return 最新的购物车视图
   */
  CartVO removeItem(Long userId, String guestId, Long productId);

  /**
   * 清空购物车全部条目
   * <p>购物车不存在时静默忽略。</p>
   *
   * @param userId   登录用户 id（游客身份为 null）
   * @param guestId  游客标识（登录身份为 null）
   */
  void clearCart(Long userId, String guestId);

  /**
   * 按身份定位购物车（供订单模块跨模块调用）
   * <p>优先按 userId 定位，其次按 guestId；两者均缺失时抛出业务异常。</p>
   *
   * @param userId          登录用户 id（游客身份为 null）
   * @param guestId         游客标识（登录身份为 null）
   * @param createIfMissing true 表示购物车不存在时自动创建
   * @return 购物车实体；createIfMissing 为 false 且购物车不存在时返回 null
   * @throws BusinessException 既无 userId 也无 guestId（401）
   */
  Cart resolveCart(Long userId, String guestId, boolean createIfMissing);
}
