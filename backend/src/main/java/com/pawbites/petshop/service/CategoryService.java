package com.pawbites.petshop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pawbites.petshop.entity.Category;

import com.pawbites.petshop.vo.category.CategoryVO;
import java.util.List;

public interface CategoryService extends IService<Category> {

  List<CategoryVO> listActiveCategories();
}
