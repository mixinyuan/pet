/**
 * 管理台类型定义：与 backend vo/admin、dto/admin 逐一对齐。
 * 本文件按模块追加（当前含商品/分类/标签/套餐/订单/用户/评价/券模板/日志），
 * 其余管理台模块迁移时继续补充，只追加不删除，避免影响并行迁移的其他模块。
 */

/** 通用启用/上架状态（与后端 status 列对齐：ACTIVE 启用·上架 / INACTIVE 停用·下架） */
export type AdminStatus = 'ACTIVE' | 'INACTIVE'

/** 管理端商品视图（AdminProductVO，列表/详情共用；categories 为分类 code，tags 为标签 id） */
export interface AdminProductVO {
  id: number
  sku: string
  name: string
  /** 适用宠物（猫咪适用/狗狗适用/猫狗通用/小型犬适用） */
  petType: string
  flavor: string
  /** 售价（元） */
  price: number
  /** 评分（0.0-5.0，人工维护，不与 C 端评价联动） */
  rating: number | null
  stock: number
  status: AdminStatus
  imageUrl: string
  description: string | null
  /** 所属分类 code 列表（不含已删除分类） */
  categories: string[]
  /** 关联标签 id 列表（不含已删除标签） */
  tags: number[]
}

/** 管理端分类视图（AdminCategoryVO，含停用分类，带引用商品数） */
export interface AdminCategoryVO {
  id: number
  /** 分类编码（全局唯一，创建后不可修改；all 为系统内置） */
  code: string
  name: string
  description: string | null
  /** 排序号，数值越小越靠前 */
  sortOrder: number
  status: AdminStatus
  /** 引用该分类的商品数（仅统计未删除商品） */
  productCount: number
}

/** 管理端标签视图（AdminTagVO，含停用标签，带引用商品数） */
export interface AdminTagVO {
  id: number
  /** 标签名（全局唯一） */
  name: string
  status: AdminStatus
  /** 引用该标签的商品数（仅统计未删除商品） */
  productCount: number
}

/** 商品列表查询参数（对齐 AdminProductController#list：keyword 最长 64 字符，category 传分类 code） */
export interface AdminProductQuery {
  page: number
  size: number
  /** 名称/SKU/口味关键字模糊搜索，可选 */
  keyword?: string
  /** 分类 code，可选（不传表示全部） */
  category?: string
  /** 上架状态筛选，可选（后台默认不过滤，含下架商品） */
  status?: AdminStatus
}

/** 商品新增/编辑共用表单（ProductSaveRequest，编辑为整体替换语义；SKU 大写转换由前端负责） */
export interface ProductSavePayload {
  sku: string
  name: string
  petType: string
  flavor: string
  /** 售价，> 0，最多 8 位整数、2 位小数 */
  price: number
  /** 库存，≥ 0 的整数 */
  stock: number
  /** 评分，0.0-5.0 一位小数；缺省 5.0 */
  rating?: number | null
  /** 图片 URL，需以 http:// 或 https:// 开头 */
  imageUrl: string
  /** 所属分类 code 列表，至少 1 个，不允许包含内置 all */
  categories: string[]
  /** 标签 id 列表，可选 */
  tags?: number[]
  /** 商品描述，最长 500 字符 */
  description?: string | null
  /** 上架状态，缺省 ACTIVE */
  status?: AdminStatus
}

/** 分类新增表单（CategoryCreateRequest；code 需匹配 ^[a-z][a-z0-9-]{0,39}$ 且不可为 all） */
export interface CategoryCreatePayload {
  code: string
  name: string
  description?: string | null
  sortOrder?: number | null
}

/** 分类编辑表单（CategoryUpdateRequest；code 不可改，不传即视为不变） */
export interface CategoryUpdatePayload {
  name: string
  description?: string | null
  sortOrder?: number | null
}

/** 标签新增/改名表单（TagCreateRequest / TagUpdateRequest，仅 name 一个字段） */
export interface TagSavePayload {
  name: string
}

/** 状态切换请求体（StatusUpdateRequest，商品/分类/标签共用） */
export interface StatusUpdatePayload {
  status: AdminStatus
}

/* ==================== 场景套餐（AdminBundleController） ==================== */

/** 管理端套餐明细商品视图（AdminBundleItemVO：商品摘要 + 数量） */
export interface AdminBundleItemVO {
  productId: number
  productName: string
  /** 商品单价（当前售价） */
  price: number
  imageUrl: string
  /** 该商品在套餐中的数量 */
  quantity: number
}

