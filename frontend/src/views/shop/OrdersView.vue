<script setup lang="ts">
/**
 * 我的订单页（/orders）：迁移旧 script.js 的 orders drawer
 * （loadOrders / renderOrders / toggleOrderDetail / renderOrderDetail / cancelOrder）为独立页面。
 *
 * - 游客与登录用户均可查看自己的订单（后端按 X-Guest-Id / token 归属过滤）；
 * - 列表接口已随单附带明细条目（后端按 orderId 批量装载），展开明细无需二次请求；
 * - 订单状态：CREATED-已创建（可取消，取消后后端回补库存并返还已核销优惠券）/ CANCELLED-已取消；
 * - 取消使用 ElMessageBox 二次确认，替代旧页 window.confirm，其余交互与旧页一致。
 */
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import { cancelOrder, fetchOrders } from '@/api/order'
import type { OrderVO } from '@/types/shop'
import { toFriendlyError } from '@/utils/error'
import { formatDateTime, formatPrice, orderStatusText } from '@/utils/format'

/** 与旧页一致的列表页容量（后端 size 上限 100，这里取旧页默认 20） */
const ORDER_PAGE_SIZE = 20

const orders = ref<OrderVO[]>([])
const total = ref(0)
const loading = ref(true)
const error = ref('')
/** 当前展开明细的订单 id 集合 */
const expandedIds = ref(new Set<number>())
/** 正在取消的订单 id（按钮禁用防重复提交） */
const cancellingId = ref<number | null>(null)

async function loadOrders() {
  loading.value = true
  error.value = ''
  try {
    const result = await fetchOrders({ page: 1, size: ORDER_PAGE_SIZE })
    orders.value = Array.isArray(result?.records) ? result.records : []
    total.value = Number(result?.total || 0)
    expandedIds.value.clear()
  } catch (err) {
    orders.value = []
    total.value = 0
    error.value = toFriendlyError(err)
  } finally {
    loading.value = false
  }
}

/** 展开/收起订单明细（明细已随列表返回） */
function toggleDetail(order: OrderVO) {
  const next = new Set(expandedIds.value)
  if (next.has(order.id)) {
    next.delete(order.id)
  } else {
    next.add(order.id)
  }
  expandedIds.value = next
}

function isExpanded(order: OrderVO): boolean {
  return expandedIds.value.has(order.id)
}

/** 取消订单：仅 CREATED 状态可取消；确认框取消时不发请求 */
async function handleCancel(order: OrderVO) {
  try {
    await ElMessageBox.confirm('确定要取消这个订单吗？取消后不可恢复。', '取消订单', {
      type: 'warning',
      confirmButtonText: '确定取消订单',
      cancelButtonText: '再想想',
    })
  } catch {
    return
  }

  if (cancellingId.value !== null) return
  cancellingId.value = order.id
  try {
    await cancelOrder(order.id)
    ElMessage.success('订单已取消')
    await loadOrders()
  } catch {
    // 失败已由拦截器弹错（状态不可取消/网络异常等）
  } finally {
    cancellingId.value = null
  }
}

onMounted(loadOrders)
</script>

<template>
  <section class="section orders-page" aria-labelledby="orders-title">
    <div class="section-heading">
      <p class="eyebrow">Orders</p>
      <h2 id="orders-title">我的订单</h2>
    </div>

    <div class="orders-list" aria-live="polite">
      <div v-if="loading" class="empty-state">订单加载中...</div>

      <div v-else-if="error" class="empty-state">
        <p>{{ error }}</p>
        <button type="button" @click="loadOrders">重新加载</button>
      </div>

      <!-- 类名沿用旧 styles.css 的 order-card / order-summary / order-detail 等 -->
      <template v-else>
        <article v-for="order in orders" :key="order.id" class="order-card">
        <button
          class="order-summary"
          type="button"
          :aria-expanded="isExpanded(order)"
          @click="toggleDetail(order)"
        >
          <span class="order-row">
            <span class="order-no">{{ order.orderNo }}</span>
            <span class="order-status" :class="{ cancelled: order.status === 'CANCELLED' }">
              {{ orderStatusText(order.status) }}
            </span>
          </span>
          <span class="order-row">
            <span class="order-time">{{ formatDateTime(order.createdAt) }}</span>
            <strong class="order-amount">{{ formatPrice(order.totalAmount) }}</strong>
          </span>
          <span class="order-toggle-hint">{{ isExpanded(order) ? '收起明细 ▴' : '查看明细 ▾' }}</span>
        </button>

        <div v-if="isExpanded(order)" class="order-detail">
          <div class="order-items">
            <div
              v-for="item in order.items || []"
              :key="`${item.itemType}-${item.productId}`"
              class="order-item"
            >
              <img :src="item.productImage" :alt="item.productName" />
              <div>
                <span class="order-item-name">
                  {{ item.productName }}
                  <em v-if="item.itemType === 'BUNDLE'" class="order-item-type">套餐</em>
                </span>
                <span class="order-item-meta">
                  {{ formatPrice(item.price) }} × {{ item.quantity }}
                </span>
              </div>
              <strong>{{ formatPrice(item.subtotal) }}</strong>
            </div>
          </div>
          <div class="order-detail-footer">
            <span>
              共 {{ (order.items || []).length }} 种商品，合计
              <strong class="order-amount">{{ formatPrice(order.totalAmount) }}</strong>
            </span>
            <button
              v-if="order.status === 'CREATED'"
              class="cancel-order-button"
              type="button"
              :disabled="cancellingId === order.id"
              @click="handleCancel(order)"
            >
              {{ cancellingId === order.id ? '取消中...' : '取消订单' }}
            </button>
          </div>
        </div>
        </article>
      </template>

      <div v-if="!loading && !error && orders.length" class="orders-page-hint">
        已显示最近 {{ orders.length }} 单<template v-if="total > orders.length">，共 {{ total }} 单</template>
      </div>
    </div>

    <div v-if="!loading && !error && !orders.length" class="orders-empty">
      还没有订单，去挑一份毛孩子喜欢的零食下单吧。
    </div>
  </section>
</template>

<style scoped>
/* 套餐条目标识（名称后金色小药丸） */
.order-item-type {
  display: inline-block;
  margin-left: 6px;
  padding: 1px 8px;
  border-radius: 999px;
  background: var(--gold);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  font-style: normal;
  vertical-align: middle;
}

/* 订单页容器：旧 .orders-list 为抽屉内滚动网格，页面化后限宽居中 */
.orders-page {
  max-width: 760px;
  margin: 0 auto;
}

.orders-page .orders-list {
  overflow: visible;
}

.orders-page-hint {
  color: var(--muted);
  font-size: 13px;
  text-align: center;
}

/* 旧 .orders-empty 依赖抽屉 flex 的 auto 边距，页面内改为常规居中 */
.orders-page .orders-empty {
  margin: 24px 0;
}
</style>
