<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useAuthStore } from '../stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Page, VehicleDocument, VehicleImage, VehicleModel, VehicleUpsertRequest } from '../api/types'
import { createVehicle, deleteVehicle, getVehicle, searchVehicles, updateVehicle, getVehicleFacets, type VehicleSearchParams } from '../api/vehicles'
import { deleteVehicleDocument, deleteVehicleImage, updateVehicleImage, getHtmlContent, getSignedUrl } from '../api/vehicleMedia'
import { getDismantleRecords, createDismantleRecord, deleteDismantleRecord } from '../api/dismantle'
import type { VehicleDismantleRecord } from '../api/types'

const q = ref('')
const loading = ref(false)
const page = ref(0)
const size = ref(20)
const result = ref<Page<VehicleModel>>({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 })

const isAdvSearchExpanded = ref(false)
const advParams = reactive<{
  brands: string[]
  manufacturers: string[]
  vehicleTypes: string[]
  fuelTypes: string[]
  sourceTypes: string[]
  batchNoMin?: number
  batchNoMax?: number
}>({
  brands: [],
  manufacturers: [],
  vehicleTypes: [],
  fuelTypes: [],
  sourceTypes: [],
  batchNoMin: undefined,
  batchNoMax: undefined
})

const brandOptions = ref<string[]>([])
const manufacturerOptions = ref<string[]>([])
const vehicleTypeOptions = ref<string[]>([])
const fuelTypeOptions = ref<string[]>([])
const sourceTypeOptions = ref([
  { label: '系统采集', value: 'CRAWLED' },
  { label: '手动录入', value: 'MANUAL' },
  { label: '采集后编辑', value: 'EDITED' }
])

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const currentId = ref<number | null>(null)

const auth = useAuthStore()
const canEdit = computed(() => (auth.me?.roles ?? []).some(r => ['ADMIN', 'OPERATOR'].includes(r)))
type VehicleSearchHistoryItem = {
  id: string
  q: string
  advParams: {
    brands: string[]
    manufacturers: string[]
    vehicleTypes: string[]
    fuelTypes: string[]
    sourceTypes: string[]
    batchNoMin?: number
    batchNoMax?: number
  }
}
const vehicleSearchHistory = ref<VehicleSearchHistoryItem[]>([])
const vehicleSearchHistoryPrefix = 'admin_vehicle_search_history'
const vehicleSearchHistoryLimit = 10

const mediaVisible = ref(false)
const mediaLoading = ref(false)
const mediaVehicle = ref<VehicleModel | null>(null)
const previewDocVisible = ref(false)
const previewDocUrl = ref('')

const dismantleVisible = ref(false)
const dismantleLoading = ref(false)
const dismantleRecords = ref<VehicleDismantleRecord[]>([])
const currentDismantleVehicleId = ref<number | null>(null)
const dismantleFormVisible = ref(false)
const dismantleForm = reactive<Partial<VehicleDismantleRecord>>({
  steelWeight: 0,
  aluminumWeight: 0,
  copperWeight: 0,
  batteryWeight: 0,
  otherWeight: 0,
  remark: ''
})

const form = reactive<VehicleUpsertRequest>({
  brand: '',
  model: '',
  modelYear: new Date().getFullYear(),
  fuelType: '',
  vehicleType: '',
  curbWeight: undefined,
  grossWeight: null,
  batteryKwh: null,
  productId: '',
  productNo: '',
})

const title = computed(() => (dialogMode.value === 'create' ? '新增车型' : '编辑车型'))

async function loadFacets() {
  try {
    const facets = await getVehicleFacets()
    brandOptions.value = facets.brands
    manufacturerOptions.value = facets.manufacturers
    vehicleTypeOptions.value = facets.vehicleTypes
    fuelTypeOptions.value = facets.fuelTypes
  } catch (e) {
    console.error('Failed to load facets', e)
  }
}

const sortProp = ref('id')
const sortOrder = ref('descending')

function getVehicleSearchHistoryKey() {
  return `${vehicleSearchHistoryPrefix}_${auth.me?.userId ?? 'anonymous'}`
}

