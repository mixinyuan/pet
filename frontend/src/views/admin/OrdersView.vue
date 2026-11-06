<script setup lang="ts">
/**
 * 订单管理页（/admin/orders）：只读查阅（状态/日期区间/订单号·下单人筛选 + 分页）
 * + 订单详情弹窗（商品快照 + 用券信息）+ 管理员兜底取消（红色二次确认，不可逆）。
 * 交互对齐旧版 admin-console-modules 订单模块：
 * - 单搜索框分流：PB 前缀按订单号精确查询（转大写），其余按下单人关键字模糊；
 * - 取消将回补商品库存并退还该订单核销的优惠券，二次确认文案保留。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

import { cancelAdminOrder, fetchAdminOrder, fetchAdminOrders } from '@/api/admin/order'
import type { AdminOrderDetailVO, AdminOrderStatus, AdminOrderVO } from '@/types/admin'
import { formatDateTime, orderStatusText } from '@/utils/format'

/** 与旧版一致的每页条数（后端 size clamp 到 [1,100]） */
const PAGE_SIZE = 10

/** 缩略图占位色块调色板（与旧版 THUMB_COLORS 一致） */
const THUMB_COLORS = [
  '#7f9cf5', '#68d391', '#f6ad55', '#f687b3', '#63b3ed', '#f6e05e',
  '#b794f4', '#4fd1c5', '#fc8181', '#9ae6b4', '#fbd38d', '#90cdf4',
  '#ecc94b', '#fed7d7',
]

/* ---------------- 列表状态与加载 ---------------- */

