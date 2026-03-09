<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useAuthStore } from '../stores/auth'
import { ElMessage } from 'element-plus'
import type { MaterialPrice, MaterialSourceConfig, MaterialSourceSuggestResult } from '../api/types'
import { getMaterialPriceHistory, listMaterialPrices, listMaterialSources, suggestMaterialSources, upsertMaterialPrice, upsertMaterialSource } from '../api/material'

const loading = ref(false)
const items = ref<MaterialPrice[]>([])
const sourceItems = ref<MaterialSourceConfig[]>([])

const dialogVisible = ref(false)
const form = reactive<{ type: string; pricePerKg: number | null; effectiveDate: string }>({ type: '', pricePerKg: null, effectiveDate: '' })

const historyVisible = ref(false)
const historyLoading = ref(false)
const historyType = ref('')
const historyFrom = ref('')
const historyTo = ref('')
const historyItems = ref<MaterialPrice[]>([])
const suggestKeyword = ref('')
const suggestLoading = ref(false)
const suggestItems = ref<MaterialSourceSuggestResult[]>([])

const auth = useAuthStore()
const canEdit = computed(() => (auth.me?.roles ?? []).some(r => ['ADMIN', 'OPERATOR'].includes(r)))

async function load() {
  loading.value = true
  try {
    const [prices, sources] = await Promise.all([listMaterialPrices(), listMaterialSources()])
    items.value = prices
    sourceItems.value = sources
  } finally {
    loading.value = false
  }
}

function openEdit(row: MaterialPrice) {
  form.type = row.type
  form.pricePerKg = row.pricePerKg
  form.effectiveDate = row.effectiveDate ?? ''
  dialogVisible.value = true
}

async function submit() {
  if (!form.type.trim() || form.pricePerKg == null) return
  await upsertMaterialPrice({ type: form.type.trim(), pricePerKg: form.pricePerKg, effectiveDate: form.effectiveDate || undefined })
  ElMessage.success('已保存')
  dialogVisible.value = false
  load()
}

function defaultRangeDays(days: number) {
  const to = new Date()
  const from = new Date()
  from.setDate(to.getDate() - days)
  const f = from.toISOString().slice(0, 10)
  const t = to.toISOString().slice(0, 10)
  return { f, t }
}

async function openHistory(row: MaterialPrice) {
  historyVisible.value = true
  historyLoading.value = true
  historyType.value = row.type
  if (!historyFrom.value || !historyTo.value) {
    const r = defaultRangeDays(90)
    historyFrom.value = r.f
    historyTo.value = r.t
  }
  try {
    historyItems.value = await getMaterialPriceHistory(historyType.value, historyFrom.value, historyTo.value)
  } catch {
    ElMessage.error('加载历史失败')
    historyVisible.value = false
  } finally {
    historyLoading.value = false
  }
}

async function reloadHistory() {
  if (!historyType.value) return
  historyLoading.value = true
  try {
    historyItems.value = await getMaterialPriceHistory(historyType.value, historyFrom.value, historyTo.value)
  } catch {
    ElMessage.error('加载历史失败')
  } finally {
    historyLoading.value = false
  }
}

async function runSuggest() {
  if (!suggestKeyword.value.trim()) return
  suggestLoading.value = true
  try {
    suggestItems.value = await suggestMaterialSources(suggestKeyword.value.trim())
    if (!suggestItems.value.length) ElMessage.warning('未检索到候选材料')
  } finally {
    suggestLoading.value = false
  }
}

async function addFromSuggest(row: MaterialSourceSuggestResult) {
  await upsertMaterialSource({
    type: row.type,
    displayName: row.displayName,
    sourceName: row.sourceName,
    sourceUrl: row.sourceUrl,
    parseKeyword: row.parseKeyword,
    enabled: true,
  })
  ElMessage.success('已添加到抓取材料')
  load()
}

async function toggleSource(row: MaterialSourceConfig, enabled: boolean) {
  await upsertMaterialSource({
    type: row.type,
    displayName: row.displayName,
    sourceName: row.sourceName,
    sourceUrl: row.sourceUrl,
    parseKeyword: row.parseKeyword,
    enabled,
  })
  ElMessage.success('已更新')
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
      <el-table-column label="跳转" width="120">
        <template #default="{ row }">
          <el-link v-if="row.sourceUrl" :href="row.sourceUrl" target="_blank" underline="never">实时查看</el-link>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button v-if="canEdit" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="primary" @click="openHistory(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-card style="margin-top:12px;">
    <template #header>
      <div style="display:flex;align-items:center;justify-content:space-between;">
        <div>抓取材料配置</div>
      </div>
    </template>
    <div style="display:flex;gap:8px;align-items:center;margin-bottom:12px;">
      <el-input v-model="suggestKeyword" placeholder="输入材料中文或英文，例如：橡胶 / rubber" style="width:420px;" />
      <el-button :loading="suggestLoading" @click="runSuggest">检索生意社候选</el-button>
    </div>
    <el-table :data="suggestItems" stripe style="margin-bottom:12px;">
      <el-table-column prop="displayName" label="候选材料" min-width="180" />
      <el-table-column prop="type" label="类型编码" width="140" />
      <el-table-column label="来源链接" min-width="260">
        <template #default="{ row }">
          <el-link :href="row.sourceUrl" target="_blank" underline="never">打开</el-link>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button v-if="canEdit" type="primary" size="small" @click="addFromSuggest(row)">添加</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-table :data="sourceItems" stripe>
      <el-table-column prop="displayName" label="已配置材料" min-width="180" />
      <el-table-column prop="type" label="类型编码" width="140" />
      <el-table-column prop="parseKeyword" label="解析关键词" min-width="160" />
      <el-table-column label="来源链接" min-width="260">
        <template #default="{ row }">
          <el-link :href="row.sourceUrl" target="_blank" underline="never">打开</el-link>
        </template>
      </el-table-column>
      <el-table-column label="启用" width="100">
        <template #default="{ row }">
          <el-switch :model-value="row.enabled" :disabled="!canEdit" @change="(v:boolean) => toggleSource(row, v)" />
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
      <el-form-item label="生效日期">
        <el-input v-model="form.effectiveDate" placeholder="YYYY-MM-DD（默认当天）" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="submit">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="historyVisible" title="材料价格详情" width="960px">
    <div style="display:flex;align-items:center;gap:12px;margin-bottom:12px;">
      <div style="font-weight:600;">{{ historyType }}</div>
      <div style="flex:1;" />
      <el-input v-model="historyFrom" placeholder="from: YYYY-MM-DD" style="width:160px;" />
      <el-input v-model="historyTo" placeholder="to: YYYY-MM-DD" style="width:160px;" />
      <el-button :loading="historyLoading" @click="reloadHistory">查询</el-button>
    </div>
    <el-table :data="historyItems" v-loading="historyLoading" stripe>
      <el-table-column prop="effectiveDate" label="生效日期" width="140" />
      <el-table-column prop="pricePerKg" label="价格(元/kg)" width="140" />
      <el-table-column prop="fetchedAt" label="抓取时间" width="180" />
      <el-table-column prop="sourceName" label="来源" min-width="160" />
      <el-table-column label="来源链接" min-width="220">
        <template #default="{ row }">
          <el-link v-if="row.sourceUrl" :href="row.sourceUrl" target="_blank" underline="never">打开</el-link>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>
</template>
