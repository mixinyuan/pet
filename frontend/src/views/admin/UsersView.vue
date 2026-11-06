<script setup lang="ts">
/**
 * 用户管理页（/admin/users）：只读列表（关键字/状态筛选 + 汇总列）+ 用户详情抽屉
 * （基本资料/订单记录/收藏/持券/邮箱订阅聚合）+ 禁用/启用切换。
 * 交互对齐旧版 admin-console-modules 用户模块：
 * - 禁用需二次确认（禁用不清数据、不删订单，已签发登录凭证在过期前仍有效），启用直接执行；
 * - Switch 乐观翻转，失败回滚（如禁用 ADMIN 账号被后端 400 拒绝，报错文案由拦截器弹出）。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import { fetchAdminUser, fetchAdminUsers, updateAdminUserStatus } from '@/api/admin/user'
import type { AdminStatus, AdminUserDetailVO, AdminUserVO } from '@/types/admin'
import { couponStatusText, formatDateTime, orderStatusText } from '@/utils/format'

/** 与旧版一致的每页条数（后端 size clamp 到 [1,100]） */
const PAGE_SIZE = 10

/** 头像占位色块调色板（与旧版 FEB_THUMB_COLORS 一致） */
const AVATAR_COLORS = [
  '#7f9cf5', '#68d391', '#f6ad55', '#f687b3', '#63b3ed', '#f6e05e',
  '#b794f4', '#4fd1c5', '#fc8181', '#9ae6b4', '#fbd38d', '#90cdf4',
  '#ecc94b', '#fed7d7',
]

/* ---------------- 列表状态与加载 ---------------- */

