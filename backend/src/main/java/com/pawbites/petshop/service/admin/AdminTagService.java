package com.pawbites.petshop.service.admin;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pawbites.petshop.dto.admin.TagCreateRequest;
import com.pawbites.petshop.dto.admin.TagUpdateRequest;
import com.pawbites.petshop.entity.ProductTag;
import com.pawbites.petshop.vo.admin.AdminTagVO;
import java.util.List;

/**
 * 管理端商品标签服务
 * <p>提供后台标签维护：全量列表（带引用商品数）、新增/改名（名称全局唯一）、启用/停用
 * 与删除（被商品引用时拒绝）。C 端商品 VO 的标签展示逻辑不受影响。</p>
 */
public interface AdminTagService extends IService<ProductTag> {

  /**
   * 查询全部标签（含停用）
   * <p>按 id 升序返回，每项带引用商品数（仅统计未删除商品）。</p>
   *
   * @return 管理端标签视图列表
   */
  List<AdminTagVO> listTags();

  /**
   * 新增标签
   *
   * @param request 标签表单（name 必填全局唯一；新建默认启用）
   * @return 新增后的标签视图
   * @throws com.pawbites.petshop.exception.BusinessException 名称重复（409）时抛出
   */
  AdminTagVO createTag(TagCreateRequest request);

  /**
   * 标签改名
   * <p>仅改名称（需保持全局唯一），状态切换走 /status 接口。</p>
   *
   * @param id      标签 id
   * @param request 标签编辑表单（name 必填）
   * @return 编辑后的标签视图
   * @throws com.pawbites.petshop.exception.BusinessException 标签不存在（404）、名称重复（409）时抛出
   */
  AdminTagVO updateTag(Long id, TagUpdateRequest request);

  /**
   * 切换标签状态
   *
   * @param id     标签 id
   * @param status 目标状态（ACTIVE/INACTIVE，与 product_tag.status 取值一致）
   * @throws com.pawbites.petshop.exception.BusinessException 标签不存在（404）时抛出
   */
  void updateStatus(Long id, String status);

  /**
   * 删除标签（逻辑删除）
   * <p>被商品引用时拒绝删除（报错文案含引用数）。</p>
   *
   * @param id 标签 id
   * @throws com.pawbites.petshop.exception.BusinessException 标签不存在（404）、被商品引用（400）时抛出
   */
  void deleteTag(Long id);
}