/** 管理端套餐视图（AdminBundleVO，列表/详情共用；含明细子表与汇总字段） */
export interface AdminBundleVO {
  id: number
  name: string
  description: string | null
  /** 套餐现价（元） */
  price: number
  /** 原价（划线展示用） */
  originalPrice: number
  /** 折扣标签文案（如「省 18%」，可为空） */
  discountLabel: string | null
  /** 排序序号，数值越小越靠前 */
  sortOrder: number
  status: AdminStatus
  /** 明细子表（商品摘要 + 数量，按录入顺序；已过滤无效行） */
  items: AdminBundleItemVO[]
  /** 汇总：明细行数（商品种数） */
  itemCount: number
  /** 汇总：总件数（各明细数量之和） */
  totalQuantity: number
}

/** 套餐列表查询参数（对齐 AdminBundleController#list：keyword 最长 64 字符） */
export interface AdminBundleQuery {
  page: number
  size: number
  /** 套餐名关键字模糊搜索，可选 */
  keyword?: string
  /** 上架状态筛选，可选（后台默认不过滤，含下架套餐） */
  status?: AdminStatus
}

/** 套餐明细行表单（BundleItemRequest：商品 id + 数量 1-99，同一商品不可重复由服务层校验） */
export interface BundleItemPayload {
  productId: number
  quantity: number
}

/** 套餐新增/编辑共用表单（BundleSaveRequest，编辑为整体替换语义；原价 ≥ 现价由服务层校验） */
export interface BundleSavePayload {
  name: string
  /** 现价，> 0，最多 8 位整数、2 位小数 */
  price: number
  /** 原价，> 0，最多 8 位整数、2 位小数；须 ≥ 现价 */
  originalPrice: number
  /** 折扣文案，最长 20 字符，留空则 C 端不显示角标 */
  discountLabel?: string | null
  /** 套餐描述，最长 200 字符 */
  description?: string | null
  /** 排序序号，缺省 0，数值越小越靠前 */
  sortOrder?: number | null
  /** 商品明细，至少 1 行 */
  items: BundleItemPayload[]
  /** 上架状态，缺省 ACTIVE */
  status?: AdminStatus
}

/* ==================== 订单管理（AdminOrderController） ==================== */

/** 订单状态（CREATED-已创建 / CANCELLED-已取消） */
export type AdminOrderStatus = 'CREATED' | 'CANCELLED'

/** 取消方（USER-用户取消 / ADMIN-管理员取消；未取消为 null） */
export type AdminOrderCancelBy = 'USER' | 'ADMIN'

/** 管理端订单列表行（AdminOrderVO：含下单人展示名、件数、用券与取消审计列） */
export interface AdminOrderVO {
  id: number
  orderNo: string
  /** 是否游客单（userId 为空即游客下单） */
  isGuest: boolean
  /** 下单用户 ID（游客单为 null） */
  userId: number | null
  /** 游客标识（登录用户下单为 null） */
  guestId: string | null
  /** 下单人展示名：用户单取昵称，游客单为「游客 + guestId 前 6 位」 */
  ownerName: string
  /** 商品件数（各条目数量之和） */
  itemCount: number
  /** 订单实付总额（商品合计 - 券抵扣，元） */
  totalAmount: number
  /** 使用的券名称（未用券或券已随取消返还时为 null） */
  couponName: string | null
  /** 券抵扣金额（元；未用券或券已随取消返还时为 null） */
  couponAmount: number | null
  status: AdminOrderStatus
  createdAt: string
  /** 取消时间（未取消为 null） */
  cancelTime: string | null
  cancelBy: AdminOrderCancelBy | null
}

/** 管理端订单条目视图（AdminOrderItemVO：下单时快照，不随商品改价改名变化） */
export interface AdminOrderItemVO {
  productName: string
  productImage: string
  price: number
  quantity: number
  subtotal: number
}

/** 管理端订单下单用户信息视图（AdminOrderUserVO；游客单 user 整体为 null） */
export interface AdminOrderUserVO {
  username: string
  nickname: string
  email: string
}

/** 管理端订单详情视图（AdminOrderDetailVO：列表行字段 + 商品快照 + 用户信息） */
export interface AdminOrderDetailVO extends AdminOrderVO {
  /** 商品快照列表（按条目 id 升序） */
  items: AdminOrderItemVO[]
  /** 商品合计（各条目小计之和；实付总额 = 商品合计 - 券抵扣） */
  itemsSubtotal: number
  /** 下单用户信息（游客单为 null） */
  user: AdminOrderUserVO | null
}

