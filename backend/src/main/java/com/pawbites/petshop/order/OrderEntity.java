package com.pawbites.petshop.order;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pawbites.petshop.common.config.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("orders")
@EqualsAndHashCode(callSuper = true)
public class OrderEntity extends BaseEntity {

  private String orderNo;
  private Long userId;
  private String guestId;
  private BigDecimal totalAmount;
  private String status;
}
