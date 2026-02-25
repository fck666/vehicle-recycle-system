<script setup lang="ts">
import { ref } from 'vue'
import { listMaterialPrices, listMaterialTemplates } from '../api/material'
import { searchVehicles } from '../api/vehicles'

const loading = ref(false)
const vehicles = ref<number>(0)
const prices = ref<number>(0)
const templates = ref<number>(0)

async function refresh() {
  loading.value = true
  try {
    const v = await searchVehicles('', 0, 1)
    vehicles.value = v.totalElements
    const p = await listMaterialPrices()
    prices.value = p.length
    const t = await listMaterialTemplates()
    templates.value = t.length
  } finally {
    loading.value = false
  }
}

refresh()
</script>

<template>
  <el-card>
    <template #header>
      <div style="display:flex;align-items:center;justify-content:space-between;">
        <div>系统概览</div>
        <el-button :loading="loading" @click="refresh">刷新</el-button>
      </div>
    </template>
    <el-row :gutter="12">
      <el-col :span="8">
        <el-statistic title="车型数量" :value="vehicles" />
      </el-col>
      <el-col :span="8">
        <el-statistic title="材料价格条目" :value="prices" />
      </el-col>
      <el-col :span="8">
        <el-statistic title="估值方式(模板)" :value="templates" />
      </el-col>
    </el-row>
  </el-card>
</template>
