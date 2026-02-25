<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Page, VehicleModel, VehicleUpsertRequest } from '../api/types'
import { createVehicle, deleteVehicle, searchVehicles, updateVehicle } from '../api/vehicles'

const q = ref('')
const loading = ref(false)
const page = ref(0)
const size = ref(20)
const result = ref<Page<VehicleModel>>({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 })

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const currentId = ref<number | null>(null)

const form = reactive<VehicleUpsertRequest>({
  brand: '',
  model: '',
  modelYear: new Date().getFullYear(),
  fuelType: '',
  vehicleType: '',
  curbWeight: undefined,
  batteryKwh: null,
  productId: '',
  productNo: '',
})

const title = computed(() => (dialogMode.value === 'create' ? '新增车型' : '编辑车型'))

async function load() {
  loading.value = true
  try {
    const res = await searchVehicles(q.value, page.value, size.value)
    result.value = res
  } finally {
    loading.value = false
  }
}

function onSearch() {
  page.value = 0
  load()
}

function openCreate() {
  dialogMode.value = 'create'
  currentId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: VehicleModel) {
  dialogMode.value = 'edit'
  currentId.value = row.id
  resetForm()
  form.brand = row.brand
  form.model = row.model
  form.modelYear = row.modelYear
  form.fuelType = row.fuelType
  form.vehicleType = row.vehicleType
  form.curbWeight = row.curbWeight
  form.batteryKwh = row.batteryKwh ?? null
  form.productId = row.productId ?? ''
  form.productNo = row.productNo ?? ''
  dialogVisible.value = true
}

function resetForm() {
  form.brand = ''
  form.model = ''
  form.modelYear = new Date().getFullYear()
  form.fuelType = ''
  form.vehicleType = ''
  form.curbWeight = undefined
  form.batteryKwh = null
  form.productId = ''
  form.productNo = ''
}

async function submit() {
  const payload: VehicleUpsertRequest = {
    brand: form.brand,
    model: form.model,
    modelYear: form.modelYear,
    fuelType: form.fuelType,
    vehicleType: form.vehicleType,
    curbWeight: form.curbWeight,
    batteryKwh: form.batteryKwh ?? null,
    productId: form.productId ? form.productId : null,
    productNo: form.productNo ? form.productNo : null,
  }

  try {
    if (dialogMode.value === 'create') {
      await createVehicle(payload)
      ElMessage.success('已创建')
    } else {
      if (currentId.value == null) return
      await updateVehicle(currentId.value, payload)
      ElMessage.success('已更新')
    }
    dialogVisible.value = false
    load()
  } catch (e: any) {
    ElMessage.error('操作失败')
    throw e
  }
}

async function onDelete(row: VehicleModel) {
  await ElMessageBox.confirm(`确认删除车型 #${row.id} 吗？`, '删除确认', { type: 'warning' })
  await deleteVehicle(row.id)
  ElMessage.success('已删除')
  load()
}

function onSizeChange(v: number) {
  size.value = v
  page.value = 0
  load()
}

function onCurrentChange(v: number) {
  page.value = v - 1
  load()
}

load()
</script>

<template>
  <el-card>
    <template #header>
      <div style="display:flex;align-items:center;justify-content:space-between;gap:12px;">
        <div style="display:flex;align-items:center;gap:12px;">
          <el-input v-model="q" placeholder="搜索 brand/model/productId/productNo/vehicleType/fuelType" clearable @keyup.enter="onSearch" style="width:420px;" />
          <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
        </div>
        <el-button type="primary" @click="openCreate">新增车型</el-button>
      </div>
    </template>

    <el-table :data="result.content" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="brand" label="品牌" width="140" />
      <el-table-column prop="model" label="车型" width="180" />
      <el-table-column prop="modelYear" label="年份" width="100" />
      <el-table-column prop="fuelType" label="燃料" width="120" />
      <el-table-column prop="vehicleType" label="类型" width="140" />
      <el-table-column prop="curbWeight" label="整备质量(kg)" width="140" />
      <el-table-column prop="batteryKwh" label="电池(kWh)" width="120" />
      <el-table-column prop="productId" label="产品ID" width="140" />
      <el-table-column prop="productNo" label="产品号" width="160" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="display:flex;justify-content:flex-end;margin-top:12px;">
      <el-pagination
        :current-page="page + 1"
        :page-size="size"
        :page-sizes="[10,20,50,100,200]"
        layout="total, sizes, prev, pager, next"
        :total="result.totalElements"
        @size-change="onSizeChange"
        @current-change="onCurrentChange"
      />
    </div>
  </el-card>

  <el-dialog v-model="dialogVisible" :title="title" width="680px">
    <el-form label-width="110px">
      <el-form-item label="品牌" required>
        <el-input v-model="form.brand" />
      </el-form-item>
      <el-form-item label="车型" required>
        <el-input v-model="form.model" />
      </el-form-item>
      <el-form-item label="年份" required>
        <el-input-number v-model="form.modelYear" :min="1900" :max="2100" />
      </el-form-item>
      <el-form-item label="燃料类型" required>
        <el-input v-model="form.fuelType" placeholder="gas / ev / hybrid ..." />
      </el-form-item>
      <el-form-item label="车型类型" required>
        <el-input v-model="form.vehicleType" placeholder="sedan / ev_sedan ..." />
      </el-form-item>
      <el-form-item label="整备质量kg" required>
        <el-input-number v-model="form.curbWeight" :min="1" :precision="2" :step="10" style="width:220px;" />
      </el-form-item>
      <el-form-item label="电池kWh">
        <el-input-number v-model="form.batteryKwh" :min="0" :precision="2" :step="1" style="width:220px;" />
      </el-form-item>
      <el-form-item label="产品ID">
        <el-input v-model="form.productId" />
      </el-form-item>
      <el-form-item label="产品号">
        <el-input v-model="form.productNo" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>
