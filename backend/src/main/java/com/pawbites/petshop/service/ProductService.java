package com.pawbites.petshop.service;

import com.pawbites.petshop.entity.Product;
import com.pawbites.petshop.result.PageResult;
import com.pawbites.petshop.vo.product.ProductVO;
import java.util.List;

public interface ProductService {

  PageResult<ProductVO> listProducts(long page, long size, String categoryCode, String keyword);

  ProductVO getProduct(Long id);

  List<ProductVO> enrichProducts(List<Product> products);
}