const rows = ref<AdminOrderVO[]>([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const filters = reactive({
  /** ACTIVE 状态下拉：all/CREATED/CANCELLED */
  status: 'all' as 'all' | AdminOrderStatus,
  /** 下单日期起（yyyy-MM-dd，闭区间） */
  from: '',
  to: '',
  /** 订单号 / 下单人关键字（PB 前缀按订单号精确查询） */
  q: '',
})

const filtersApplied = computed(
  () => filters.q.trim() !== '' || filters.status !== 'all' || filters.from !== '' || filters.to !== '',
)
const emptyText = computed(() => (filtersApplied.value ? '未找到匹配的订单' : '暂无订单'))

async function load() {
  // 日期闭区间前置校验（后端同样校验 400，前端先拦截体验更好）
  if (filters.from && filters.to && filters.from > filters.to) {
    ElMessage.error('开始日期不能晚于结束日期')
    return
  }
  const params: {
    page: number
    size: number
    orderNo?: string
    keyword?: string
    status?: AdminOrderStatus
    from?: string
    to?: string
  } = { page: page.value, size: PAGE_SIZE }
  const kw = filters.q.trim()
  if (kw) {
    // 单搜索框参数分流：订单号为 PB 前缀（后端 eq 精确匹配），其余视为下单人关键字
    if (/^pb/i.test(kw)) params.orderNo = kw.toUpperCase()
    else params.keyword = kw
  }
  if (filters.status !== 'all') params.status = filters.status
  if (filters.from) params.from = filters.from
  if (filters.to) params.to = filters.to

  loading.value = true
  try {
    const data = await fetchAdminOrders(params)
    rows.value = data.records
    total.value = data.total
  } catch {
    // 错误提示由拦截器统一弹出，这里只做收尾
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const route = useRoute()
const router = useRouter()

onMounted(() => {
  // 看板「最近订单」跳转落地：按订单号预填搜索（locate 标记触发定位提示，之后清除）
  if (typeof route.query.q === 'string' && route.query.q.trim()) {
    filters.q = route.query.q.trim()
    if (route.query.locate === '1') {
      ElMessage.info(`已按订单号定位：${filters.q}`)
    }
    router.replace({ query: filters.q ? { q: filters.q } : {} })
  }
  load()
})

/** 搜索/筛选变化：重置到第 1 页（与旧版一致） */
function search() {
  page.value = 1
  load()
}

/** 一键清空全部筛选条件（状态/日期区间/关键字）并重新查询 */
function resetFilters() {
  filters.status = 'all'
  filters.from = ''
  filters.to = ''
  filters.q = ''
  search()
}

/* ---------------- 展示辅助 ---------------- */

function formatMoney(value: number | null | undefined) {
  return `¥${Number(value ?? 0).toFixed(2)}`
}

function thumbColor(seed: number) {
  return THUMB_COLORS[Math.abs(Number.isFinite(seed) ? seed : 0) % THUMB_COLORS.length]
}

function cancelByText(cancelBy: string | null) {
  if (cancelBy === 'ADMIN') return '由管理员取消'
  if (cancelBy === 'USER') return '由用户取消'
  return cancelBy || '—'
}

/* ---------------- 订单详情弹窗 ---------------- */

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<AdminOrderDetailVO | null>(null)

async function openDetail(id: number) {
  detailLoading.value = true
  detailVisible.value = true
  detail.value = null
  try {
    detail.value = await fetchAdminOrder(id)
  } catch {
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

/* ---------------- 管理员兜底取消（红色二次确认，不可逆） ---------------- */

const cancelling = ref(false)

/**
 * 取消订单：列表行或详情弹窗内均可发起；
 * 用券信息优先取详情（弹窗内发起时），否则取行数据。
 */
async function handleCancel(id: number) {
  const src =
    detail.value && detail.value.id === id
      ? detail.value
      : rows.value.find((r) => r.id === id)
  const orderNo = src?.orderNo || `#${id}`
  const couponPart = src?.couponName ? `并<strong>退还用户优惠券「${src.couponName}」</strong>` : ''
  const confirmed = await ElMessageBox.confirm(
    `取消后不可恢复；将自动<strong>回补商品库存</strong>${couponPart}。`,
    `取消订单 ${orderNo}？`,
    {
      type: 'warning',
      confirmButtonText: '确认取消',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger',
      dangerouslyUseHTMLString: true,
    },
  ).catch(() => false)
  if (!confirmed) return
  cancelling.value = true
  try {
    await cancelAdminOrder(id)
    ElMessage.success(`订单已取消：库存已回补${src?.couponName ? '、优惠券已退还' : ''}`)
    detailVisible.value = false // 详情弹窗内发起时一并关闭
    load()
  } catch {
    // 后端拒绝（如订单状态不允许取消 400）时拦截器已弹出报错文案
  } finally {
    cancelling.value = false
  }
}
</script>

<template>
  <el-card shadow="never" class="orders-card">
    <div class="page-head">
      <h2>订单管理</h2>
      <p>只读查阅 + 管理员兜底取消；取消将回补库存并退还优惠券，不可逆</p>
    </div>

    <!-- 工具栏：状态筛选 / 日期区间 / 关键字（对齐旧版 toolbar） -->
    <div class="tab-toolbar">
      <el-select v-model="filters.status" style="width: 120px" @change="search">
        <el-option label="全部状态" value="all" />
        <el-option label="已创建" value="CREATED" />
        <el-option label="已取消" value="CANCELLED" />
      </el-select>
      <el-date-picker
        v-model="filters.from"
        type="date"
        placeholder="开始日期"
        value-format="YYYY-MM-DD"
        style="width: 140px"
        @change="search"
      />
      <span class="range-sep">至</span>
      <el-date-picker
        v-model="filters.to"
        type="date"
        placeholder="结束日期"
        value-format="YYYY-MM-DD"
        style="width: 140px"
        @change="search"
      />
      <div class="spacer" />
      <el-input
        v-model="filters.q"
        placeholder="订单号 / 用户"
        clearable
        style="width: 220px"
        @keyup.enter="search"
        @clear="search"
      />
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" :empty-text="emptyText">
      <el-table-column label="订单号" min-width="180">
        <template #default="{ row }">
          <span class="mono">{{ row.orderNo }}</span>
        </template>
      </el-table-column>
      <el-table-column label="下单用户" min-width="140">
        <template #default="{ row }">{{ row.ownerName || '—' }}</template>
      </el-table-column>
      <el-table-column label="件数" width="70" align="right">
        <template #default="{ row }">{{ row.itemCount || 0 }}</template>
      </el-table-column>
      <el-table-column label="总额" width="100" align="right">
        <template #default="{ row }">{{ formatMoney(row.totalAmount) }}</template>
      </el-table-column>
      <el-table-column label="优惠" min-width="150">
        <template #default="{ row }">
          <!-- 未用券 / 取消后退券（券字段为 null）显示 — -->
          <el-tag v-if="row.couponName" size="small" type="warning" class="coupon-tag">
            {{ row.couponName }} −¥{{ Number(row.couponAmount || 0).toFixed(2) }}
          </el-tag>
          <span v-else class="dim">—</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'CREATED' ? 'success' : 'info'" size="small">
            {{ orderStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="下单时间" width="140">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
          <el-button v-if="row.status === 'CREATED'" link type="danger" @click="handleCancel(row.id)">
            取消
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      class="pager"
      background
      layout="total, prev, pager, next, jumper"
      :total="total"
      :page-size="PAGE_SIZE"
      @current-change="load"
    />

    <!-- 订单详情弹窗：头部信息 / 商品快照表 / 金额汇总 / 取消入口 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="720px">
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions :column="2" border size="small" class="detail-desc">
            <el-descriptions-item label="订单号">
              <span class="mono">{{ detail.orderNo }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="detail.status === 'CREATED' ? 'success' : 'info'" size="small">
                {{ orderStatusText(detail.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="下单用户">
              <!-- 用户单展示 昵称（用户名 · 邮箱），游客单展示 guestId -->
              <template v-if="detail.user">
                {{ detail.ownerName }}（{{ detail.user.username || '—' }} · {{ detail.user.email || '邮箱未填' }}）
              </template>
              <template v-else>
                {{ detail.ownerName }}<template v-if="detail.guestId">（游客单 · guestId {{ detail.guestId }}）</template>
              </template>
            </el-descriptions-item>
            <el-descriptions-item label="下单时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
            <el-descriptions-item v-if="detail.status === 'CANCELLED'" label="取消信息" :span="2">
              {{ formatDateTime(detail.cancelTime) }} · {{ cancelByText(detail.cancelBy) }}
            </el-descriptions-item>
          </el-descriptions>

          <el-table :data="detail.items" size="small" class="items-table">
            <el-table-column label="商品快照" min-width="240">
              <template #default="{ row }">
                <div class="prod-cell">
                  <el-image :src="row.productImage" fit="cover" loading="lazy" class="prod-thumb">
                    <template #error>
                      <div class="prod-thumb-fb" :style="{ background: thumbColor(row.productName.length) }">
                        {{ String(row.productName || '·').slice(0, 1) }}
                      </div>
                    </template>
                  </el-image>
                  <span>{{ row.productName }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="单价" width="100" align="right">
              <template #default="{ row }">{{ formatMoney(row.price) }}</template>
            </el-table-column>
            <el-table-column label="数量" width="70" align="center">
              <template #default="{ row }">{{ row.quantity }}</template>
            </el-table-column>
            <el-table-column label="小计" width="100" align="right">
              <template #default="{ row }">{{ formatMoney(row.subtotal) }}</template>
            </el-table-column>
          </el-table>

          <div class="amount-summary">
            <div>商品合计 <b>{{ formatMoney(detail.itemsSubtotal) }}</b></div>
            <div v-if="detail.couponName" class="coupon-line">
              优惠券「{{ detail.couponName }}」抵扣 −{{ formatMoney(detail.couponAmount) }}
            </div>
            <div v-else class="dim">优惠 —</div>
            <div class="total-line">实付总额 {{ formatMoney(detail.totalAmount) }}</div>
          </div>
          <div class="snapshot-hint">
            * 商品信息为下单时快照，此后改价改名不影响本订单；已取消订单的优惠券已退还，优惠显示为 —
          </div>
        </template>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="detail && detail.status === 'CREATED'"
          type="danger"
          :loading="cancelling"
          @click="handleCancel(detail.id)"
        >
          取消订单
        </el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.page-head {
  margin-bottom: 14px;
}

.page-head h2 {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 4px;
  color: #1f2937;
}

.page-head p {
  font-size: 12.5px;
  color: #9ca3af;
  margin: 0;
}

.tab-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.range-sep {
  font-size: 12px;
  color: #9ca3af;
}

.spacer {
  flex: 1;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12.5px;
}

.coupon-tag {
  max-width: 100%;
}

.dim {
  color: #c0c4cc;
}

.pager {
  margin-top: 14px;
  justify-content: flex-end;
}

.detail-desc {
  margin-bottom: 14px;
}

.items-table {
  margin-bottom: 12px;
}

.prod-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.prod-thumb {
  width: 40px;
  height: 40px;
  border-radius: 6px;
  flex: none;
  overflow: hidden;
}

.prod-thumb-fb {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 600;
}

.amount-summary {
  text-align: right;
  font-size: 13px;
  line-height: 1.9;
}

.coupon-line {
  color: #e6a23c;
}

.total-line {
  font-size: 15px;
  font-weight: 600;
}

.snapshot-hint {
  margin-top: 10px;
  font-size: 12px;
  color: #c0c4cc;
}
</style>
