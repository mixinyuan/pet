<script setup lang="ts">
/**
 * 套餐新增/编辑弹窗：主表字段 + 商品明细子表编辑器（对齐旧版 openBundleModal）。
 *
 * - 打开时先拉前 100 个未删除商品作为明细下拉数据源（失败则关闭弹窗，与旧版一致）；
 * - 编辑时再拉套餐详情回填明细（列表行虽含 items，仍以详情接口为准）；
 * - 明细行内商品下拉不出现其他行已选商品（同一商品不可重复）；
 * - 「一键填入明细合计为原价」辅助定价；提交校验对齐 BundleSaveRequest。
 */
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

import { createAdminBundle, fetchAdminBundle, updateAdminBundle } from '@/api/admin/bundle'
import { fetchAdminProducts } from '@/api/admin/product'
import type { AdminBundleVO, AdminProductVO, AdminStatus, BundleSavePayload } from '@/types/admin'

/** 商品下拉数据源拉取条数（后端 size clamp [1,100]，商品超过 100 时仅取前 100 条） */
const OPTION_SIZE = 100

const props = defineProps<{
  modelValue: boolean
  /** 编辑的套餐行（null 表示新增）；明细以详情接口为准，打开时拉取 */
  bundle: AdminBundleVO | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'saved'): void
}>()

/* ---------------- 表单状态 ---------------- */

interface BundleItemRow {
  productId: number | null
  quantity: number
}

interface BundleFormState {
  name: string
  discountLabel: string
  price: number | null
  originalPrice: number | null
  sortOrder: number
  status: AdminStatus
  description: string
  items: BundleItemRow[]
}

function blankForm(): BundleFormState {
  return {
    name: '',
    discountLabel: '',
    price: null,
    originalPrice: null,
    sortOrder: 0,
    status: 'ACTIVE',
    description: '',
    items: [],
  }
}

const form = reactive<BundleFormState>(blankForm())
const formRef = ref<FormInstance>()
const saving = ref(false)
const products = ref<AdminProductVO[]>([])
const productsLoading = ref(false)
/** 本次弹窗编辑的套餐 id（打开时捕获；null 表示新增） */
const editingId = ref<number | null>(null)

function onVisibleChange(value: boolean) {
  emit('update:modelValue', value)
}

/* ---------------- 打开弹窗：拉商品数据源 + 详情回填 ---------------- */

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) prepare()
  },
)

async function prepare() {
  // 先拉全部未删除商品作为明细下拉数据源；失败则关闭弹窗（报错由拦截器弹出）
  productsLoading.value = true
  try {
    const data = await fetchAdminProducts({ page: 1, size: OPTION_SIZE })
    products.value = data.records
  } catch {
    products.value = []
    emit('update:modelValue', false)
    return
  } finally {
    productsLoading.value = false
  }

  editingId.value = props.bundle ? props.bundle.id : null
  Object.assign(form, blankForm())
  if (!props.bundle) return
  // 编辑：再拉详情回填明细（与旧版一致）
  try {
    const detail = await fetchAdminBundle(props.bundle.id)
    Object.assign(form, {
      name: detail.name,
      discountLabel: detail.discountLabel ?? '',
      price: detail.price,
      originalPrice: detail.originalPrice,
      sortOrder: detail.sortOrder ?? 0,
      status: detail.status,
      description: detail.description ?? '',
      items: (detail.items || []).map((it) => ({ productId: it.productId, quantity: Number(it.quantity || 1) })),
    })
  } catch {
    emit('update:modelValue', false)
  }
}

/* ---------------- 明细编辑器 ---------------- */

const productMap = computed(() => new Map(products.value.map((p) => [p.id, p] as const)))

function productPrice(productId: number | null) {
  return productMap.value.get(productId ?? -1)?.price ?? 0
}

