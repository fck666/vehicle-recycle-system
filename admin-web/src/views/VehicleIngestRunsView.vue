<script setup lang="ts">
import { ref } from 'vue'
import type { JobRun } from '../api/jobRuns'
import { listJobRuns } from '../api/jobRuns'
import type { Page } from '../api/types'

const loading = ref(false)
const page = ref(0)
const size = ref(20)
const specRuns = ref<Page<JobRun>>({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 })
const docRuns = ref<Page<JobRun>>({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 })

async function load() {
  loading.value = true
  try {
    const [a, b] = await Promise.all([
      listJobRuns('MIIT_VEHICLE_SPECS_UPSERT', page.value, size.value),
      listJobRuns('MIIT_VEHICLE_DOCS_UPSERT', page.value, size.value),
    ])
    specRuns.value = a
    docRuns.value = b
  } finally {
    loading.value = false
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
        <div>车型抓取入库记录（来自 Python MIIT 工具 upsert）</div>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
    </template>

    <el-tabs>
      <el-tab-pane label="车型参数 Upsert">
        <el-table :data="specRuns.content" v-loading="loading" stripe>
          <el-table-column prop="status" label="状态" width="110" />
          <el-table-column prop="startedAt" label="时间" width="190" />
          <el-table-column prop="insertedCount" label="新增" width="80" />
          <el-table-column prop="updatedCount" label="更新" width="80" />
          <el-table-column prop="skippedCount" label="跳过" width="80" />
          <el-table-column prop="actorName" label="写入者" width="140" />
          <el-table-column prop="runId" label="RunId" min-width="220" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="车型文档 Upsert">
        <el-table :data="docRuns.content" v-loading="loading" stripe>
          <el-table-column prop="status" label="状态" width="110" />
          <el-table-column prop="startedAt" label="时间" width="190" />
          <el-table-column prop="insertedCount" label="新增" width="80" />
          <el-table-column prop="updatedCount" label="更新" width="80" />
          <el-table-column prop="skippedCount" label="跳过" width="80" />
          <el-table-column prop="actorName" label="写入者" width="140" />
          <el-table-column prop="runId" label="RunId" min-width="220" />
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <div style="display:flex;justify-content:flex-end;margin-top:12px;">
      <el-pagination
        :current-page="page + 1"
        :page-size="size"
        :page-sizes="[10,20,50,100,200]"
        layout="total, sizes, prev, pager, next"
        :total="Math.max(specRuns.totalElements, docRuns.totalElements)"
        @size-change="onSizeChange"
        @current-change="onCurrentChange"
      />
    </div>
  </el-card>
</template>