const rows = ref<AdminUserVO[]>([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const filters = reactive({
  keyword: '',
  /** ACTIVE/INACTIVE/all */
  status: 'all' as 'all' | AdminStatus,
})

const filtersApplied = computed(() => filters.keyword.trim() !== '' || filters.status !== 'all')
const emptyText = computed(() => (filtersApplied.value ? '未找到匹配的用户' : '暂无用户'))

async function load() {
  const params: { page: number; size: number; keyword?: string; status?: AdminStatus } = {
    page: page.value,
    size: PAGE_SIZE,
  }
  const keyword = filters.keyword.trim()
  if (keyword) params.keyword = keyword
  if (filters.status !== 'all') params.status = filters.status
  loading.value = true
  try {
    const data = await fetchAdminUsers(params)
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

onMounted(load)

/** 搜索/筛选变化：重置到第 1 页（与旧版一致） */
function search() {
  page.value = 1
  load()
}

/* ---------------- 展示辅助 ---------------- */

function formatMoney(value: number | null | undefined) {
  return `¥${Number(value ?? 0).toFixed(2)}`
}

function avatarColor(id: number) {
  return AVATAR_COLORS[Math.abs(Number.isFinite(id) ? id : 0) % AVATAR_COLORS.length]
}

function displayName(user: { id: number; nickname: string | null; username: string }) {
  return user.nickname || user.username || `用户 #${user.id}`
}

/** 持券列 tag 类型：可用绿 / 已使用灰 / 已过期红 */
function couponTagType(status: number) {
  if (status === 0) return 'success'
  if (status === 2) return 'danger'
  return 'info'
}

/* ---------------- 禁用/启用（禁用二次确认；乐观更新失败回滚） ---------------- */

const switchingId = ref<number | null>(null)

/** 状态切换请求：成功同步本地行并 toast；失败回滚（后端文案由拦截器弹出） */
async function putUserStatus(row: AdminUserVO, next: AdminStatus) {
  switchingId.value = row.id
  try {
    await updateAdminUserStatus(row.id, next)
    row.status = next
    ElMessage.success(next === 'ACTIVE' ? '账号已启用' : '账号已禁用')
  } catch {
    row.status = next === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE' // 回滚（如禁用 ADMIN 账号被拒绝）
  } finally {
    switchingId.value = null
  }
}

/** 禁用需二次确认，确认文案保留旧版语义；启用直接执行 */
async function requestStatusChange(row: AdminUserVO, next: AdminStatus) {
  if (next === 'INACTIVE') {
    switchingId.value = row.id // 确认期间锁住该行开关
    const confirmed = await ElMessageBox.confirm(
      '禁用后该用户无法登录与下单，数据与订单保留；已签发的登录凭证在过期前仍有效。',
      `禁用账号「${displayName(row)}」？`,
      { type: 'warning', confirmButtonText: '确认禁用', cancelButtonText: '取消', confirmButtonClass: 'el-button--danger' },
    ).catch(() => false)
    if (!confirmed) {
      row.status = 'ACTIVE' // Switch 已翻转到 INACTIVE，回弹（按钮路径本来也是 ACTIVE）
      switchingId.value = null
      return
    }
  }
  putUserStatus(row, next)
}

/** Switch 触发：v-model 已翻转；用户在二次确认中点取消时回弹 */
function handleSwitchChange(row: AdminUserVO) {
  requestStatusChange(row, row.status)
}

/** 操作列按钮触发：目标状态与当前相反 */
function handleToggle(row: AdminUserVO) {
  requestStatusChange(row, row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE')
}

/* ---------------- 用户详情抽屉 ---------------- */

const drawerVisible = ref(false)
const drawerLoading = ref(false)
const detail = ref<AdminUserDetailVO | null>(null)

async function openDetail(id: number) {
  drawerVisible.value = true
  drawerLoading.value = true
  detail.value = null
  try {
    detail.value = await fetchAdminUser(id)
  } catch {
    drawerVisible.value = false
  } finally {
    drawerLoading.value = false
  }
}

/** 有效订单（CREATED）与累计消费（口径同列表汇总字段） */
const validOrders = computed(() => detail.value?.orders.filter((o) => o.status === 'CREATED') ?? [])
const validSpend = computed(() =>
  validOrders.value.reduce((sum, o) => sum + Number(o.totalAmount || 0), 0),
)
</script>

<template>
  <el-card shadow="never" class="users-card">
    <div class="page-head">
      <h2>用户管理</h2>
      <p>只读查阅 + 禁用/启用；禁用不清数据、不删订单，已签发登录凭证在过期前仍有效</p>
    </div>

    <!-- 工具栏：状态筛选 / 关键字（对齐旧版 toolbar） -->
    <div class="tab-toolbar">
      <el-select v-model="filters.status" style="width: 120px" @change="search">
        <el-option label="全部状态" value="all" />
        <el-option label="正常" value="ACTIVE" />
        <el-option label="已禁用" value="INACTIVE" />
      </el-select>
      <div class="spacer" />
      <el-input
        v-model="filters.keyword"
        placeholder="用户名 / 昵称 / 邮箱"
        clearable
        style="width: 220px"
        @keyup.enter="search"
        @clear="search"
      />
    </div>

    <el-table v-loading="loading" :data="rows" :empty-text="emptyText">
      <el-table-column label="ID" width="70" align="center">
        <template #default="{ row }">{{ row.id }}</template>
      </el-table-column>
      <el-table-column label="用户" min-width="180">
        <template #default="{ row }">
          <div class="user-cell">
            <span class="avatar" :style="{ background: avatarColor(row.id) }">
              {{ String(displayName(row)).slice(0, 1) }}
            </span>
            <div class="user-meta">
              <div class="user-name">{{ displayName(row) }}</div>
              <div class="user-sub">{{ row.username || '—' }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="邮箱" min-width="180">
        <template #default="{ row }">{{ row.email || '—' }}</template>
      </el-table-column>
      <el-table-column label="订单数" width="80" align="right">
        <template #default="{ row }">{{ row.orderCount || 0 }}</template>
      </el-table-column>
      <el-table-column label="消费金额" width="100" align="right">
        <template #default="{ row }">{{ formatMoney(row.totalSpend) }}</template>
      </el-table-column>
      <el-table-column label="持券" width="120" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.couponTotal > 0" size="small" type="warning">
            {{ row.couponAvailable }} 可用 / {{ row.couponTotal }}
          </el-tag>
          <span v-else class="dim">—</span>
        </template>
      </el-table-column>
      <el-table-column label="账号状态" width="100">
        <template #default="{ row }">
          <el-switch
            v-model="row.status"
            active-value="ACTIVE"
            inactive-value="INACTIVE"
            :disabled="switchingId === row.id"
            inline-prompt
            active-text="启用"
            inactive-text="禁用"
            @change="handleSwitchChange(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="注册时间" width="140">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
          <el-button v-if="row.status === 'ACTIVE'" link type="danger" @click="handleToggle(row)">
            禁用
          </el-button>
          <el-button v-else link type="primary" @click="handleToggle(row)">启用</el-button>
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

    <!-- 用户详情抽屉：基本资料 + 订单记录 + 收藏 + 持券 + 邮箱订阅 -->
    <el-drawer v-model="drawerVisible" title="用户详情" size="440px">
      <div v-loading="drawerLoading" class="drawer-body">
        <template v-if="detail">
          <div class="drawer-head">
            <span class="avatar avatar-lg" :style="{ background: avatarColor(detail.id) }">
              {{ String(displayName(detail)).slice(0, 1) }}
            </span>
            <div>
              <div class="drawer-name">
                {{ displayName(detail) }}
                <el-tag :type="detail.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
                  {{ detail.status === 'ACTIVE' ? '正常' : '已禁用' }}
                </el-tag>
              </div>
              <div class="user-sub">{{ detail.username || '—' }}</div>
            </div>
          </div>

          <div class="drawer-section">
            <h4>基本资料</h4>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="邮箱">{{ detail.email || '—' }}</el-descriptions-item>
              <el-descriptions-item label="注册时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
              <el-descriptions-item label="角色">
                <el-tag v-if="detail.role === 'ADMIN'" size="small">管理员</el-tag>
                <span v-else>普通用户</span>
              </el-descriptions-item>
              <el-descriptions-item label="有效订单">
                {{ validOrders.length }} 笔 · 累计 {{ formatMoney(validSpend) }}
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <div class="drawer-section">
            <h4>订单记录（{{ detail.orders.length }}）</h4>
            <template v-if="detail.orders.length">
              <div v-for="(o, i) in detail.orders" :key="i" class="mini-item">
                <span class="mono ellipsis" :title="o.orderNo">{{ o.orderNo }}</span>
                <span class="mini-amount">{{ formatMoney(o.totalAmount) }}</span>
                <el-tag :type="o.status === 'CREATED' ? 'success' : 'info'" size="small">
                  {{ orderStatusText(o.status) }}
                </el-tag>
                <span class="mini-time">{{ formatDateTime(o.createdAt) }}</span>
              </div>
            </template>
            <div v-else class="dim">暂无订单</div>
          </div>

          <div class="drawer-section">
            <h4>收藏（{{ detail.favorites.total }}）</h4>
            <template v-if="detail.favorites.products.length">
              <div v-for="p in detail.favorites.products" :key="p.productId" class="mini-item">
                <el-image :src="p.imageUrl" fit="cover" loading="lazy" class="fav-thumb">
                  <template #error>
                    <div class="prod-thumb-fb" :style="{ background: avatarColor(p.productId) }">
                      {{ String(p.name || '·').slice(0, 1) }}
                    </div>
                  </template>
                </el-image>
                <span class="ellipsis" :title="p.name">{{ p.name }}</span>
                <b class="mini-amount">{{ formatMoney(p.price) }}</b>
              </div>
              <div v-if="detail.favorites.total > detail.favorites.products.length" class="dim more-note">
                … 共 {{ detail.favorites.total }} 件收藏，仅展示最近 {{ detail.favorites.products.length }} 件
              </div>
            </template>
            <div v-else class="dim">暂无收藏</div>
          </div>

          <div class="drawer-section">
            <h4>优惠券（{{ detail.coupons.length }}）</h4>
            <template v-if="detail.coupons.length">
              <div v-for="(c, i) in detail.coupons" :key="i" class="mini-item">
                <span class="ellipsis" :title="c.name">{{ c.name }}</span>
                <span class="mini-time">至 {{ formatDateTime(c.expiresAt) }}</span>
                <el-tag :type="couponTagType(c.status)" size="small">{{ couponStatusText(c.status) }}</el-tag>
              </div>
            </template>
            <div v-else class="dim">暂无持券</div>
          </div>

          <div class="drawer-section">
            <h4>邮箱订阅</h4>
            <div v-if="detail.subscription && detail.subscription.subscribed">
              <el-tag type="success" size="small">已订阅</el-tag>
              <span class="dim sub-note">（「订阅回馈券」发放来源）</span>
            </div>
            <div v-else class="dim">未订阅</div>
          </div>
        </template>
      </div>
    </el-drawer>
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

.spacer {
  flex: 1;
}

.dim {
  color: #c0c4cc;
}

.pager {
  margin-top: 14px;
  justify-content: flex-end;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  flex: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

.avatar-lg {
  width: 44px;
  height: 44px;
  font-size: 17px;
}

.user-meta {
  min-width: 0;
}

.user-name {
  font-weight: 500;
  color: #1f2937;
}

.user-sub {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
}

.drawer-body {
  min-height: 200px;
}

.drawer-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 14px;
  border-bottom: 1px solid #f3f4f6;
}

.drawer-name {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  display: flex;
  align-items: center;
  gap: 8px;
}

.drawer-section {
  margin-top: 16px;
}

.drawer-section h4 {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin: 0 0 10px;
}

.mini-item {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 7px 0;
  border-bottom: 1px dashed #f3f4f6;
  font-size: 12.5px;
}

.mini-item:last-of-type {
  border-bottom: 0;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}

.ellipsis {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mini-amount {
  flex: none;
}

.mini-time {
  flex: none;
  color: #c0c4cc;
}

.fav-thumb {
  width: 32px;
  height: 32px;
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

.more-note {
  padding: 6px 0;
}

.sub-note {
  font-size: 12px;
}
</style>
