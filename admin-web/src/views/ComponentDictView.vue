<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import type { ComponentDict } from '../api/types'
import { listComponents, createComponent, updateComponent, deleteComponent } from '../api/components'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const canEdit = computed(() => (auth.me?.roles ?? []).some(r => ['ADMIN'].includes(r)))

const loading = ref(false)
const items = ref<ComponentDict[]>([])

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitting = ref(false)
const form = ref<Partial<ComponentDict>>({
  name: '',
  sortOrder: 0,
  isEnabled: true
})

async function load() {
  loading.value = true
  try {
    items.value = await listComponents()
  } catch (e: any) {
    ElMessage.error('加载失败: ' + e.message)
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  dialogMode.value = 'create'
  const maxSortOrder = items.value.length > 0 ? (items.value[items.value.length - 1]?.sortOrder ?? 0) : 0
  form.value = {
    name: '',
    sortOrder: maxSortOrder + 10,
    isEnabled: true
  }
  dialogVisible.value = true
}

function handleEdit(row: ComponentDict) {
  dialogMode.value = 'edit'
  form.value = { ...row }
  dialogVisible.value = true
}

async function handleDelete(row: ComponentDict) {
  try {
    await ElMessageBox.confirm(`确认删除组件 "${row.name}" 吗？`, '删除确认', { type: 'warning' })
    await deleteComponent(row.id)
    ElMessage.success('已删除')
    load()
  } catch {
    // cancelled
  }
}

async function handleSubmit() {
  if (!form.value.name?.trim()) {
    ElMessage.warning('请输入组件名称')
    return
  }
  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await createComponent(form.value)
      ElMessage.success('新增成功')
    } else {
      await updateComponent(form.value.id!, form.value)
      ElMessage.success('保存成功')
    }
    dialogVisible.value = false
    load()
  } catch (e: any) {
    ElMessage.error('保存失败: ' + e.message)
  } finally {
    submitting.value = false
  }
}

load()
</script>

<template>
  <el-card>
    <template #header>
      <div style="display:flex;align-items:center;justify-content:space-between;">
        <div>组件字典管理</div>
        <div style="display:flex;gap:12px;">
          <el-button v-if="canEdit" type="primary" :icon="Plus" @click="handleAdd">新增组件</el-button>
          <el-button :loading="loading" @click="load">刷新</el-button>
        </div>
      </div>
    </template>

    <el-alert 
      title="此处管理的组件列表，将作为小程序端“高价值部件”录入时的下拉选项。" 
      type="info" 
      show-icon 
      style="margin-bottom:12px;" 
      :closable="false"
    />

    <el-table :data="items" v-loading="loading" stripe border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="组件名称" min-width="150" />
      <el-table-column prop="sortOrder" label="排序" width="100" />
      <el-table-column prop="isEnabled" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.isEnabled ? 'success' : 'info'">{{ row.isEnabled ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column v-if="canEdit" label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增组件' : '编辑组件'" width="400px">
      <el-form label-width="80px" @submit.prevent="handleSubmit">
        <el-form-item label="组件名称" required>
          <el-input v-model="form.name" placeholder="请输入组件名称" />
        </el-form-item>
        <el-form-item label="排序值">
          <el-input-number v-model="form.sortOrder" :min="0" :step="10" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="是否启用">
          <el-switch v-model="form.isEnabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            确认
          </el-button>
        </span>
      </template>
    </el-dialog>
  </el-card>
</template>