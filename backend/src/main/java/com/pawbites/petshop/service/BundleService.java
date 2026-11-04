package com.pawbites.petshop.service;

import com.pawbites.petshop.vo.bundle.BundleVO;
import java.util.List;

public interface BundleService {

  List<BundleVO> listActiveBundles();
}
