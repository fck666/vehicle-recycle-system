<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { ExternalTrim, MappingStatus, VehicleMappingRow } from '../api/vehicleMappings'
import { confirmVehicleMapping, listVehicleMappings, recomputeVehicleMapping, recomputeVehicleMappings, searchExternalTrims } from '../api/vehicleMappings'
import type { Page } from '../api/types'

const loading = ref(false)
const page = ref(0)
const size = ref(20)
const q = ref('')
const status = ref<'' | MappingStatus>('')
const result = ref<Page<VehicleMappingRow>>({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 })

const drawerVisible = ref(false)
const current = ref<VehicleMappingRow | null>(null)

const extLoading = ref(false)
const extQ = ref('')
const extPage = ref(0)
const extSize = ref(10)
const extResult = ref<Page<ExternalTrim>>({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 10 })

const statusOptions: Array<{ label: string; value: '' | MappingStatus }> = [
  { label: '全部', value: '' },
  { label: '未关联', value: 'UNMAPPED' },
  { label: '待确认', value: 'SUGGESTED' },
  { label: '已确认', value: 'CONFIRMED' },
]

const drawerTitle = computed(() => {
  if (!current.value) return '车型关联'
  return `车型关联 #${current.value.miitVehicleId} ${current.value.brand} ${current.value.model} ${current.value.modelYear}`
})