/** 订单列表查询参数（对齐 AdminOrderController#list：orderNo 精确 / keyword 模糊，日期 yyyy-MM-dd 闭区间） */
export interface AdminOrderQuery {
  page: number
  size: number
  /** 订单号精确查询，最长 40 字符 */
  orderNo?: string
  /** 下单人关键字模糊搜索（用户名/昵称，游客单按 guestId 前缀匹配），最长 64 字符 */
  keyword?: string
  status?: AdminOrderStatus
  /** 下单日期起（含当天，yyyy-MM-dd） */
  from?: string
  /** 下单日期止（含当天，yyyy-MM-dd） */
  to?: string
}

/* ==================== 用户管理（AdminUserController） ==================== */

/** 管理端用户列表行（AdminUserVO：含订单/消费/持券汇总） */
export interface AdminUserVO {
  id: number
  username: string
  nickname: string | null
  email: string | null
  /** 账号状态（ACTIVE 正常 / INACTIVE 已禁用；禁用后该用户 token 即时失效） */
  status: AdminStatus
  createdAt: string
  /** 有效订单数（仅状态为 CREATED 的订单） */
  orderCount: number
  /** 有效订单实付总额（仅 CREATED 订单合计，元） */
  totalSpend: number
  /** 持券总数 */
  couponTotal: number
  /** 可用券数（未使用且未过期） */
  couponAvailable: number
}

/** 管理端用户详情订单简表（AdminUserOrderVO：该用户全部订单，含已取消） */
export interface AdminUserOrderVO {
  orderNo: string
  totalAmount: number
  status: AdminOrderStatus
  createdAt: string
}

/** 管理端用户详情收藏商品（AdminUserFavoriteProductVO，最近收藏最多 5 个） */
export interface AdminUserFavoriteProductVO {
  productId: number
  name: string
  imageUrl: string
  price: number
}

/** 管理端用户详情收藏汇总（AdminUserFavoriteVO：总数 + 最近 5 个商品） */
export interface AdminUserFavoriteVO {
  total: number
  products: AdminUserFavoriteProductVO[]
}

/** 管理端用户详情持券（AdminUserCouponVO：status 为懒判定展示态） */
export interface AdminUserCouponVO {
  name: string
  /** 持券展示状态（0-未使用 1-已使用 2-已过期〔懒判定〕） */
  status: number
  expiresAt: string
}

/** 管理端用户详情订阅状态（AdminUserSubscriptionVO：邮箱存在 ACTIVE 订阅记录即已订阅） */
export interface AdminUserSubscriptionVO {
  subscribed: boolean
}

/** 管理端用户详情聚合视图（AdminUserDetailVO：基本资料 + 订单/收藏/持券/订阅） */
export interface AdminUserDetailVO {
  id: number
  username: string
  nickname: string | null
  email: string | null
  status: AdminStatus
  createdAt: string
  /** 角色（USER-普通用户 / ADMIN-管理员；管理员账号不允许禁用） */
  role: 'USER' | 'ADMIN'
  orders: AdminUserOrderVO[]
  favorites: AdminUserFavoriteVO
  coupons: AdminUserCouponVO[]
  subscription: AdminUserSubscriptionVO
}

/** 用户列表查询参数（对齐 AdminUserController#list：keyword 匹配用户名/昵称/邮箱） */
export interface AdminUserQuery {
  page: number
  size: number
  /** 用户名/昵称/邮箱关键字模糊搜索，最长 64 字符 */
  keyword?: string
  /** 账号状态筛选（ACTIVE/INACTIVE） */
  status?: AdminStatus
}

/* ==================== 评价管理（AdminReviewController） ==================== */

/** 管理端评价视图（AdminReviewVO：后台不过滤展示状态，列表/详情/表单回显共用） */
export interface AdminReviewVO {
  id: number
  /** 关联商品 ID（null 表示通用评价） */
  productId: number | null
  buyerName: string
  petName: string | null
  petBreed: string | null
  content: string
  avatarUrl: string | null
  /** 所购商品名称（冗余展示列，随关联商品名同步维护） */
  purchasedProduct: string | null
  /** 评分（0.0-5.0 一位小数，人工维护，不与 C 端联动） */
  rating: number | null
  /** 排序号（越小越靠前，与 C 端展示序一致） */
  sortOrder: number | null
  /** 展示状态（ACTIVE 展示 / INACTIVE 隐藏，隐藏后 C 端立即不展示） */
  status: AdminStatus
  createdAt: string
  updatedAt: string
  /** 关联商品名（联查 product.name；未关联或商品已删除为 null，前端展示「通用」） */
  productName: string | null
}

