/**
 * 商城侧 VO 类型：与后端 vo/ 下非 admin 的 record 逐字段对齐。
 * 来源：vo/category/CategoryVO、vo/product/ProductVO|ProductSummaryVO、
 *      vo/bundle/BundleVO、vo/review/ReviewVO、vo/subscription/SubscriptionVO、
 *      vo/cart/CartVO|CartItemVO、vo/order/OrderVO|OrderItemVO、vo/coupon/CouponVO。
 * 映射约定：Long → number，BigDecimal → number，String 枚举 → string。
 */

/** 商品分类（首页分类导航，GET /api/categories） */
export interface CategoryVO {
  id: number
  /** 分类编码（全局唯一，商品筛选参数 category 使用该值，all 表示全部） */
  code: string
  name: string
  description: string
}

/** 商品详情/摘要（GET /api/products、GET /api/products/{id}） */
export interface ProductVO {
  id: number
  /** SKU 编码（全局唯一） */
  sku: string
  name: string
  /** 适用宠物类型（如猫、狗） */
  petType: string
  /** 口味/风味 */
  flavor: string
  price: number
  /** 平均评分（1 位小数，满分 5.0） */
  rating: number
  stock: number
  imageUrl: string
  description: string
  /** 所属分类名称列表（后端 javadoc 口径；旧页面渲染时按分类编码字典转名） */
  categories: string[]
  /** 标签名称列表 */
  tags: string[]
}

/** 套餐内商品摘要（BundleVO.items 元素） */
export interface ProductSummaryVO {
  id: number
  name: string
  price: number
  imageUrl: string
  /** 套餐内包含件数 */
  quantity: number
  /** 商品已下架/删除时为 true，展示时置灰 */
  offShelf: boolean | null
}

/** 场景套餐（GET /api/bundles） */
export interface BundleVO {
  id: number
  name: string
  description: string
  /** 套餐现价 */
  price: number
  /** 原价（划线价） */
  originalPrice: number
  /** 折扣角标文案 */
  discountLabel: string | null
  /** 套餐内商品清单 */
  items: ProductSummaryVO[]
}

/** 用户评价（GET /api/reviews，支持 productId 过滤） */
export interface ReviewVO {
  id: number
  productId: number
  buyerName: string
  petName: string
  petBreed: string
  content: string
  avatarUrl: string
  /** 所购商品名称 */
  purchasedProduct: string
  /** 星级评分（1 位小数，满分 5.0） */
  rating: number
}

/** 邮件订阅结果（POST /api/subscriptions） */
export interface SubscriptionVO {
  id: number
  /** 订阅邮箱（统一小写存储） */
  email: string
  /** 订阅状态（ACTIVE-已订阅） */
  status: string
}

/** 购物车条目（vo/cart/CartItemVO，GET/POST/PUT/DELETE /api/cart** 的返回元素） */
export interface CartItemVO {
  productId: number
  productName: string
  /** 商品主图地址 */
  productImage: string
  /** 商品单价（结算时按此价格计） */
  price: number
  quantity: number
  /** 小计金额（单价 × 数量） */
  subtotal: number
  /** 勾选状态（是否参与结算，加购时默认勾选） */
  checked: boolean | null
}

/** 购物车聚合（vo/cart/CartVO，购物车接口的统一返回体） */
export interface CartVO {
  items: CartItemVO[] | null
  /** 购物车商品总件数（各条目数量之和） */
  count: number | null
  /** 购物车总金额（各条目小计之和） */
  totalAmount: number | null
}

/** 订单明细（vo/order/OrderItemVO，名称/图片/单价均为下单时快照） */
export interface OrderItemVO {
  productId: number
  productName: string
  productImage: string
  /** 成交单价（下单时快照） */
  price: number
  quantity: number
  subtotal: number
}

/** 订单（vo/order/OrderVO，POST/GET /api/orders** 的返回体） */
export interface OrderVO {
  id: number
  /** 订单号（全局唯一，展示与取消的凭证） */
  orderNo: string
  /** 订单总金额（商品总额 − 券抵扣，不为负） */
  totalAmount: number
  /** 订单状态（CREATED-已创建，CANCELLED-已取消） */
  status: string
  /** 下单时间 */
  createdAt: string
  items: OrderItemVO[] | null
}

/** 我的优惠券条目（vo/coupon/CouponVO，GET /api/coupons，需登录） */
export interface CouponVO {
  /** 持券实例 id（user_coupon.id，下单接口 couponId 传此 id） */
  id: number
  /** 券模板 id */
  couponId: number
  /** 券名称（如：新人专享券） */
  name: string
  /** 抵扣金额（元） */
  amount: number
  /** 使用门槛（元，0-无门槛） */
  minSpend: number
  /** 展示状态（0-未使用 1-已使用 2-已过期，过期为懒判定展示态） */
  status: number
  /** 发放时间 */
  receivedAt: string
  /** 过期时间 */
  expiresAt: string
  /** 是否可用（未使用且未过期；门槛由前端结合购物车金额判断） */
  available: boolean | null
}

/** 我的收藏条目（vo/favorite/FavoriteVO，GET /api/favorites，需登录） */
export interface FavoriteVO {
  /** 收藏记录 id */
  id: number
  /** 商品 id（加购、取消收藏均以此为准） */
  productId: number
  productName: string
  /** 商品主图地址 */
  productImage: string
  /** 售价（单位：元） */
  price: number
  petType: string
  flavor: string
  /** 评分（0.0-5.0） */
  rating: number
  /** 收藏时间 */
  createTime: string
}

/** 下单请求体（dto/order/CreateOrderRequest）：couponId 为 null/缺省表示不使用券 */
export interface CreateOrderPayload {
  /** 用户持券实例 id（user_coupon.id，非模板 id）；仅登录用户可用券 */
  couponId?: number | null
}
