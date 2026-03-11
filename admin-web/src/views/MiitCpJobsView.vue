<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { JobRun } from '../api/jobRuns'
import { createMiitCpJob, listMiitCpJobs, retryMiitCpJob } from '../api/miitCpJobs'
import type { Page } from '../api/types'
import { useAuthStore } from '../stores/auth'

const loading = ref(false)
const page = ref(0)
const size = ref(20)
const result = ref<Page<JobRun>>({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 })
const detailDialogVisible = ref(false)
const currentJob = ref<JobRun | null>(null)
const failedItems = ref<any[]>([])
const auth = useAuthStore()
const miitFormStoragePrefix = 'admin_miit_cp_form'

const form = reactive({
  pcFrom: 398,
  pcTo: 398,
  cpsb: '',
  clxh: '',
  clmc: '',
  pageSize: 10,
  limit: 200,
  headful: true,
  qymcListText: '大众,奥迪',
})

const workerCmd = computed(() => {
  return `python -m miit_vehicle_crawler.cli worker --backend http://localhost:8090 --token <ADMIN_TOKEN>`
})

function getMiitFormStorageKey() {
  return `${miitFormStoragePrefix}_${auth.me?.userId ?? 'anonymous'}`
}

function restoreForm() {
  const raw = localStorage.getItem(getMiitFormStorageKey())
  if (!raw) return
  try {
    const saved = JSON.parse(raw) as Partial<typeof form>
    if (typeof saved.pcFrom === 'number') form.pcFrom = saved.pcFrom
    if (typeof saved.pcTo === 'number') form.pcTo = saved.pcTo
    if (typeof saved.cpsb === 'string') form.cpsb = saved.cpsb
    if (typeof saved.clxh === 'string') form.clxh = saved.clxh
    if (typeof saved.clmc === 'string') form.clmc = saved.clmc
    if (typeof saved.pageSize === 'number') form.pageSize = saved.pageSize
    if (typeof saved.limit === 'number') form.limit = saved.limit
    if (typeof saved.headful === 'boolean') form.headful = saved.headful
    if (typeof saved.qymcListText === 'string') form.qymcListText = saved.qymcListText
  } catch {
  }
}

function persistForm() {
  localStorage.setItem(
    getMiitFormStorageKey(),
    JSON.stringify({
      pcFrom: form.pcFrom,
      pcTo: form.pcTo,
      cpsb: form.cpsb,
      clxh: form.clxh,
      clmc: form.clmc,
      pageSize: form.pageSize,
      limit: form.limit,
      headful: form.headful,
      qymcListText: form.qymcListText,
    })
  )
}

function parseQymcList() {
  if (!form.qymcListText) return []
  return form.qymcListText.split(/[,，]/).map(s => s.trim()).filter(s => !!s)
}

