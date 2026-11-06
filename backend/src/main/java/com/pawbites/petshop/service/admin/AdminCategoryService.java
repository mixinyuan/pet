package com.pawbites.petshop.service.admin;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pawbites.petshop.dto.admin.CategoryCreateRequest;
import com.pawbites.petshop.dto.admin.CategoryUpdateRequest;
import com.pawbites.petshop.entity.Category;
import com.pawbites.petshop.vo.admin.AdminCategoryVO;
import java.util.List;

/**
 * 管理端分类服务
 * <p>提供后台分类维护：全量列表（含停用，带引用商品数）、新增（编码唯一、不允许 all）、
 * 编辑（编码不可改）、启用/停用与删除（被商品引用或系统内置 all 时拒绝）。</p>
 */
public interface AdminCategoryService extends IService<Category> {

  /**
   * 查询全部分类（含停用）
   * <p>按 sort_order、id 升序返回，每项带引用商品数（仅统计未删除商品）。</p>
   *
   * @return 管理端分类视图列表
   */
  List<AdminCategoryVO> listCategories();

  /**
   * 查询单个分类（后台视角，停用分类也可查看）
   *
   * @param id 分类 id
   * @return 管理端分类视图
   * @throws com.pawbites.petshop.exception.BusinessException 分类不存在（404）时抛出
   */
  AdminCategoryVO getCategory(Long id);

  /**
   * 新增分类
   *
   * @param request 分类表单（code 必填小写字母唯一、不允许 all；name 必填；sortOrder 缺省 0）
   * @return 新增后的分类视图
   * @throws com.pawbites.petshop.exception.BusinessException 编码为 all（400）、编码重复（409）时抛出
   */
  AdminCategoryVO createCategory(CategoryCreateRequest request);

  /**
   * 编辑分类
   * <p>仅可改 name/description/sortOrder/status；编码不允许修改（传入且与现值不同时报错）。</p>
   *
   * @param id      分类 id
   * @param request 分类编辑表单（字段可选，null 表示不变）
   * @return 编辑后的分类视图
   * @throws com.pawbites.petshop.exception.BusinessException 分类不存在（404）、编码被修改（400）、
   *                                                          名称空白（400）时抛出
   */
  AdminCategoryVO updateCategory(Long id, CategoryUpdateRequest request);

  /**
   * 切换分类状态
   *
   * @param id     分类 id
   * @param status 目标状态（ACTIVE/INACTIVE）
   * @throws com.pawbites.petshop.exception.BusinessException 分类不存在（404）时抛出
   */
  void updateStatus(Long id, String status);

  /**
   * 删除分类（逻辑删除）
   * <p>系统内置 all 或被商品引用时拒绝删除（引用时报错文案含引用数）。</p>
   *
   * @param id 分类 id
   * @throws com.pawbites.petshop.exception.BusinessException 分类不存在（404）、系统内置（400）、
   *                                                          被商品引用（400）时抛出
   */
  void deleteCategory(Long id);
}