function createHistoryId() {
  return `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
}

function captureAdvParams() {
  return {
    brands: [...advParams.brands],
    manufacturers: [...advParams.manufacturers],
    vehicleTypes: [...advParams.vehicleTypes],
    fuelTypes: [...advParams.fuelTypes],
    sourceTypes: [...advParams.sourceTypes],
    batchNoMin: advParams.batchNoMin,
    batchNoMax: advParams.batchNoMax,
  }
}

function normalizeHistoryItem(item: any): VehicleSearchHistoryItem | null {
  if (typeof item === 'string') {
    return {
      id: createHistoryId(),
      q: item.trim(),
      advParams: {
        brands: [],
        manufacturers: [],
        vehicleTypes: [],
        fuelTypes: [],
        sourceTypes: [],
        batchNoMin: undefined,
        batchNoMax: undefined,
      },
    }
  }
  if (!item || typeof item !== 'object') return null
  return {
    id: typeof item.id === 'string' && item.id ? item.id : createHistoryId(),
    q: typeof item.q === 'string' ? item.q.trim() : '',
    advParams: {
      brands: Array.isArray(item.advParams?.brands) ? item.advParams.brands.filter((v: any) => typeof v === 'string') : [],
      manufacturers: Array.isArray(item.advParams?.manufacturers) ? item.advParams.manufacturers.filter((v: any) => typeof v === 'string') : [],
      vehicleTypes: Array.isArray(item.advParams?.vehicleTypes) ? item.advParams.vehicleTypes.filter((v: any) => typeof v === 'string') : [],
      fuelTypes: Array.isArray(item.advParams?.fuelTypes) ? item.advParams.fuelTypes.filter((v: any) => typeof v === 'string') : [],
      sourceTypes: Array.isArray(item.advParams?.sourceTypes) ? item.advParams.sourceTypes.filter((v: any) => typeof v === 'string') : [],
      batchNoMin: typeof item.advParams?.batchNoMin === 'number' ? item.advParams.batchNoMin : undefined,
      batchNoMax: typeof item.advParams?.batchNoMax === 'number' ? item.advParams.batchNoMax : undefined,
    },
  }
}

function loadSearchHistory() {
  const raw = localStorage.getItem(getVehicleSearchHistoryKey())
  if (!raw) {
    vehicleSearchHistory.value = []
    return
  }
  try {
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) {
      vehicleSearchHistory.value = []
      return
    }
    vehicleSearchHistory.value = parsed
      .map(v => normalizeHistoryItem(v))
      .filter((v): v is VehicleSearchHistoryItem => !!v)
      .slice(0, vehicleSearchHistoryLimit)
  } catch {
    vehicleSearchHistory.value = []
  }
}

function saveSearchHistory(history: VehicleSearchHistoryItem[]) {
  localStorage.setItem(getVehicleSearchHistoryKey(), JSON.stringify(history))
}

function recordSearchHistory() {
  const nextItem: VehicleSearchHistoryItem = {
    id: createHistoryId(),
    q: q.value.trim(),
    advParams: captureAdvParams(),
  }
  const next = [nextItem, ...vehicleSearchHistory.value].slice(0, vehicleSearchHistoryLimit)
  vehicleSearchHistory.value = next
  saveSearchHistory(next)
}

async function load() {
  loading.value = true
  try {
    const params: VehicleSearchParams = {
      q: q.value,
      page: page.value,
      size: size.value,
      ...advParams,
      sort: sortProp.value ? `${sortProp.value},${sortOrder.value === 'ascending' ? 'asc' : 'desc'}` : undefined
    }
    result.value = await searchVehicles(params)
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function onSearch(recordHistory = true) {
  if (recordHistory) recordSearchHistory()
  page.value = 0
  load()
}

function applySearchHistory(item: VehicleSearchHistoryItem) {
  q.value = item.q
  advParams.brands = [...item.advParams.brands]
  advParams.manufacturers = [...item.advParams.manufacturers]
  advParams.vehicleTypes = [...item.advParams.vehicleTypes]
  advParams.fuelTypes = [...item.advParams.fuelTypes]
  advParams.sourceTypes = [...item.advParams.sourceTypes]
  advParams.batchNoMin = item.advParams.batchNoMin
  advParams.batchNoMax = item.advParams.batchNoMax
  onSearch(false)
}

function clearSearchHistory() {
  vehicleSearchHistory.value = []
  localStorage.removeItem(getVehicleSearchHistoryKey())
}

function getSearchHistoryLabel(item: VehicleSearchHistoryItem) {
  const parts: string[] = []
  if (item.q) parts.push(`关键词:${item.q}`)
  if (item.advParams.brands.length) parts.push(`品牌:${item.advParams.brands.join('/')}`)
  if (item.advParams.manufacturers.length) parts.push(`企业:${item.advParams.manufacturers.join('/')}`)
  if (item.advParams.vehicleTypes.length) parts.push(`类型:${item.advParams.vehicleTypes.join('/')}`)
  if (item.advParams.fuelTypes.length) parts.push(`燃料:${item.advParams.fuelTypes.join('/')}`)
  if (item.advParams.sourceTypes.length) parts.push(`来源:${item.advParams.sourceTypes.join('/')}`)
  if (item.advParams.batchNoMin != null || item.advParams.batchNoMax != null) {
    parts.push(`批次:${item.advParams.batchNoMin ?? ''}-${item.advParams.batchNoMax ?? ''}`)
  }
  return parts.length ? parts.join(' | ') : '全部条件'
}

function onSortChange({ prop, order }: { prop: string, order: string }) {
  sortProp.value = prop
  sortOrder.value = order
  onSearch(false)
}

function toggleAdvSearch() {
  isAdvSearchExpanded.value = !isAdvSearchExpanded.value
}

function resetAdvSearch() {
   advParams.brands = []
   advParams.manufacturers = []
   advParams.vehicleTypes = []
   advParams.fuelTypes = []
   advParams.sourceTypes = []
   advParams.batchNoMin = undefined
   advParams.batchNoMax = undefined
   onSearch(false)
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
  form.grossWeight = row.grossWeight ?? null
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
  form.grossWeight = null
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
    grossWeight: form.grossWeight ?? null,
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

async function openMedia(row: VehicleModel) {
  mediaVisible.value = true
  mediaLoading.value = true
  try {
    mediaVehicle.value = await getVehicle(row.id)
  } catch {
    ElMessage.error('加载媒体失败')
    mediaVisible.value = false
  } finally {
    mediaLoading.value = false
  }
}

async function showDoc(url: string) {
  try {
    if (isHtmlUrl(url)) {
      const signedUrl = await getSignedUrl(url)
      const html = await getHtmlContent(url)
      previewDocUrl.value = buildHtmlBlobUrl(html, signedUrl)
    } else {
      const signedUrl = await getSignedUrl(url)
      previewDocUrl.value = signedUrl
    }
    previewDocVisible.value = true
  } catch {
    ElMessage.error('获取预览链接失败')
  }
}

async function openDoc(url: string) {
  try {
    if (isHtmlUrl(url)) {
      const signedUrl = await getSignedUrl(url)
      const html = await getHtmlContent(url)
      const blobUrl = buildHtmlBlobUrl(html, signedUrl)
      window.open(blobUrl, '_blank')
      setTimeout(() => URL.revokeObjectURL(blobUrl), 60_000)
    } else {
      const signed = await getSignedUrl(url)
      window.open(signed, '_blank')
    }
  } catch {
    ElMessage.error('打开文件失败')
  }
}

watch(previewDocVisible, (v) => {
  if (!v && previewDocUrl.value.startsWith('blob:')) {
    URL.revokeObjectURL(previewDocUrl.value)
    previewDocUrl.value = ''
  }
})

watch(
  () => auth.me?.userId,
  () => {
    loadSearchHistory()
  },
  { immediate: true }
)

function isHtmlUrl(url: string): boolean {
  const clean = (url.split('?')[0] ?? '').toLowerCase()
  return clean.endsWith('.html') || clean.endsWith('.htm')
}

function buildHtmlBlobUrl(rawHtml: string, baseUrl: string): string {
  const baseTag = `<base href="${baseUrl}">`
  let html = rawHtml
  html = html.replace(/http:\/\//gi, 'https://')
  if (html.includes('<head>')) {
    html = html.replace('<head>', `<head>${baseTag}<meta http-equiv="Content-Security-Policy" content="upgrade-insecure-requests">`)
  } else {
    html = `<html><head>${baseTag}<meta http-equiv="Content-Security-Policy" content="upgrade-insecure-requests"></head><body>${html}</body></html>`
  }
  return URL.createObjectURL(new Blob([html], { type: 'text/html' }))
}

async function loadDismantleRecords(vehicleId: number) {
  dismantleLoading.value = true
  try {
    dismantleRecords.value = await getDismantleRecords(vehicleId)
  } catch (e) {
    ElMessage.error('加载拆解记录失败')
  } finally {
    dismantleLoading.value = false
  }
}

function openDismantle(row: VehicleModel) {
  currentDismantleVehicleId.value = row.id
  dismantleVisible.value = true
  loadDismantleRecords(row.id)
}

function openDismantleForm() {
  dismantleForm.steelWeight = 0
  dismantleForm.aluminumWeight = 0
  dismantleForm.copperWeight = 0
  dismantleForm.batteryWeight = 0
  dismantleForm.otherWeight = 0
  dismantleForm.remark = ''
  dismantleFormVisible.value = true
}

async function submitDismantle() {
  if (!currentDismantleVehicleId.value) return
  
  try {
    await createDismantleRecord({
      vehicleId: currentDismantleVehicleId.value,
      steelWeight: dismantleForm.steelWeight || 0,
      aluminumWeight: dismantleForm.aluminumWeight || 0,
      copperWeight: dismantleForm.copperWeight || 0,
      batteryWeight: dismantleForm.batteryWeight || 0,
      otherWeight: dismantleForm.otherWeight || 0,
      remark: dismantleForm.remark
    } as any)
    ElMessage.success('已保存')
    dismantleFormVisible.value = false
    loadDismantleRecords(currentDismantleVehicleId.value)
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

async function deleteDismantle(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该记录吗？', '删除确认', { type: 'warning' })
    await deleteDismantleRecord(id)
    ElMessage.success('已删除')
    if (currentDismantleVehicleId.value) {
      loadDismantleRecords(currentDismantleVehicleId.value)
    }
  } catch {
  }
}

async function saveImage(vehicleId: number, img: VehicleImage) {
  try {
    await updateVehicleImage(vehicleId, img.id, { imageName: img.imageName ?? null, sortOrder: img.sortOrder ?? 0 })
    ElMessage.success('已保存')
    mediaVehicle.value = await getVehicle(vehicleId)
  } catch {
    ElMessage.error('保存失败')
  }
}

async function removeImage(vehicleId: number, img: VehicleImage) {
  try {
    await ElMessageBox.confirm('确认删除该图片吗？', '删除确认', { type: 'warning' })
    await deleteVehicleImage(vehicleId, img.id)
    ElMessage.success('已删除')
    mediaVehicle.value = await getVehicle(vehicleId)
  } catch {
  }
}

async function removeDoc(vehicleId: number, doc: VehicleDocument) {
  try {
    await ElMessageBox.confirm('确认删除该文档吗？', '删除确认', { type: 'warning' })
    await deleteVehicleDocument(vehicleId, doc.id)
    ElMessage.success('已删除')
    mediaVehicle.value = await getVehicle(vehicleId)
  } catch {
  }
}

async function handleUploadSuccess() {
  ElMessage.success('上传成功')
  if (mediaVehicle.value) {
    mediaVehicle.value = await getVehicle(mediaVehicle.value.id)
  }
}

function handleUploadError() {
  ElMessage.error('上传失败')
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
loadFacets()
</script>

<template>
  <el-card>
    <template #header>
      <div style="display:flex;flex-direction:column;gap:12px;">
        <div style="display:flex;align-items:center;justify-content:space-between;gap:12px;">
          <div style="display:flex;align-items:center;gap:12px;flex:1;">
            <el-input v-model="q" placeholder="搜索品牌/车型/产品号..." style="max-width:400px;" @keyup.enter="onSearch">
              <template #append>
                <el-button @click="onSearch">查询</el-button>
              </template>
            </el-input>
            <el-button @click="toggleAdvSearch">
              {{ isAdvSearchExpanded ? '收起筛选' : '高级搜索' }}
            </el-button>
          </div>
          <el-button v-if="canEdit" type="primary" @click="openCreate">新增车型</el-button>
        </div>
        <div v-if="vehicleSearchHistory.length" style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;">
          <span style="font-size:12px;color:var(--el-text-color-secondary);">搜索历史</span>
          <el-tag
            v-for="item in vehicleSearchHistory"
            :key="item.id"
            style="cursor:pointer;"
            effect="plain"
            @click="applySearchHistory(item)"
          >
            {{ getSearchHistoryLabel(item) }}
          </el-tag>
          <el-button link type="primary" @click="clearSearchHistory">清空</el-button>
        </div>

        <div v-show="isAdvSearchExpanded" style="background-color:var(--el-fill-color-light);padding:16px;border-radius:4px;">
           <el-form label-position="top">
             <el-row :gutter="20">
               <el-col :span="6">
                 <el-form-item label="品牌">
                   <el-select v-model="advParams.brands" multiple filterable collapse-tags placeholder="全部" style="width:100%">
                     <el-option v-for="item in brandOptions" :key="item" :label="item" :value="item" />
                   </el-select>
                 </el-form-item>
               </el-col>
               <el-col :span="6">
                 <el-form-item label="生产企业">
                   <el-select v-model="advParams.manufacturers" multiple filterable collapse-tags placeholder="全部" style="width:100%">
                     <el-option v-for="item in manufacturerOptions" :key="item" :label="item" :value="item" />
                   </el-select>
                 </el-form-item>
               </el-col>
               <el-col :span="6">
                 <el-form-item label="车辆类型">
                   <el-select v-model="advParams.vehicleTypes" multiple filterable collapse-tags placeholder="全部" style="width:100%">
                     <el-option v-for="item in vehicleTypeOptions" :key="item" :label="item" :value="item" />
                   </el-select>
                 </el-form-item>
               </el-col>
               <el-col :span="6">
                 <el-form-item label="燃料类型">
                   <el-select v-model="advParams.fuelTypes" multiple filterable collapse-tags placeholder="全部" style="width:100%">
                     <el-option v-for="item in fuelTypeOptions" :key="item" :label="item" :value="item" />
                   </el-select>
                 </el-form-item>
               </el-col>
               <el-col :span="6">
                 <el-form-item label="数据来源">
                   <el-select v-model="advParams.sourceTypes" multiple collapse-tags placeholder="全部" style="width:100%">
                     <el-option v-for="item in sourceTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
                   </el-select>
                 </el-form-item>
               </el-col>
               <el-col :span="12">
                 <el-form-item label="批次范围">
                   <div style="display:flex;align-items:center;gap:8px;">
                     <el-input-number v-model="advParams.batchNoMin" :min="0" placeholder="起始批次" style="width:140px;" />
                     <span>-</span>
                     <el-input-number v-model="advParams.batchNoMax" :min="0" placeholder="结束批次" style="width:140px;" />
                   </div>
                 </el-form-item>
               </el-col>
               <el-col :span="6" style="display:flex;align-items:flex-end;justify-content:flex-end;">
                 <el-button @click="resetAdvSearch">重置</el-button>
                 <el-button type="primary" @click="onSearch">确认筛选</el-button>
               </el-col>
             </el-row>
           </el-form>
        </div>
      </div>
    </template>

    <el-table :data="result.content" v-loading="loading" stripe @sort-change="onSortChange">
      <el-table-column prop="id" label="ID" width="90" sortable="custom" />
      <el-table-column prop="sourceType" label="来源" width="100" sortable="custom">
        <template #default="{ row }">
          <el-tag v-if="row.sourceType === 'CRAWLED'" type="info">系统采集</el-tag>
          <el-tag v-else-if="row.sourceType === 'MANUAL'" type="success">手动录入</el-tag>
          <el-tag v-else-if="row.sourceType === 'EDITED'" type="warning">采集后编辑</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="brand" label="品牌" width="120" sortable="custom" />
      <el-table-column prop="model" label="车型" width="180" sortable="custom" />
      <el-table-column prop="releaseDate" label="发布日期" width="120" sortable="custom" />
      <el-table-column prop="fuelType" label="燃料" width="120" sortable="custom" />
      <el-table-column prop="vehicleType" label="类型" width="140" sortable="custom" />
      <el-table-column prop="curbWeight" label="整备质量(kg)" width="140" sortable="custom" />
      <el-table-column prop="grossWeight" label="总质量(kg)" width="140" sortable="custom" />
      <el-table-column prop="batteryKwh" label="电池(kWh)" width="120" sortable="custom" />
      <el-table-column prop="productId" label="产品ID" width="140" sortable="custom" />
      <el-table-column prop="productNo" label="产品号" width="160" sortable="custom" />
      <el-table-column prop="batchNo" label="批次" width="100" sortable="custom" />
      <el-table-column label="HTML 存档" width="160">
        <template #default="{ row }">
          <div v-if="row.documents && row.documents.length > 0">
            <el-link @click="openDoc(row.documents[0].docUrl)" underline="never" type="primary">查看网页</el-link>
            <el-link v-if="row.documents[0].sourceUrl" :href="row.documents[0].sourceUrl" target="_blank" underline="never" type="warning" style="margin-left:8px;font-size:12px;">来源</el-link>
          </div>
          <span v-else style="color:var(--el-text-color-placeholder);">无</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openMedia(row)">媒体</el-button>
          <el-button link type="primary" size="small" @click="openDismantle(row)">拆解记录</el-button>
          <el-button v-if="canEdit" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="canEdit" link type="danger" size="small" @click="onDelete(row)">删除</el-button>
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
      v-model="dialogVisible" :title="title" width="680px">
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
      <el-form-item label="总质量kg">
        <el-input-number v-model="form.grossWeight" :min="0" :precision="2" :step="10" style="width:220px;" />
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

  <el-drawer v-model="mediaVisible" title="车型媒体" size="880px">
    <div v-loading="mediaLoading">
      <div v-if="mediaVehicle" style="margin-bottom:12px;">
        <div style="font-weight:600;">#{{ mediaVehicle.id }} {{ mediaVehicle.brand }} {{ mediaVehicle.model }} {{ mediaVehicle.modelYear }}</div>
        <div style="color:var(--el-text-color-secondary);font-size:12px;">productId: {{ mediaVehicle.productId || '-' }} / productNo: {{ mediaVehicle.productNo || '-' }}</div>
      </div>

      <el-card v-if="mediaVehicle" style="margin-bottom:12px;">
        <template #header>
          <div style="display:flex;align-items:center;justify-content:space-between;">
            <div style="font-weight:600;">图片</div>
            <el-upload
              v-if="canEdit"
              :action="`/api/admin/vehicles/${mediaVehicle.id}/images`"
              :show-file-list="false"
              :on-success="handleUploadSuccess"
              :on-error="handleUploadError"
              accept="image/*"
              style="display:inline-block;"
            >
              <el-button type="primary" size="small">上传图片</el-button>
            </el-upload>
          </div>
        </template>
        <el-table :data="mediaVehicle.images || []" stripe>
          <el-table-column label="预览" width="120">
            <template #default="{ row }">
              <el-image :src="row.imageUrl" :preview-src-list="(mediaVehicle?.images || []).map(i => i.imageUrl)" style="width:88px;height:56px;object-fit:cover;border-radius:6px;" />
            </template>
          </el-table-column>
          <el-table-column label="名称" min-width="220">
            <template #default="{ row }">
              <el-input v-model="row.imageName" placeholder="可选" />
            </template>
          </el-table-column>
          <el-table-column label="排序" width="140">
            <template #default="{ row }">
              <el-input-number v-model="row.sortOrder" :min="0" :max="9999" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button v-if="canEdit" size="small" type="primary" @click="saveImage(mediaVehicle!.id, row)">保存</el-button>
              <el-button v-if="canEdit" size="small" type="danger" @click="removeImage(mediaVehicle!.id, row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card v-if="mediaVehicle">
        <template #header>
          <div style="font-weight:600;">PDF/文档</div>
        </template>
        <el-table :data="mediaVehicle.documents || []" stripe>
          <el-table-column prop="docType" label="类型" width="120" />
          <el-table-column prop="docName" label="名称" min-width="220" />
          <el-table-column prop="fetchedAt" label="抓取时间" width="190" />
          <el-table-column label="链接" min-width="240">
            <template #default="{ row }">
              <el-link @click="openDoc(row.docUrl)" underline="never" type="primary">打开</el-link>
              <el-button size="small" style="margin-left:8px;" @click="showDoc(row.docUrl)">预览</el-button>
            </template>
          </el-table-column>
          <el-table-column label="来源" min-width="200">
            <template #default="{ row }">
              <el-link v-if="row.sourceUrl" :href="row.sourceUrl" target="_blank" underline="never">来源页</el-link>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button v-if="canEdit" size="small" type="danger" @click="removeDoc(mediaVehicle!.id, row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </el-drawer>

  <el-dialog v-model="dismantleVisible" title="拆解记录" width="900px">
    <div style="margin-bottom:12px;text-align:right;">
      <el-button type="primary" @click="openDismantleForm">新增记录</el-button>
    </div>
    <el-table :data="dismantleRecords" v-loading="dismantleLoading" stripe border>
      <el-table-column prop="createdAt" label="录入时间" width="160" />
      <el-table-column prop="operatorName" label="操作员" width="120" />
      <el-table-column prop="steelWeight" label="钢(kg)" width="100" />
      <el-table-column prop="aluminumWeight" label="铝(kg)" width="100" />
      <el-table-column prop="copperWeight" label="铜(kg)" width="100" />
      <el-table-column prop="batteryWeight" label="电池(kg)" width="100" />
      <el-table-column prop="otherWeight" label="其他(kg)" width="100" />
      <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="danger" size="small" @click="deleteDismantle(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>

  <el-dialog v-model="dismantleFormVisible" title="录入拆解数据" width="500px">
    <el-form label-width="100px">
      <el-form-item label="钢重量(kg)">
        <el-input-number v-model="dismantleForm.steelWeight" :min="0" :precision="2" style="width:100%" />
      </el-form-item>
      <el-form-item label="铝重量(kg)">
        <el-input-number v-model="dismantleForm.aluminumWeight" :min="0" :precision="2" style="width:100%" />
      </el-form-item>
      <el-form-item label="铜重量(kg)">
        <el-input-number v-model="dismantleForm.copperWeight" :min="0" :precision="2" style="width:100%" />
      </el-form-item>
      <el-form-item label="电池重量(kg)">
        <el-input-number v-model="dismantleForm.batteryWeight" :min="0" :precision="2" style="width:100%" />
      </el-form-item>
      <el-form-item label="其他重量(kg)">
        <el-input-number v-model="dismantleForm.otherWeight" :min="0" :precision="2" style="width:100%" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input type="textarea" v-model="dismantleForm.remark" rows="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dismantleFormVisible = false">取消</el-button>
      <el-button type="primary" @click="submitDismantle">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="previewDocVisible" title="文档预览" width="980px">
    <iframe v-if="previewDocUrl" :src="previewDocUrl" style="width:100%;height:70vh;border:none;" />
  </el-dialog>
</template>
