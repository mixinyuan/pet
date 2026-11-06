package com.pawbites.petshop.service.impl.admin;
import jakarta.annotation.Resource;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pawbites.petshop.service.admin.AdminCouponTemplateService;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.pawbites.petshop.constant.StatusConstants;
import com.pawbites.petshop.dto.admin.CouponTemplateSaveRequest;
import com.pawbites.petshop.entity.Coupon;
import com.pawbites.petshop.entity.UserCoupon;
import com.pawbites.petshop.exception.BusinessException;
import com.pawbites.petshop.mapper.CouponMapper;
import com.pawbites.petshop.mapper.UserCouponMapper;
import com.pawbites.petshop.vo.admin.AdminCouponTemplateVO;

/**
 * 管理端优惠券模板服务实现
 * <p>listTemplates 全量返回（模板量小不分页，id 倒序），issuedCount/usedCount 批量装配：
 * 一次 in 查询该批模板的全部持券记录后内存分组计数（沿用管理端用户列表的批量统计套路）。
 * create/update 前置名称唯一校验（编辑排除自身，重复 409）；update 只 SET 表单覆盖的五列。
 * 重要事实：user_coupon 未快照金额/门槛/名称，持券展示（listCoupons）与核销校验
 * （validateForOrder）均随模板实时读取，因此编辑模板会影响存量持券实例——本期保持
 * 该行为不变（不引入快照列），后台编辑时由前端提示「编辑将同步作用于存量持券」。
 * delete 走物理删除（coupon 表无逻辑删除列），已发放数 &gt; 0 时拒绝（防审计断链，
 * 亦避免 user_coupon 外键悬空）；updateStatus 只更新状态列，停用后自动发券处
 * 按 status=ACTIVE 过滤即不再发放（见 CouponServiceImpl）。</p>
 */
