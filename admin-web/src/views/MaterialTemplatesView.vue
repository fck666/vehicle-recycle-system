<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { MaterialTemplate } from '../api/types'
import { deleteMaterialTemplate, listMaterialTemplates, upsertMaterialTemplate } from '../api/material'

const loading = ref(false)
const items = ref<MaterialTemplate[]>([])

const dialogVisible = ref(false)
const form = reactive<{
  vehicleType: string
  steelRatio: number | null
  aluminumRatio: number | null
  copperRatio: number | null
  recoveryRatio: number | null
}>({ vehicleType: '', steelRatio: null, aluminumRatio: null, copperRatio: null, recoveryRatio: null })

async function load() {
  loading.value = true
  try {
    items.value = await listMaterialTemplates()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.vehicleType = ''
  form.steelRatio = null
  form.aluminumRatio = null
  form.copperRatio = null
  form.recoveryRatio = null
  dialogVisible.value = true
}

function openEdit(row: MaterialTemplate) {
  form.vehicleType = row.vehicleType
  form.steelRatio = row.steelRatio
  form.aluminumRatio = row.aluminumRatio
  form.copperRatio = row.copperRatio
  form.recoveryRatio = row.recoveryRatio
  dialogVisible.value = true
}

async function submit() {
  if (
    !form.vehicleType.trim() ||
    form.steelRatio == null ||
    form.aluminumRatio == null ||
    form.copperRatio == null ||
    form.recoveryRatio == null
  ) {
    return
  }
  await upsertMaterialTemplate({
    vehicleType: form.vehicleType.trim(),
    steelRatio: form.steelRatio,
    aluminumRatio: form.aluminumRatio,
    copperRatio: form.copperRatio,
    recoveryRatio: form.recoveryRatio,
  })
  ElMessage.success('已保存')
  dialogVisible.value = false
  load()
}

async function onDelete(row: MaterialTemplate) {
  await ElMessageBox.confirm(`确认删除模板 ${row.vehicleType} 吗？`, '删除确认', { type: 'warning' })
  await deleteMaterialTemplate(row.vehicleType)
  ElMessage.success('已删除')
  load()
}

load()
</script>

<template>
  <el-card>
    <template #header>
      <div style="display:flex;align-items:center;justify-content:space-between;">
        <div>估值方式（材料配比模板）</div>
        <div style="display:flex;gap:8px;">
          <el-button :loading="loading" @click="load">刷新</el-button>
          <el-button type="primary" @click="openCreate">新增模板</el-button>
        </div>
      </div>
    </template>

    <el-table :data="items" v-loading="loading" stripe>
      <el-table-column prop="vehicleType" label="车型类型" width="160" />
      <el-table-column prop="steelRatio" label="钢占比" width="120" />
      <el-table-column prop="aluminumRatio" label="铝占比" width="120" />
      <el-table-column prop="copperRatio" label="铜占比" width="120" />
      <el-table-column prop="recoveryRatio" label="回收系数" width="120" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="dialogVisible" title="模板编辑" width="560px">
    <el-form label-width="110px">
      <el-form-item label="车型类型" required>
        <el-input v-model="form.vehicleType" />
      </el-form-item>
      <el-form-item label="钢占比" required>
        <el-input-number v-model="form.steelRatio" :min="0" :max="1" :precision="4" :step="0.01" />
      </el-form-item>
      <el-form-item label="铝占比" required>
        <el-input-number v-model="form.aluminumRatio" :min="0" :max="1" :precision="4" :step="0.01" />
      </el-form-item>
      <el-form-item label="铜占比" required>
        <el-input-number v-model="form.copperRatio" :min="0" :max="1" :precision="4" :step="0.01" />
      </el-form-item>
      <el-form-item label="回收系数" required>
        <el-input-number v-model="form.recoveryRatio" :min="0" :max="1" :precision="4" :step="0.01" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>