async function load() {
  loading.value = true
  try {
    result.value = await listVehicleMappings(q.value, status.value, page.value, size.value)
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function onSearch() {
  page.value = 0
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

async function openDrawer(row: VehicleMappingRow) {
  current.value = row
  drawerVisible.value = true
  extQ.value = `${row.brand} ${row.model}`
  extPage.value = 0
  await loadExternal()
}

async function doRecomputeRow() {
  if (!current.value) return
  try {
    await recomputeVehicleMapping(current.value.miitVehicleId)
    ElMessage.success('已生成候选')
    await load()
    current.value = result.value.content.find((x) => x.miitVehicleId === current.value?.miitVehicleId) ?? current.value
  } catch {
    ElMessage.error('生成失败')
  }
}

async function doConfirm(vehicleId: number, externalTrimId: number) {
  try {
    await confirmVehicleMapping(vehicleId, externalTrimId)
    ElMessage.success('已确认')
    await load()
    if (current.value && current.value.miitVehicleId === vehicleId) {
      current.value = result.value.content.find((x) => x.miitVehicleId === vehicleId) ?? current.value
    }
  } catch {
    ElMessage.error('确认失败')
  }
}

async function doRecomputeBatch() {
  try {
    await recomputeVehicleMappings(200)
    ElMessage.success('已触发批量生成')
    load()
  } catch {
    ElMessage.error('触发失败')
  }
}

async function loadExternal() {
  extLoading.value = true
  try {
    extResult.value = await searchExternalTrims(extQ.value, extPage.value, extSize.value)
  } catch {
    ElMessage.error('外部车型库加载失败')
  } finally {
    extLoading.value = false
  }
}

function onExtSearch() {
  extPage.value = 0
  loadExternal()
}

function onExtCurrentChange(v: number) {
  extPage.value = v - 1
  loadExternal()
}

load()
</script>

<template>
  <el-card>
    <template #header>
      <div style="display:flex;align-items:center;justify-content:space-between;gap:12px;">
        <div style="display:flex;align-items:center;gap:12px;">
          <el-input v-model="q" placeholder="搜索品牌/车型/产品ID/产品号" clearable @keyup.enter="onSearch" style="width:360px;" />
          <el-select v-model="status" style="width:140px;">
            <el-option v-for="o in statusOptions" :key="o.label" :label="o.label" :value="o.value" />
          </el-select>
          <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
          <el-button :loading="loading" @click="load">刷新</el-button>
        </div>
        <el-button type="primary" @click="doRecomputeBatch">批量生成候选</el-button>
      </div>
    </template>

    <el-table :data="result.content" v-loading="loading" stripe>
      <el-table-column prop="miitVehicleId" label="ID" width="90" />
      <el-table-column label="车型" min-width="260">
        <template #default="{ row }">
          <div style="font-weight:600;">{{ row.brand }} {{ row.model }} {{ row.modelYear }}</div>
          <div style="color:var(--el-text-color-secondary);font-size:12px;">
            fuel: {{ row.fuelType }} / battery: {{ row.batteryKwh ?? '-' }} / weight: {{ row.curbWeight ?? '-' }}
          </div>
          <div style="color:var(--el-text-color-secondary);font-size:12px;">
            productId: {{ row.productId || '-' }} / productNo: {{ row.productNo || '-' }}
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="110" />
      <el-table-column label="通用名称" min-width="320">
        <template #default="{ row }">
          <div v-if="row.externalMarketName">
            <div style="display:flex;align-items:center;gap:8px;">
              <el-image v-if="row.externalCoverUrl" :src="row.externalCoverUrl" style="width:72px;height:44px;object-fit:cover;border-radius:6px;" />
              <div>
                <div style="font-weight:600;">{{ row.externalMarketName }}</div>
                <el-link v-if="row.externalPageUrl" :href="row.externalPageUrl" target="_blank" :underline="false">打开来源页</el-link>
              </div>
            </div>
          </div>
          <div v-else style="color:var(--el-text-color-secondary);">
            -
          </div>
        </template>
      </el-table-column>
      <el-table-column label="候选" min-width="260">
        <template #default="{ row }">
          <div v-if="row.candidates && row.candidates.length">
            <div v-for="c in row.candidates.slice(0, 2)" :key="c.externalTrimId" style="display:flex;gap:8px;align-items:center;margin-bottom:6px;">
              <el-tag type="info">#{{ c.rankNo }} {{ c.score.toFixed(2) }}</el-tag>
              <span style="white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">{{ c.marketName || '-' }}</span>
            </div>
          </div>
          <div v-else style="color:var(--el-text-color-secondary);">-</div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="openDrawer(row)">关联</el-button>
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

  <el-drawer v-model="drawerVisible" :title="drawerTitle" size="980px">
    <div v-if="current">
      <div style="display:flex;gap:12px;align-items:center;margin-bottom:12px;">
        <el-button type="primary" @click="doRecomputeRow">重新生成候选</el-button>
        <div style="flex:1;" />
        <el-tag v-if="current.status === 'CONFIRMED'" type="success">已确认</el-tag>
        <el-tag v-else-if="current.status === 'SUGGESTED'" type="warning">待确认</el-tag>
        <el-tag v-else type="info">未关联</el-tag>
      </div>

      <el-card style="margin-bottom:12px;">
        <template #header>
          <div style="font-weight:600;">系统候选（Top 5）</div>
        </template>
        <el-table :data="current.candidates" stripe>
          <el-table-column label="排名" width="120">
            <template #default="{ row }">
              <el-tag type="info">#{{ row.rankNo }} {{ row.score.toFixed(2) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="名称" min-width="320">
            <template #default="{ row }">
              <div style="display:flex;gap:10px;align-items:center;">
                <el-image v-if="row.coverUrl" :src="row.coverUrl" style="width:88px;height:54px;object-fit:cover;border-radius:6px;" />
                <div>
                  <div style="font-weight:600;">{{ row.marketName || '-' }}</div>
                  <div style="color:var(--el-text-color-secondary);font-size:12px;">
                    {{ row.source }} / {{ row.seriesName || '-' }} / {{ row.modelYear || '-' }} / {{ row.energyType || '-' }}
                  </div>
                  <el-link v-if="row.pageUrl" :href="row.pageUrl" target="_blank" :underline="false">打开来源页</el-link>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" @click="doConfirm(current!.miitVehicleId, row.externalTrimId)">确认</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card>
        <template #header>
          <div style="display:flex;align-items:center;gap:12px;">
            <div style="font-weight:600;">外部车型库检索（手工兜底）</div>
            <div style="flex:1;" />
            <el-input v-model="extQ" placeholder="搜索外部库" clearable style="width:320px;" @keyup.enter="onExtSearch" />
            <el-button :loading="extLoading" @click="onExtSearch">搜索</el-button>
          </div>
        </template>
        <el-table :data="extResult.content" v-loading="extLoading" stripe>
          <el-table-column label="名称" min-width="360">
            <template #default="{ row }">
              <div style="display:flex;gap:10px;align-items:center;">
                <el-image v-if="row.coverUrl" :src="row.coverUrl" style="width:88px;height:54px;object-fit:cover;border-radius:6px;" />
                <div>
                  <div style="font-weight:600;">{{ row.marketName || '-' }}</div>
                  <div style="color:var(--el-text-color-secondary);font-size:12px;">
                    {{ row.source }} / {{ row.brand }} / {{ row.seriesName || '-' }} / {{ row.modelYear || '-' }} / {{ row.energyType || '-' }}
                  </div>
                  <el-link v-if="row.pageUrl" :href="row.pageUrl" target="_blank" :underline="false">打开来源页</el-link>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" @click="doConfirm(current!.miitVehicleId, row.id)">确认</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div style="display:flex;justify-content:flex-end;margin-top:12px;">
          <el-pagination
            :current-page="extPage + 1"
            :page-size="extSize"
            layout="total, prev, pager, next"
            :total="extResult.totalElements"
            @current-change="onExtCurrentChange"
          />
        </div>
      </el-card>
    </div>
  </el-drawer>
</template>
