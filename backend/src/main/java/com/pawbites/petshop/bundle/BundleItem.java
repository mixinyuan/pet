package com.pawbites.petshop.bundle;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pawbites.petshop.common.config.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("bundle_item")
@EqualsAndHashCode(callSuper = true)
public class BundleItem extends BaseEntity {

  private Long bundleId;
  private Long productId;
  private Integer quantity;
}