@Service
public class AdminCouponTemplateServiceImpl extends ServiceImpl<CouponMapper, Coupon>
    implements AdminCouponTemplateService {

  /** 优惠类型固定取值（本期仅固定满减券） */
  private static final String DISCOUNT_TYPE_FIXED_AMOUNT = "FIXED_AMOUNT";

  /** 用户持券 Mapper（已发放/已使用统计与删除前的引用检查） */
  @Resource
  private UserCouponMapper userCouponMapper;

  @Override
  public List<AdminCouponTemplateVO> listTemplates() {
    // 模板量小，全量返回不分页；id 倒序（最新模板在前），含停用模板（后台要能看到）
    List<Coupon> templates = baseMapper.selectList(Wrappers.<Coupon>lambdaQuery()
        .orderByDesc(Coupon::getId));
    if (templates.isEmpty()) {
      return List.of();
    }
    return assembleVOs(templates);
  }

  @Override
  public AdminCouponTemplateVO getTemplate(Long id) {
    return assembleVOs(List.of(requireTemplate(id))).getFirst();
  }

  @Transactional
  @Override
  public AdminCouponTemplateVO createTemplate(CouponTemplateSaveRequest request) {
    Coupon template = new Coupon();
    applyEditableFields(template, request);
    ensureNameUnique(template.getName(), null);
    // create_time 由数据库 DEFAULT CURRENT_TIMESTAMP 维护；discount_type 固定 FIXED_AMOUNT，status 缺省 ACTIVE（列默认值）
    baseMapper.insert(template);
    return getTemplate(template.getId());
  }

  @Transactional
  @Override
  public AdminCouponTemplateVO updateTemplate(Long id, CouponTemplateSaveRequest request) {
    requireTemplate(id);
    Coupon template = new Coupon();
    applyEditableFields(template, request);
    ensureNameUnique(template.getName(), id);

    // 只 SET 表单覆盖的五列（沿用日志/商品管理更新写法），create_time 不触碰。
    // 注意：user_coupon 未快照金额/门槛/名称，此更新会同步作用于存量持券实例的展示与核销
    baseMapper.update(null, Wrappers.<Coupon>lambdaUpdate()
        .eq(Coupon::getId, id)
        .set(Coupon::getName, template.getName())
        .set(Coupon::getAmount, template.getAmount())
        .set(Coupon::getMinSpend, template.getMinSpend())
        .set(Coupon::getValidDays, template.getValidDays())
        .set(Coupon::getRemark, template.getRemark()));
    return getTemplate(id);
  }

  @Transactional
  @Override
  public void updateStatus(Long id, String status) {
    requireTemplate(id);
    // 只更新状态列；coupon 表无 update_time 列，无需额外维护。
    // 停用后自动发券按 status=ACTIVE 过滤模板，存量持券照常展示与核销
    baseMapper.update(null, Wrappers.<Coupon>lambdaUpdate()
        .eq(Coupon::getId, id)
        .set(Coupon::getStatus, status));
  }

  @Transactional
  @Override
  public void deleteTemplate(Long id) {
    requireTemplate(id);
    // 已发放引用检查：存在持券记录即拒绝物理删除（外键约束亦会拦截），提示仅支持停用
    Long issuedCount = userCouponMapper.selectCount(Wrappers.<UserCoupon>lambdaQuery()
        .eq(UserCoupon::getCouponId, id));
    if (issuedCount != null && issuedCount > 0) {
      throw new BusinessException(400,
          "该券已发放 " + issuedCount + " 张，不能删除，仅支持停用");
    }
    // coupon 表无逻辑删除列，deleteById 即物理删除
    baseMapper.deleteById(id);
  }

  /** 按 id 查询模板（含停用模板），不存在时抛 404 */
  private Coupon requireTemplate(Long id) {
    Coupon template = baseMapper.selectById(id);
    if (template == null) {
      throw new BusinessException(404, "优惠券模板不存在");
    }
    return template;
  }

  /** 名称唯一性预校验（excludeId 用于编辑时排除自身），重复抛 409 */
  private void ensureNameUnique(String name, Long excludeId) {
    Long count = baseMapper.selectCount(Wrappers.<Coupon>lambdaQuery()
        .eq(Coupon::getName, name)
        .ne(excludeId != null, Coupon::getId, excludeId));
    if (count != null && count > 0) {
      throw new BusinessException(409, "券名称已存在：" + name);
    }
  }

  /** 应用表单可编辑字段（去空白；discount_type 固定 FIXED_AMOUNT，status 不在编辑表单内） */
  private void applyEditableFields(Coupon template, CouponTemplateSaveRequest request) {
    template.setName(request.name().trim());
    template.setDiscountType(DISCOUNT_TYPE_FIXED_AMOUNT);
    template.setAmount(request.amount());
    template.setMinSpend(request.minSpend());
    template.setValidDays(request.validDays());
    template.setRemark(StringUtils.hasText(request.remark()) ? request.remark().trim() : null);
  }

  /** 批量装配管理端模板视图：一次 in 查询当批模板全部持券记录后内存分组统计已发放/已使用 */
  private List<AdminCouponTemplateVO> assembleVOs(List<Coupon> templates) {
    if (templates == null || templates.isEmpty()) {
      return List.of();
    }
    List<Long> templateIds = templates.stream().map(Coupon::getId).toList();
    // issuedCount：该批模板的全部持券记录（含已使用/已过期未核销）按模板分组计数
    Map<Long, Long> issuedMap = userCouponMapper.selectList(Wrappers.<UserCoupon>lambdaQuery()
            .in(UserCoupon::getCouponId, templateIds))
        .stream()
        .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));
    // usedCount：其中 status=1 已核销的记录按模板分组计数（已过期为展示态不落库，天然计入未使用）
    Map<Long, Long> usedMap = userCouponMapper.selectList(Wrappers.<UserCoupon>lambdaQuery()
            .in(UserCoupon::getCouponId, templateIds)
            .eq(UserCoupon::getStatus, UserCoupon.STATUS_USED))
        .stream()
        .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));
    return templates.stream()
        .map(template -> toVO(template,
            issuedMap.getOrDefault(template.getId(), 0L),
            usedMap.getOrDefault(template.getId(), 0L)))
        .toList();
  }

  /** 模板实体 + 统计数值 → 管理端模板视图 */
  private AdminCouponTemplateVO toVO(Coupon template, Long issuedCount, Long usedCount) {
    return new AdminCouponTemplateVO(
        template.getId(),
        template.getName(),
        template.getDiscountType(),
        template.getAmount(),
        template.getMinSpend(),
        template.getValidDays(),
        template.getRemark(),
        template.getStatus(),
        template.getCreateTime(),
        issuedCount,
        usedCount
    );
  }
}
