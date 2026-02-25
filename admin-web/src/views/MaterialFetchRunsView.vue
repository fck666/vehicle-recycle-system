<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { JobRun } from '../api/jobRuns'
import { listJobRuns, runMaterialPriceNow } from '../api/jobRuns'
import type { Page } from '../api/types'

const loading = ref(false)
const running = ref(false)
const page = ref(0)
const size = ref(20)
const result = ref<Page<JobRun>>({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 })

async function load() {
  loading.value = true
  try {
    result.value = await listJobRuns('MATERIAL_PRICE_FETCH', page.value, size.value)
  } finally {
    loading.value = false
  }
}

async function runNow() {
  running.value = true
  try {
    await runMaterialPriceNow()
    ElMessage.success('已触发抓取')
    load()
  } catch {
    ElMessage.error('触发失败')
  } finally {
    running.value = false
  }
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
      <div style="display:flex;align-items:center;justify-content:space-between;">
        <div>材料抓取（后端定时/手动触发）</div>
        <div style="display:flex;gap:8px;">
          <el-button :loading="loading" @click="load">刷新</el-button>
          <el-button type="primary" :loading="running" @click="runNow">立即抓取</el-button>
        </div>
      </div>
    </template>

    <el-table :data="result.content" v-loading="loading" stripe>
      <el-table-column prop="status" label="状态" width="110" />
      <el-table-column prop="startedAt" label="开始时间" width="190" />
      <el-table-column prop="finishedAt" label="结束时间" width="190" />
      <el-table-column prop="insertedCount" label="新增" width="80" />
      <el-table-column prop="updatedCount" label="更新" width="80" />
      <el-table-column prop="skippedCount" label="失败" width="80" />
      <el-table-column prop="message" label="备注" min-width="160" />
      <el-table-column prop="actorName" label="触发者" width="140" />
      <el-table-column prop="runId" label="RunId" min-width="220" />
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
</template>

