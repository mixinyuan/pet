package com.pawbites.petshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pawbites.petshop.entity.Bundle;

import com.pawbites.petshop.vo.bundle.BundleVO;
import java.util.List;

public interface BundleService extends IService<Bundle> {

  List<BundleVO> listActiveBundles();
}
