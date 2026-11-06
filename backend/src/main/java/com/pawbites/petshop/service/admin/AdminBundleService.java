package com.pawbites.petshop.service.admin;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pawbites.petshop.dto.admin.BundleSaveRequest;
import com.pawbites.petshop.entity.Bundle;
import com.pawbites.petshop.result.PageResult;
import com.pawbites.petshop.vo.admin.AdminBundleVO;

/**
 * 管理端套餐服务
 * <p>提供后台场景套餐维护：不过滤上架状态的分页查询（含下架套餐）、详情（含明细子表与汇总字段）、
 * 新增/编辑（事务内写 bundle 主表 + bundle_item 明细，编辑时明细整体替换）、上下架切换与删除
 * （主表逻辑删除，明细行保留，查询侧过滤）。</p>
 */
public interface AdminBundleService extends IService<Bundle> {

  /**
   * 分页查询套餐（后台视角，不过滤上架状态）
   * <p>关键字匹配套餐名称（like，通配符转义），结果按 sort_order、id 升序；
   * records 每条携带明细子表与 itemCount/totalQuantity 汇总字段。</p>
   *
   * @param page    页码（从 1 开始，小于 1 时按 1 处理，大于 10000 时按 10000 处理）
   * @param size    每页数量（clamp 到 [1, 100]）
   * @param keyword 套餐名关键字（null 或空白表示不过滤）
   * @param status  上架状态筛选（ACTIVE/INACTIVE，null 或空白表示不过滤）
   * @return 管理端套餐视图分页结果（含明细与汇总字段）
   * @throws com.pawbites.petshop.exception.BusinessException status 取值非法（400）时抛出
   */
  PageResult<AdminBundleVO> pageBundles(long page, long size, String keyword, String status);

  /**
   * 查询套餐详情（后台视角，下架套餐也可查看）
   *
   * @param id 套餐 id
   * @return 管理端套餐视图（与列表单条同构）
   * @throws com.pawbites.petshop.exception.BusinessException 套餐不存在（404）时抛出
   */
  AdminBundleVO getBundle(Long id);

  /**
   * 新增套餐
   * <p>事务内写 bundle 主表与 bundle_item 明细；要求原价 ≥ 现价、明细 ≥ 1 行、
   * 同一商品不可重复、引用商品必须存在且未逻辑删除（不要求上架）。</p>
   *
   * @param request 套餐表单（字段校验见 {@link BundleSaveRequest}）
   * @return 新增后的套餐视图
   * @throws com.pawbites.petshop.exception.BusinessException 价格规则不符/明细无效（400）时抛出
   */
  AdminBundleVO createBundle(BundleSaveRequest request);

  /**
   * 编辑套餐
   * <p>整体替换语义：主表字段全量更新，bundle_item 明细先物理删除旧行再重插（事务内）。</p>
   *
   * @param id      套餐 id
   * @param request 套餐表单（与新增同一份校验）
   * @return 编辑后的套餐视图
   * @throws com.pawbites.petshop.exception.BusinessException 套餐不存在（404）、
   *                                                          价格规则不符/明细无效（400）时抛出
   */
  AdminBundleVO updateBundle(Long id, BundleSaveRequest request);

  /**
   * 切换套餐上架状态
   *
   * @param id     套餐 id
   * @param status 目标状态（ACTIVE/INACTIVE）
   * @throws com.pawbites.petshop.exception.BusinessException 套餐不存在（404）时抛出
   */
  void updateStatus(Long id, String status);

  /**
   * 删除套餐（主表逻辑删除）
   * <p>bundle 与订单无关联（C 端套餐仅展示不可购），删除不影响历史数据；
   * bundle_item 明细行保留不动，查询侧按 bundle.deleted=0 自然过滤。</p>
   *
   * @param id 套餐 id
   * @throws com.pawbites.petshop.exception.BusinessException 套餐不存在（404）时抛出
   */
  void deleteBundle(Long id);
}
