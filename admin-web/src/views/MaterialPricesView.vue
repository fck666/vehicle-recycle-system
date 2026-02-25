<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { MaterialPrice } from '../api/types'
import { listMaterialPrices, upsertMaterialPrice } from '../api/material'

const loading = ref(false)
const items = ref<MaterialPrice[]>([])

const dialogVisible = ref(false)
const form = reactive<{ type: string; pricePerKg: number | null }>({ type: '', pricePerKg: null })

async function load() {
  loading.value = true
  try {
    items.value = await listMaterialPrices()
  } finally {
    loading.value = false
  }
}

function openEdit(row: MaterialPrice) {
  form.type = row.type
  form.pricePerKg = row.pricePerKg
  dialogVisible.value = true
}

async function submit() {
  if (!form.type.trim() || form.pricePerKg == null) return
  await upsertMaterialPrice({ type: form.type.trim(), pricePerKg: form.pricePerKg })
  ElMessage.success('已保存')
  dialogVisible.value = false
  load()
}

load()
</script>

<template>
  <el-card>
    <template #header>
      <div style="display:flex;align-items:center;justify-content:space-between;">
        <div>材料价格</div>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
    </template>
    <el-table :data="items" v-loading="loading" stripe>
      <el-table-column prop="type" label="类型" width="140" />
      <el-table-column prop="pricePerKg" label="价格(元/kg)" width="140" />
      <el-table-column prop="effectiveDate" label="生效日期" width="140" />
      <el-table-column prop="fetchedAt" label="抓取时间" width="180" />
      <el-table-column prop="sourceName" label="来源" min-width="160" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="dialogVisible" title="编辑材料价格" width="520px">
    <el-form label-width="110px">
      <el-form-item label="类型">
        <el-input v-model="form.type" disabled />
      </el-form-item>
      <el-form-item label="价格(元/kg)" required>
        <el-input-number v-model="form.pricePerKg" :min="0" :precision="2" :step="0.1" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>
