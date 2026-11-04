package com.pawbites.petshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pawbites.petshop.entity.OrderEntity;

import com.pawbites.petshop.vo.order.OrderVO;
import java.util.List;

public interface OrderService extends IService<OrderEntity> {

  OrderVO createFromCart(Long userId, String guestId);

  List<OrderVO> listOrders(Long userId, String guestId);

  OrderVO getOrder(Long userId, String guestId, Long orderId);

  OrderVO cancelOrder(Long userId, String guestId, Long orderId);
}