/** 评价列表查询参数（对齐 AdminReviewController#list：keyword 匹配内容/评价人，最长 64 字符） */
export interface AdminReviewQuery {
  page: number
  size: number
  /** 内容/评价人关键字模糊搜索，可选 */
  keyword?: string
  /** 展示状态筛选（ACTIVE/INACTIVE），可选（不传表示全部） */
  status?: AdminStatus
  /** 关联商品 id 筛选，可选 */
  productId?: number
}

/** 评价新增/编辑共用表单（ReviewSaveRequest，编辑为整体替换语义；productId 空=通用评价） */
export interface ReviewSavePayload {
  buyerName: string
  petName?: string | null
  petBreed?: string | null
  content: string
  avatarUrl?: string | null
  /** 评分，0.0-5.0 一位小数；缺省 5.0 */
  rating?: number | null
  /** 排序号，0-999999 整数；缺省 0 */
  sortOrder?: number | null
  /** 关联商品 id，空表示通用评价；非空时须存在且未删除（服务层校验） */
  productId?: number | null
  /** 展示状态，缺省 ACTIVE */
  status?: AdminStatus
}

/* ==================== 优惠券模板（AdminCouponTemplateController） ==================== */

/** 管理端优惠券模板视图（AdminCouponTemplateVO：全量列表/表单回显共用，附发放/使用统计） */
export interface AdminCouponTemplateVO {
  id: number
  /** 券名称（全局唯一；自动发券按名称「新人专享券」定位模板） */
  name: string
  /** 优惠类型（FIXED_AMOUNT-固定满减，本期唯一取值） */
  discountType: string
  /** 抵扣面额（元） */
  amount: number
  /** 使用门槛（元；0 表示无门槛） */
  minSpend: number
  /** 有效天数（发放时按 发放时间 + valid_days 计算过期时间） */
  validDays: number
  remark: string | null
  /** 状态（ACTIVE 启用，可被注册/订阅自动发放；INACTIVE 停用，不再自动发放） */
  status: AdminStatus
  createTime: string
  /** 已发放数（user_coupon 中该模板的持券记录总数） */
  issuedCount: number
  /** 已使用数（user_coupon 中该模板已核销的记录数；使用率 = usedCount/issuedCount） */
  usedCount: number
}

/** 券模板新增/编辑共用表单（CouponTemplateSaveRequest，编辑为整体替换语义；名称全局唯一） */
export interface CouponTemplateSavePayload {
  name: string
  /** 抵扣面额，> 0 的整数（元） */
  amount: number
  /** 使用门槛，≥ 0 的整数（元）；0 表示无门槛 */
  minSpend: number
  /** 有效天数，> 0 的整数 */
  validDays: number
  /** 备注说明，最长 255 字符 */
  remark?: string | null
}

/* ==================== 日志管理（AdminLogsController，沿用 UpdateLogService） ==================== */

/** 日志列表项（UpdateLogVO；createTime 已由服务端格式化为 yyyy-MM-dd HH:mm 字符串） */
export interface UpdateLogVO {
  id: number
  title: string
  /** 日志类型（如「AI自习室通知」） */
  type: string | null
  createTime: string
  /** 发布状态（0-未发布 1-已发布） */
  publishStatus: number
  /** 点击量（预留字段，默认 0） */
  viewCount: number
}

/** 日志详情（UpdateLogDetailVO：较列表项多正文 content 与更新时间，编辑弹窗回填用） */
export interface UpdateLogDetailVO extends UpdateLogVO {
  content: string
  updateTime: string
}

/** 日志分页结果（UpdateLogPageVO：{total, list}，注意与 PageResult{records,total} 结构不同） */
export interface UpdateLogPageVO {
  /** 关键字过滤后的总记录数 */
  total: number
  /** 当前页日志列表 */
  list: UpdateLogVO[]
}

/** 日志列表查询参数（对齐 AdminLogsController#list：参数名是 pageSize 不是 size；keyword 匹配标题） */
export interface AdminLogQuery {
  page: number
  pageSize: number
  /** 标题关键字模糊搜索，可选，最长 64 字符 */
  keyword?: string
}

/** 日志新增表单（CreateLogRequest：title/content 必填；type 空=「AI自习室通知」；status 缺省 0 未发布） */
export interface CreateLogPayload {
  title: string
  /** 最长 64 字符，为空时服务端默认「AI自习室通知」 */
  type?: string | null
  content: string
  /** 发布状态，0-未发布 1-已发布；缺省 0 */
  status?: number
}

/** 日志编辑表单（UpdateLogRequest：仅 title/type/content；发布状态走独立 /status 接口） */
export interface UpdateLogPayload {
  title: string
  type?: string | null
  content: string
}

/** 日志发布状态切换请求体（UpdateLogStatusRequest：status 只接受 0/1） */
export interface UpdateLogStatusPayload {
  status: number
}
