package com.pawbites.petshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pawbites.petshop.entity.Bundle;

import com.pawbites.petshop.vo.bundle.BundleVO;
import java.util.List;

/**
 * 套餐服务
 * <p>提供有效套餐列表查询，并汇总各套餐内含的商品明细。</p>
 */
public interface BundleService extends IService<Bundle> {

  /**
   * 查询全部有效套餐
   * <p>按排序值与 id 升序返回，每个套餐附带内含商品的汇总信息（名称、价格、图片、数量）。</p>
   *
   * @return 套餐视图列表
   */
  List<BundleVO> listActiveBundles();
}