/** 某行可选的商品：其他行已选的商品不再出现（同一商品去重） */
function optionsForRow(index: number) {
  const usedByOthers = new Set(form.items.filter((_, j) => j !== index).map((x) => x.productId))
  return products.value.filter((p) => !usedByOthers.has(p.id))
}

/** 明细合计（按商品当前售价 × 数量） */
const itemsTotal = computed(() =>
  form.items.reduce((sum, it) => sum + productPrice(it.productId) * Number(it.quantity || 0), 0),
)

function formatMoney(value: number) {
  return `¥${value.toFixed(2)}`
}

/** 添加一行明细：默认选第一个未被占用的商品 */
function addItem() {
  const used = new Set(form.items.map((it) => it.productId))
  const candidate = products.value.find((p) => !used.has(p.id))
  if (!candidate) {
    ElMessage.warning('没有更多可添加的商品')
    return
  }
  form.items.push({ productId: candidate.id, quantity: 1 })
}

function removeItem(index: number) {
  form.items.splice(index, 1)
}

/** 一键填入：把明细合计写进原价（划线价） */
function fillOriginal() {
  form.originalPrice = Number(itemsTotal.value.toFixed(2))
  ElMessage.success(`已将明细合计 ${formatMoney(itemsTotal.value)} 填入原价`)
}

/* ---------------- 校验与提交 ---------------- */

/** 价格：> 0，最多 8 位整数、2 位小数（对齐后端 @DecimalMin/@Digits） */
const validatePrice = (_rule: unknown, value: number | null, callback: (error?: Error) => void) => {
  if (value == null) return callback(new Error('请填写价格'))
  if (!/^\d{1,8}(\.\d{1,2})?$/.test(String(value)) || Number(value) <= 0) {
    return callback(new Error('价格必须大于 0，最多 8 位整数、2 位小数'))
  }
  callback()
}

/** 原价：在价格规则之上须 ≥ 现价（前端先拦截，服务层兜底） */
const validateOriginalPrice = (_rule: unknown, value: number | null, callback: (error?: Error) => void) => {
  if (value == null) return callback(new Error('请填写原价'))
  if (!/^\d{1,8}(\.\d{1,2})?$/.test(String(value)) || Number(value) <= 0) {
    return callback(new Error('原价必须大于 0，最多 8 位整数、2 位小数'))
  }
  if (form.price != null && Number(value) < Number(form.price)) {
    return callback(new Error('原价应不低于现价'))
  }
  callback()
}

/** 明细：至少 1 行且每行已选商品 */
const validateItems = (_rule: unknown, value: BundleItemRow[], callback: (error?: Error) => void) => {
  if (!value || value.length < 1) return callback(new Error('套餐至少包含 1 个商品'))
  if (value.some((it) => it.productId == null)) return callback(new Error('每行明细均需选择商品'))
  callback()
}

const rules: FormRules = {
  name: [
    { required: true, message: '请填写套餐名称', trigger: 'blur' },
    { max: 100, message: '套餐名称长度不能超过 100 字符', trigger: 'blur' },
  ],
  price: [{ required: true, validator: validatePrice, trigger: 'blur' }],
  originalPrice: [{ required: true, validator: validateOriginalPrice, trigger: 'blur' }],
  discountLabel: [{ max: 20, message: '折扣文案长度不能超过 20 字符', trigger: 'blur' }],
  description: [{ max: 200, message: '描述长度不能超过 200 字符', trigger: 'blur' }],
  items: [{ required: true, validator: validateItems, trigger: 'change' }],
}

