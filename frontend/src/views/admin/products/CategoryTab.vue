<script setup lang="ts">
/**
 * 分类管理子标签：列表（含停用、带引用商品数）+ 新增/编辑/删除 + 启用停用开关。
 * 列表数据直接来自父组件的引用数据（对齐旧版 renderCatsCard 读 refData.categories）；
 * 编码创建后不可修改（编辑时输入框禁用），系统内置 all 不可删除。
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

import {
  createAdminCategory,
  deleteAdminCategory,
  updateAdminCategory,
  updateAdminCategoryStatus,
} from '@/api/admin/category'
import type { AdminCategoryVO } from '@/types/admin'

const props = defineProps<{
  categories: AdminCategoryVO[]
  loading?: boolean
}>()

const emit = defineEmits<{ (e: 'refChanged'): void }>()

/* ---------------- 启用/停用开关（乐观更新，失败回滚） ---------------- */

const switchingId = ref<number | null>(null)

async function handleStatusChange(row: AdminCategoryVO) {
  const next = row.status // v-model 已写入目标状态
  switchingId.value = row.id
  try {
    await updateAdminCategoryStatus(row.id, next)
    ElMessage.success(next === 'ACTIVE' ? '分类已启用' : '分类已停用，C 端导航将不再展示')
  } catch {
    row.status = next === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE' // 失败回滚
  } finally {
    switchingId.value = null
  }
}

/* ---------------- 删除（系统内置 all 禁用删除按钮） ---------------- */

async function handleDelete(row: AdminCategoryVO) {
  const confirmed = await ElMessageBox.confirm(
    '删除后不可恢复；被商品引用时后端将拒绝并提示引用数。',
    `删除分类「${row.name}」？`,
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消', confirmButtonClass: 'el-button--danger' },
  ).catch(() => false)
  if (!confirmed) return
  try {
    await deleteAdminCategory(row.id)
    ElMessage.success('分类已删除')
    emit('refChanged') // 父组件刷新引用数据，列表随之更新
  } catch {
    // 后端拒绝（内置分类/被引用）时拦截器已弹出报错文案
  }
}

/* ---------------- 新增/编辑弹窗 ---------------- */

interface CategoryForm {
  code: string
  name: string
  description: string
  sortOrder: number | null
}

const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<CategoryForm>({ code: '', name: '', description: '', sortOrder: 0 })

/** 编码：仅新增可填，小写字母开头 + 小写字母/数字/连字符，最长 40 字符，不允许 all */
const validateCode = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  const code = value.trim().toLowerCase()
  if (!code) return callback(new Error('请填写分类编码'))
  if (code === 'all') return callback(new Error('all 为系统内置分类编码，不允许使用'))
  if (!/^[a-z][a-z0-9-]{0,39}$/.test(code)) {
    return callback(
      new Error('编码需以小写字母开头，仅可含小写字母、数字与连字符，最长 40 字符'),
    )
  }
  callback()
}

/** 排序号：可空（缺省 0），填写须为整数 */
const validateSortOrder = (_rule: unknown, value: number | null, callback: (error?: Error) => void) => {
  if (value == null) return callback() // 留空按 0 提交
  if (!/^-?\d+$/.test(String(value))) return callback(new Error('排序号须为整数'))
  callback()
}

const rules: FormRules = {
  code: [{ required: true, validator: validateCode, trigger: 'blur' }],
  name: [
    { required: true, message: '请填写分类名称', trigger: 'blur' },
    { max: 64, message: '分类名称长度不能超过 64 字符', trigger: 'blur' },
  ],
  description: [{ max: 255, message: '分类描述长度不能超过 255 字符', trigger: 'blur' }],
  sortOrder: [{ validator: validateSortOrder, trigger: 'blur' }],
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { code: '', name: '', description: '', sortOrder: 0 })
  dialogVisible.value = true
}

function openEdit(row: AdminCategoryVO) {
  editingId.value = row.id
  Object.assign(form, {
    code: row.code, // 仅回显，编辑时禁用（编码创建后不可修改）
    name: row.name,
    description: row.description ?? '',
    sortOrder: row.sortOrder ?? 0,
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  const formEl = formRef.value
  if (!formEl) return
  const valid = await formEl.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editingId.value) {
      // 编辑：编码不可改，不传 code（后端视为不变）
      await updateAdminCategory(editingId.value, {
        name: form.name.trim(),
        description: form.description.trim(),
        sortOrder: form.sortOrder == null ? 0 : Number(form.sortOrder),
      })
      ElMessage.success('分类已保存')
    } else {
      await createAdminCategory({
        code: form.code.trim().toLowerCase(),
        name: form.name.trim(),
        description: form.description.trim(),
        sortOrder: form.sortOrder == null ? 0 : Number(form.sortOrder),
      })
      ElMessage.success('分类已创建')
    }
    dialogVisible.value = false
    emit('refChanged') // 父组件刷新引用数据（商品表单的分类 chips 同步更新）
  } catch {
    // 保存失败（如编码重复 409）时保持弹窗打开，报错由拦截器弹出
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div>
    <div class="tab-toolbar">
      <el-button type="primary" :icon="Plus" @click="openCreate">新增分类</el-button>
      <span class="toolbar-tip">编码创建后不可修改；「全部」为系统内置分类</span>
    </div>

    <el-table
      v-loading="loading"
      :data="props.categories"
      empty-text="暂无分类，点击「新增分类」创建"
    >
      <el-table-column prop="sortOrder" label="排序" width="70" align="center" />
      <el-table-column label="编码" min-width="140">
        <template #default="{ row }">
          <span class="mono">{{ row.code }}</span>
          <el-tag v-if="row.code === 'all'" size="small" type="info" class="pill">内置</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="名称" min-width="120" />
      <el-table-column label="描述" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          <span :class="row.description ? '' : 'dim'">{{ row.description || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="引用商品" width="90" align="center">
        <template #default="{ row }">
          <span v-if="row.productCount > 0">{{ row.productCount }}</span>
          <span v-else class="dim">—</span>
        </template>
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
            @change="handleStatusChange(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-tooltip
            v-if="row.code === 'all'"
            content="系统内置分类不可删除"
            placement="top"
          >
            <span class="disabled-btn"><el-button link disabled>删除</el-button></span>
          </el-tooltip>
          <el-button v-else link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑弹窗：编码仅新增可填（编辑禁用） -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑分类' : '新增分类'"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="分类编码" prop="code">
          <el-input
            v-model="form.code"
            :disabled="!!editingId"
            maxlength="40"
            :placeholder="editingId ? '编码创建后不可修改' : '以小写字母开头，仅可含小写字母、数字与连字符，最长 40 字符，唯一'"
          />
        </el-form-item>
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" maxlength="64" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" maxlength="255" />
        </el-form-item>
        <el-form-item label="排序号（缺省 0）" prop="sortOrder">
          <el-input-number
            v-model="form.sortOrder"
            :controls="false"
            :precision="0"
            placeholder="数值越小越靠前"
            class="w-full"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.tab-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.toolbar-tip {
  font-size: 12px;
  color: #9ca3af;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  color: #4b5563;
  margin-right: 6px;
}

.pill {
  margin-left: 2px;
}

.dim {
  color: #c0c4cc;
}

/* el-button disabled 时 tooltip 不触发，包一层 span 接管鼠标事件 */
.disabled-btn {
  margin-left: 8px;
  display: inline-block;
}

.w-full {
  width: 100%;
}
</style>
