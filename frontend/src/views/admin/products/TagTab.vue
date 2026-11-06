<script setup lang="ts">
/**
 * 标签管理子标签：列表（含停用、带引用商品数）+ 新增/改名/删除 + 启用停用开关。
 * 标签仅名称一个可编辑字段（全局唯一）；被商品引用时后端拒绝删除。
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

import { createAdminTag, deleteAdminTag, updateAdminTag, updateAdminTagStatus } from '@/api/admin/tag'
import type { AdminTagVO } from '@/types/admin'

const props = defineProps<{
  tags: AdminTagVO[]
  loading?: boolean
}>()

const emit = defineEmits<{ (e: 'refChanged'): void }>()

/* ---------------- 启用/停用开关（乐观更新，失败回滚） ---------------- */

const switchingId = ref<number | null>(null)

async function handleStatusChange(row: AdminTagVO) {
  const next = row.status // v-model 已写入目标状态
  switchingId.value = row.id
  try {
    await updateAdminTagStatus(row.id, next)
    ElMessage.success(next === 'ACTIVE' ? '标签已启用' : '标签已停用')
  } catch {
    row.status = next === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE' // 失败回滚
  } finally {
    switchingId.value = null
  }
}

/* ---------------- 删除 ---------------- */

async function handleDelete(row: AdminTagVO) {
  const confirmed = await ElMessageBox.confirm(
    '删除后不可恢复；被商品引用时后端将拒绝并提示引用数。',
    `删除标签「${row.name}」？`,
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消', confirmButtonClass: 'el-button--danger' },
  ).catch(() => false)
  if (!confirmed) return
  try {
    await deleteAdminTag(row.id)
    ElMessage.success('标签已删除')
    emit('refChanged') // 父组件刷新引用数据，列表随之更新
  } catch {
    // 后端拒绝（被引用）时拦截器已弹出报错文案
  }
}

/* ---------------- 新增/改名弹窗 ---------------- */

const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({ name: '' })

const rules: FormRules = {
  name: [
    { required: true, message: '请填写标签名称', trigger: 'blur' },
    { max: 64, message: '标签名称长度不能超过 64 字符', trigger: 'blur' },
  ],
}

function openCreate() {
  editingId.value = null
  form.name = ''
  dialogVisible.value = true
}

function openEdit(row: AdminTagVO) {
  editingId.value = row.id
  form.name = row.name
  dialogVisible.value = true
}

async function handleSubmit() {
  const formEl = formRef.value
  if (!formEl) return
  const valid = await formEl.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = { name: form.name.trim() }
    if (editingId.value) {
      await updateAdminTag(editingId.value, payload)
      ElMessage.success('标签已保存')
    } else {
      await createAdminTag(payload)
      ElMessage.success('标签已创建')
    }
    dialogVisible.value = false
    emit('refChanged') // 父组件刷新引用数据（商品表单的标签 chips 同步更新）
  } catch {
    // 保存失败（如名称重复 409）时保持弹窗打开，报错由拦截器弹出
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div>
    <div class="tab-toolbar">
      <el-button type="primary" :icon="Plus" @click="openCreate">新增标签</el-button>
      <span class="toolbar-tip">标签名全局唯一；被商品引用时不可删除（后端校验）</span>
    </div>

    <el-table v-loading="loading" :data="props.tags" empty-text="暂无标签，点击「新增标签」创建">
      <el-table-column label="标签名" min-width="180">
        <template #default="{ row }">
          <el-tag size="small" type="success">{{ row.name }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="引用商品数" width="110" align="center">
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
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/改名弹窗：仅名称一个字段 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑标签' : '新增标签'"
      width="420px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="handleSubmit">
        <el-form-item label="标签名称" prop="name">
          <el-input v-model="form.name" maxlength="64" placeholder="全局唯一，最长 64 字符" />
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

.dim {
  color: #c0c4cc;
}
</style>
