<script setup lang="ts">
/**
 * 优惠券模板管理页（/admin/coupons，接口 /api/admin/coupon-templates）：
 * 全量列表（不分页，含停用模板与已发放/已使用统计）+ 新建/编辑/启停/删除。
 * 交互对齐旧版 admin-console-modules 券模板模块：
 * - 固定金额券（FIXED_AMOUNT），注册/邮箱订阅自动发放，停用后不再自动发放、存量持券不受影响；
 * - 编辑弹窗顶部黄色警示条：user_coupon 无金额快照，改模板实时作用于存量持券（文案保留旧版）；
 * - 停用需二次确认（文案保留旧版），启用直接执行；Switch 乐观翻转，失败回滚；
 * - 已发放数 > 0 的模板后端拒绝删除（400 文案由拦截器直接展示），删除前有确认提示。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

import {
  createAdminCouponTemplate,
  deleteAdminCouponTemplate,
  fetchAdminCouponTemplates,
  updateAdminCouponTemplate,
  updateAdminCouponTemplateStatus,
} from '@/api/admin/coupon-template'
import type { AdminCouponTemplateVO, AdminStatus, CouponTemplateSavePayload } from '@/types/admin'

/** 金额格式化：¥xx.xx（与旧版 ctx.money 一致） */
function money(value: number | null | undefined) {
  return `¥${Number(value ?? 0).toFixed(2)}`
}

/* ---------------- 列表状态与加载 ---------------- */

const rows = ref<AdminCouponTemplateVO[]>([])
const loading = ref(false)

const emptyText = computed(() => '暂无券模板，点击「新建模板」创建')