async function handleSubmit() {
  const formEl = formRef.value
  if (!formEl) return
  const valid = await formEl.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  const payload: BundleSavePayload = {
    name: form.name.trim(),
    price: Number(form.price),
    originalPrice: Number(form.originalPrice),
    discountLabel: form.discountLabel.trim(),
    description: form.description.trim(),
    sortOrder: form.sortOrder,
    status: form.status,
    items: form.items.map((it) => ({ productId: Number(it.productId), quantity: Number(it.quantity) })),
  }
  try {
    if (editingId.value) {
      await updateAdminBundle(editingId.value, payload)
      ElMessage.success(`套餐「${payload.name}」已保存`)
    } else {
      await createAdminBundle(payload)
      ElMessage.success(`套餐「${payload.name}」已创建`)
    }
    emit('update:modelValue', false)
    emit('saved')
  } catch {
    // 保存失败（如明细无效 400）时保持弹窗打开供修改，报错由拦截器弹出
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="editingId ? '编辑套餐' : '新增套餐'"
    width="780px"
    :close-on-click-modal="false"
    @update:model-value="onVisibleChange"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" v-loading="productsLoading">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="套餐名称" prop="name">
            <el-input v-model="form.name" maxlength="100" placeholder="最长 100 字符" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="折扣文案" prop="discountLabel">
            <el-input v-model="form.discountLabel" maxlength="20" placeholder="如：省 18%，留空不显示角标" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="现价（元）" prop="price">
            <el-input-number
              v-model="form.price"
              :controls="false"
              :min="0"
              :max="99999999.99"
              :precision="2"
              placeholder="> 0，最多 8 位整数、2 位小数"
              class="w-full"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="原价（元）" prop="originalPrice">
            <el-input-number
              v-model="form.originalPrice"
              :controls="false"
              :min="0"
              :max="99999999.99"
              :precision="2"
              placeholder="应 ≥ 现价（划线价）"
              class="w-full"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="排序号">
            <el-input-number v-model="form.sortOrder" :precision="0" :step="1" class="w-full" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="上架状态">
            <el-select v-model="form.status" style="width: 100%">
              <el-option label="上架" value="ACTIVE" />
              <el-option label="下架" value="INACTIVE" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="套餐描述" prop="description">
            <el-input v-model="form.description" type="textarea" :rows="3" maxlength="200" show-word-limit />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 商品明细子表编辑器：同一商品不可重复（下拉互斥）、数量 1-99、一键填原价 -->
      <el-form-item label="商品明细（至少 1 行，同一商品不可重复）" prop="items">
        <div class="bi-tools">
          <el-button size="small" :icon="Plus" @click="addItem">添加商品</el-button>
          <el-button size="small" @click="fillOriginal">一键填入明细合计为原价</el-button>
          <div class="spacer" />
          <span class="bi-total">明细合计 <b>{{ formatMoney(itemsTotal) }}</b></span>
        </div>
        <el-table :data="form.items" size="small" empty-text="暂无明细，请点击「添加商品」" class="bi-table">
          <el-table-column label="商品（全部未删除商品，已下架带后缀）" min-width="280">
            <template #default="{ row, $index }">
              <el-select v-model="row.productId" placeholder="选择商品" style="width: 100%">
                <el-option
                  v-for="p in optionsForRow($index)"
                  :key="p.id"
                  :value="p.id"
                  :label="`${p.name}（¥${Number(p.price).toFixed(2)}）${p.status === 'INACTIVE' ? '（已下架）' : ''}`"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="数量（1–99）" width="140" align="center">
            <template #default="{ row }">
              <el-input-number v-model="row.quantity" :min="1" :max="99" :step="1" :precision="0" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="小计" width="100" align="right">
            <template #default="{ row }">{{ formatMoney(productPrice(row.productId) * Number(row.quantity || 0)) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="70" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" @click="removeItem($index)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="bi-hint">仅展示前 {{ OPTION_SIZE }} 个商品；明细行不可重复选择同一商品</div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSubmit">
        {{ editingId ? '保存修改' : '创建套餐' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.w-full {
  width: 100%;
}

.bi-tools {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  margin-bottom: 10px;
}

.spacer {
  flex: 1;
}

.bi-total {
  font-size: 13px;
  color: #6b7280;
}

.bi-total b {
  color: #1f2937;
}

.bi-table {
  width: 100%;
}

.bi-hint {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 6px;
}
</style>
