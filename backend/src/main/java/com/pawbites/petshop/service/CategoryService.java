package com.pawbites.petshop.service;

import com.pawbites.petshop.vo.category.CategoryVO;
import java.util.List;

public interface CategoryService {

  List<CategoryVO> listActiveCategories();
}