async function load() {
  loading.value = true
  try {
    rows.value = await fetchAdminCouponTemplates()
  } catch {
    // 错误提示由拦截器统一弹出，这里只做收尾
    rows.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)

/** 使用率：已使用 / 已发放（取整百分比，未发放过展示 —） */
function usageRate(row: AdminCouponTemplateVO) {
  const issued = Number(row.issuedCount || 0)
  const used = Number(row.usedCount || 0)
  return issued > 0 ? `${Math.round((used / issued) * 100)}%` : '—'
}

/* ---------------- 启用/停用（停用二次确认；乐观更新失败回滚） ---------------- */

const switchingId = ref<number | null>(null)

/** 状态切换请求：成功同步本地行并 toast；失败回滚（后端文案由拦截器弹出） */
async function putStatus(row: AdminCouponTemplateVO, next: AdminStatus) {
  switchingId.value = row.id
  try {
    await updateAdminCouponTemplateStatus(row.id, next)
    row.status = next
    ElMessage.success(next === 'ACTIVE' ? '模板已启用，将恢复自动发放' : '模板已停用，不再自动发放')
  } catch {
    row.status = next === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE' // 回滚
  } finally {
    switchingId.value = null
  }
}

/** 停用需二次确认（确认文案保留旧版语义）；启用直接执行 */
async function requestStatusChange(row: AdminCouponTemplateVO, next: AdminStatus) {
  if (next === 'INACTIVE') {
    switchingId.value = row.id // 确认期间锁住该行开关
    const confirmed = await ElMessageBox.confirm(
      '停用后新用户注册/邮箱订阅将不再自动发放该券，存量持券不受影响。',
      `停用券模板「${row.name}」？`,
      { type: 'warning', confirmButtonText: '确认停用', cancelButtonText: '取消', confirmButtonClass: 'el-button--danger' },
    ).catch(() => false)
    if (!confirmed) {
      row.status = 'ACTIVE' // Switch 已翻转到 INACTIVE，回弹
      switchingId.value = null
      return
    }
  }
  putStatus(row, next)
}

/** Switch 触发：v-model 已翻转；用户在二次确认中点取消时回弹 */
function handleSwitchChange(row: AdminCouponTemplateVO) {
  requestStatusChange(row, row.status)
}

/* ---------------- 删除（已发放 > 0 后端拒绝，文案直接提示） ---------------- */

async function handleDelete(row: AdminCouponTemplateVO) {
  const confirmed = await ElMessageBox.confirm(
    '已发放数 > 0 时后端将拒绝删除（仅支持停用），失败原因将直接提示。',
    `删除模板「${row.name}」？`,
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消', confirmButtonClass: 'el-button--danger' },
  ).catch(() => false)
  if (!confirmed) return
  try {
    await deleteAdminCouponTemplate(row.id)
    ElMessage.success('模板已删除')
    load()
  } catch {
    // 已发放时展示后端 400 文案（由拦截器弹出）
  }
}

/* ---------------- 新建/编辑弹窗 ---------------- */

interface CouponFormState {
  name: string
  amount: number | null
  minSpend: number | null
  validDays: number | null
  remark: string
}

function blankForm(): CouponFormState {
  return {
    name: '',
    amount: null,
    minSpend: null,
    validDays: 30, // 与旧版一致的缺省有效天数
    remark: '',
  }
}

const dialogVisible = ref(false)
const saving = ref(false)
/** 本次弹窗编辑的模板 id（null 表示新建） */
const editingId = ref<number | null>(null)
const form = reactive<CouponFormState>(blankForm())
const formRef = ref<FormInstance>()

/** 正整数（对齐后端 @Min(1)） */
const validatePositiveInt = (label: string) =>
  (_rule: unknown, value: number | null, callback: (error?: Error) => void) => {
    if (value == null) return callback(new Error(`请填写${label}`))
    if (!/^\d+$/.test(String(value)) || Number(value) < 1) {
      return callback(new Error(`${label}须为大于 0 的整数`))
    }
    callback()
  }

/** 非负整数（对齐后端 @Min(0)，0 表示无门槛） */
const validateNonNegativeInt = (label: string) =>
  (_rule: unknown, value: number | null, callback: (error?: Error) => void) => {
    if (value == null) return callback(new Error(`请填写${label}`))
    if (!/^\d+$/.test(String(value))) {
      return callback(new Error(`${label}须为不小于 0 的整数`))
    }
    callback()
  }

const rules: FormRules = {
  name: [
    { required: true, message: '请填写券名称', trigger: 'blur' },
    { max: 64, message: '券名称长度不能超过 64 字符', trigger: 'blur' },
  ],
  amount: [{ required: true, validator: validatePositiveInt('面额'), trigger: 'blur' }],
  minSpend: [{ required: true, validator: validateNonNegativeInt('使用门槛'), trigger: 'blur' }],
  validDays: [{ required: true, validator: validatePositiveInt('有效天数'), trigger: 'blur' }],
  remark: [{ max: 255, message: '备注长度不能超过 255 字符', trigger: 'blur' }],
}

/** 打开弹窗：列表行与详情 VO 同构，直接回填（与旧版一致） */
function openDialog(row: AdminCouponTemplateVO | null) {
  editingId.value = row ? row.id : null
  Object.assign(form, blankForm())
  if (row) {
    Object.assign(form, {
      name: row.name ?? '',
      amount: row.amount ?? null,
      minSpend: row.minSpend ?? null,
      validDays: row.validDays ?? 30,
      remark: row.remark ?? '',
    })
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  const formEl = formRef.value
  if (!formEl) return
  const valid = await formEl.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  const payload: CouponTemplateSavePayload = {
    name: form.name.trim(),
    amount: Number(form.amount),
    minSpend: Number(form.minSpend),
    validDays: Number(form.validDays),
    remark: form.remark.trim() || null,
  }
  try {
    if (editingId.value) {
      await updateAdminCouponTemplate(editingId.value, payload)
      ElMessage.success('模板已保存（已同步生效至存量持券）')
    } else {
      await createAdminCouponTemplate(payload)
      ElMessage.success('模板已创建，将按发放规则自动发放')
    }
    dialogVisible.value = false
    load()
  } catch {
    // 保存失败（如名称重复 409）时保持弹窗打开供修改，后端文案由拦截器弹出
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-card shadow="never">
    <div class="page-head">
      <h2>优惠券模板</h2>
      <p>注册 / 邮箱订阅自动发放；停用后不再自动发放，存量持券展示与核销不受影响</p>
    </div>

    <!-- 工具栏：新建 + 规则提示（对齐旧版 toolbar） -->
    <div class="tab-toolbar">
      <el-button type="primary" :icon="Plus" @click="openDialog(null)">新建模板</el-button>
      <span class="toolbar-tip">固定金额券；已发放 &gt; 0 的模板后端拒绝删除，仅支持停用</span>
    </div>

    <el-table v-loading="loading" :data="rows" :empty-text="emptyText">
      <el-table-column label="券名称" min-width="200">
        <template #default="{ row }">
          <div class="cell-main">{{ row.name }}</div>
          <div v-if="row.remark" class="cell-sub ellipsis" :title="row.remark">{{ row.remark }}</div>
        </template>
      </el-table-column>
      <el-table-column label="面额" width="100" align="right">
        <template #default="{ row }">
          <b>{{ money(row.amount) }}</b>
        </template>
      </el-table-column>
      <el-table-column label="使用门槛" width="130">
        <template #default="{ row }">
          {{ Number(row.minSpend || 0) > 0 ? `满 ${money(row.minSpend)} 可用` : '无门槛' }}
        </template>
      </el-table-column>
      <el-table-column label="有效天数" width="90" align="center">
        <template #default="{ row }">{{ Number(row.validDays || 0) }} 天</template>
      </el-table-column>
      <el-table-column label="已发放" width="80" align="center">
        <template #default="{ row }">{{ Number(row.issuedCount || 0) }}</template>
      </el-table-column>
      <el-table-column label="已使用" width="80" align="center">
        <template #default="{ row }">{{ Number(row.usedCount || 0) }}</template>
      </el-table-column>
      <el-table-column label="使用率" width="80" align="center">
        <template #default="{ row }">{{ usageRate(row) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-switch
            v-model="row.status"
            active-value="ACTIVE"
            inactive-value="INACTIVE"
            :disabled="switchingId === row.id"
            inline-prompt
            active-text="启用"
            inactive-text="停用"
            @change="handleSwitchChange(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新建/编辑模板弹窗：编辑时顶部黄色警示条（无金额快照，改动同步作用存量持券） -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑优惠券模板' : '新建优惠券模板'"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-alert v-if="editingId" type="warning" :closable="false" class="edit-warn">
        <b>修改模板将同步作用于已发放到用户手中的存量优惠券（系统无金额快照）。</b>
        持券展示与核销实时读取模板，请谨慎调整面额与门槛。
      </el-alert>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="券名称" prop="name">
          <el-input v-model="form.name" maxlength="64" placeholder="全局唯一，最长 64 字符" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="面额（元）" prop="amount">
              <el-input-number
                v-model="form.amount"
                :controls="false"
                :min="1"
                :step="1"
                :precision="0"
                class="w-full"
                placeholder="大于 0 的整数"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="使用门槛（元）" prop="minSpend">
              <el-input-number
                v-model="form.minSpend"
                :controls="false"
                :min="0"
                :step="1"
                :precision="0"
                class="w-full"
                placeholder="0 为无门槛"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="领取后有效天数" prop="validDays">
              <el-input-number
                v-model="form.validDays"
                :controls="false"
                :min="1"
                :step="1"
                :precision="0"
                class="w-full"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="2"
            maxlength="255"
            placeholder="发放场景说明，如「注册自动发放」"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">
          {{ editingId ? '保存修改' : '创建模板' }}
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

.toolbar-tip {
  font-size: 12.5px;
  color: #c0c4cc;
}

.cell-main {
  font-weight: 500;
  color: #1f2937;
}

.cell-sub {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
}

.ellipsis {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.w-full {
  width: 100%;
}

.edit-warn {
  margin-bottom: 14px;
}
</style>
