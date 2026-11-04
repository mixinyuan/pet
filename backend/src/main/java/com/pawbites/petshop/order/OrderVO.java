package com.pawbites.petshop.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderVO(
    Long id,
    String orderNo,
    BigDecimal totalAmount,
    String status,
    LocalDateTime createdAt,
    List<OrderItemVO> items
) {
}