async function load() {
  loading.value = true
  try {
    result.value = await listMiitCpJobs(page.value, size.value)
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

async function createJob() {
  if (form.pcFrom > form.pcTo) {
    ElMessage.warning('批次起始不能大于结束')
    return
  }
  const qymcList = parseQymcList()
  if (!qymcList.length && !form.cpsb.trim() && !form.clxh.trim() && !form.clmc.trim()) {
    ElMessage.warning('至少提供一个查询条件（长度不少于2）')
    return
  }
  persistForm()
  try {
    await createMiitCpJob({
      pcFrom: form.pcFrom,
      pcTo: form.pcTo,
      cpsb: form.cpsb.trim() || null,
      qymcList: qymcList.length ? qymcList : null,
      clxh: form.clxh.trim() || null,
      clmc: form.clmc.trim() || null,
      pageSize: form.pageSize || null,
      limit: form.limit || null,
      headful: !!form.headful,
    })
    ElMessage.success('已创建任务')
    load()
  } catch {
    ElMessage.error('创建失败')
  }
}

async function retryJob(runId: string) {
  try {
    await ElMessageBox.confirm('确定要对失败记录进行重试抓取吗？', '提示', { type: 'warning' })
    await retryMiitCpJob(runId)
    ElMessage.success('已创建重试任务')
    load()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('重试失败')
  }
}

function showDetail(row: JobRun) {
  currentJob.value = row
  failedItems.value = []
  try {
    if (row.detailsJson) {
      const detail = JSON.parse(row.detailsJson)
      if (detail?.progress?.result?.failed_items) {
        failedItems.value = detail.progress.result.failed_items
      }
    }
  } catch (e) {
    console.error(e)
  }
  detailDialogVisible.value = true
}

function getFailedCount(row: JobRun) {
  try {
    if (row.detailsJson) {
      const detail = JSON.parse(row.detailsJson)
      return detail?.progress?.result?.failed || 0
    }
  } catch {
    return 0
  }
  return 0
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

function formatStatus(s: string) {
  if (s === 'PENDING') return '待执行'
  if (s === 'RUNNING') return '执行中'
  if (s === 'SUCCESS') return '成功'
  if (s === 'FAILED') return '失败'
  return s
}

watch(
  () => auth.me?.userId,
  () => {
    restoreForm()
  },
  { immediate: true }
)

load()
</script>

<template>
  <el-card style="margin-bottom:12px;">
    <template #header>
      <div style="font-weight:600;">创建工信部抓取任务（本机执行）</div>
    </template>
    <el-form class="miit-job-form" label-width="160px">
      <el-form-item label="批次范围" required>
        <div style="display:flex;gap:10px;align-items:center;">
          <el-input-number v-model="form.pcFrom" :min="1" :max="9999" />
          <span style="color:var(--el-text-color-secondary);">到</span>
          <el-input-number v-model="form.pcTo" :min="1" :max="9999" />
        </div>
      </el-form-item>
      <el-form-item label="企业名称列表">
        <el-input v-model="form.qymcListText" placeholder="逗号分隔，支持简称模糊匹配，例如：大众,奥迪,蔚来" />
      </el-form-item>
      <el-form-item label="产品商标(cpsb)">
        <el-input v-model="form.cpsb" placeholder="例如：大众牌、奥迪(AUDI)牌" />
      </el-form-item>
      <el-form-item label="车辆型号(clxh)">
        <el-input v-model="form.clxh" placeholder="例如：FV6506" />
      </el-form-item>
      <el-form-item label="车辆名称(clmc)">
        <el-input v-model="form.clmc" placeholder="例如：轿车、多用途乘用车" />
      </el-form-item>
      <el-form-item label="每页条数">
        <el-input-number v-model="form.pageSize" :min="1" :max="50" />
      </el-form-item>
      <el-form-item label="最多处理条数">
        <el-input-number v-model="form.limit" :min="1" :max="5000" />
      </el-form-item>
      <el-form-item label="需要可视浏览器">
        <el-switch v-model="form.headful" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="createJob">创建任务</el-button>
        <el-button @click="load">刷新列表</el-button>
      </el-form-item>
    </el-form>

    <el-alert type="info" :closable="false" style="margin-top:8px;">
      <template #title>执行说明</template>
      <div style="font-size:12px;">
        该任务不会在服务器上自动跑，需要在你的本机启动 worker 轮询执行；遇到“访问行为验证（滑块拼图）”时，worker 会打开浏览器窗口等待你手动通过。
      </div>
      <div style="margin-top:8px;font-family:ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;font-size:12px;">
        {{ workerCmd }}
      </div>
    </el-alert>
  </el-card>

  <el-card>
    <template #header>
      <div style="display:flex;align-items:center;justify-content:space-between;">
        <div style="font-weight:600;">任务列表</div>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
    </template>
    <el-table :data="result.content" v-loading="loading" stripe>
      <el-table-column prop="startedAt" label="创建时间" width="180" />
      <el-table-column prop="runId" label="runId" min-width="220" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag v-if="row.status === 'SUCCESS'" type="success">{{ formatStatus(row.status) }}</el-tag>
          <el-tag v-else-if="row.status === 'FAILED'" type="danger">{{ formatStatus(row.status) }}</el-tag>
          <el-tag v-else-if="row.status === 'RUNNING'" type="warning">{{ formatStatus(row.status) }}</el-tag>
          <el-tag v-else type="info">{{ formatStatus(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="计数" width="280">
        <template #default="{ row }">
          <span>ins: {{ row.insertedCount ?? 0 }}</span>
          <span style="margin-left:10px;">upd: {{ row.updatedCount ?? 0 }}</span>
          <span style="margin-left:10px;">skip: {{ row.skippedCount ?? 0 }}</span>
          <span style="margin-left:10px;color:red;" v-if="getFailedCount(row) > 0">fail: {{ getFailedCount(row) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="message" label="消息" min-width="220" />
      <el-table-column prop="finishedAt" label="结束时间" width="180" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="showDetail(row)">详情</el-button>
          <el-button 
            v-if="row.status !== 'RUNNING' && row.status !== 'PENDING' && getFailedCount(row) > 0" 
            link 
            type="warning" 
            @click="retryJob(row.runId)"
          >重试失败</el-button>
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

  <el-dialog
    v-model="detailDialogVisible"
    title="任务详情"
    width="800px"
  >
    <div v-if="currentJob">
      <h3>失败记录 ({{ failedItems.length }})</h3>
      <el-table :data="failedItems" height="400" border stripe v-if="failedItems.length">
        <el-table-column prop="cpid" label="产品ID" width="120" />
        <el-table-column prop="clxh" label="产品型号" width="150" />
        <el-table-column prop="cpsb" label="商标" width="120" />
        <el-table-column prop="pc" label="批次" width="80" />
        <el-table-column prop="error" label="错误信息" min-width="200" show-overflow-tooltip />
      </el-table>
      <el-empty v-else description="无失败记录" />
      
      <div style="margin-top:20px;">
        <h3>原始配置</h3>
        <pre style="background:#f5f7fa;padding:10px;border-radius:4px;overflow:auto;">{{ JSON.stringify(JSON.parse(currentJob.detailsJson || '{}').config, null, 2) }}</pre>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped>
.miit-job-form :deep(.el-form-item__label) {
  white-space: nowrap;
}
</style>
