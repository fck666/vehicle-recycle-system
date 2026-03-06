<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useAuthStore } from '../stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Page, VehicleDocument, VehicleImage, VehicleModel, VehicleUpsertRequest } from '../api/types'
import { createVehicle, deleteVehicle, getVehicle, searchVehicles, updateVehicle, type VehicleSearchParams } from '../api/vehicles'
import { deleteVehicleDocument, deleteVehicleImage, updateVehicleImage } from '../api/vehicleMedia'

const q = ref('')
const loading = ref(false)
const page = ref(0)
const size = ref(20)
const result = ref<Page<VehicleModel>>({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 })

const advSearchVisible = ref(false)
const advParams = reactive<{
  brands: string[]
  manufacturers: string[]
  vehicleTypes: string[]
  fuelTypes: string[]
  sourceTypes: string[]
}>({
  brands: [],
  manufacturers: [],
  vehicleTypes: [],
  fuelTypes: [],
  sourceTypes: []
})

// TODO: These should be fetched from backend aggregations, but for now we can allow user input or hardcode some
const brandOptions = ref(['奥迪', '宝马', '奔驰', '大众', '丰田', '本田'])
const manufacturerOptions = ref(['一汽-大众', '上汽大众', '一汽丰田', '广汽丰田'])
const vehicleTypeOptions = ref(['轿车', 'SUV', 'MPV', '货车', '客车'])
const fuelTypeOptions = ref(['汽油', '柴油', '纯电动', '插电式混合动力'])
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

const mediaVisible = ref(false)
const mediaLoading = ref(false)
const mediaVehicle = ref<VehicleModel | null>(null)
const previewDocVisible = ref(false)
const previewDocUrl = ref('')

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

async function load() {
  loading.value = true
  try {
    const params: VehicleSearchParams = {
      q: q.value,
      page: page.value,
      size: size.value,
      ...advParams
    }
    result.value = await searchVehicles(params)
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function onSearch() {
  page.value = 0
  load()
}

function onAdvSearch() {
  advSearchVisible.value = false
  onSearch()
}

function resetAdvSearch() {
   advParams.brands = []
   advParams.manufacturers = []
   advParams.vehicleTypes = []
   advParams.fuelTypes = []
   advParams.sourceTypes = []
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

function showDoc(url: string) {
  previewDocUrl.value = url
  previewDocVisible.value = true
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
</script>

<template>
  <el-card>
    <template #header>
      <div style="display:flex;align-items:center;justify-content:space-between;gap:12px;">
        <div style="display:flex;align-items:center;gap:12px;">
          <el-input v-model="q" placeholder="搜索品牌/车型/产品号..." style="width:300px;" @keyup.enter="onSearch" />
          <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
          <el-button @click="advSearchVisible = true">高级搜索</el-button>
        </div>
        <el-button v-if="canEdit" type="primary" @click="openCreate">新增车型</el-button>
      </div>
    </template>

    <el-table :data="result.content" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="sourceType" label="来源" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.sourceType === 'CRAWLED'" type="info">系统采集</el-tag>
          <el-tag v-else-if="row.sourceType === 'MANUAL'" type="success">手动录入</el-tag>
          <el-tag v-else-if="row.sourceType === 'EDITED'" type="warning">采集后编辑</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="brand" label="品牌" width="120" />
      <el-table-column prop="model" label="车型" width="180" />
      <el-table-column prop="modelYear" label="年份" width="100" />
      <el-table-column prop="fuelType" label="燃料" width="120" />
      <el-table-column prop="vehicleType" label="类型" width="140" />
      <el-table-column prop="curbWeight" label="整备质量(kg)" width="140" />
      <el-table-column prop="grossWeight" label="总质量(kg)" width="140" />
      <el-table-column prop="batteryKwh" label="电池(kWh)" width="120" />
      <el-table-column prop="productId" label="产品ID" width="140" />
      <el-table-column prop="productNo" label="产品号" width="160" />
      <el-table-column prop="batchNo" label="批次" width="100" />
      <el-table-column label="HTML 存档" width="160">
        <template #default="{ row }">
          <div v-if="row.documents && row.documents.length > 0">
            <el-link :href="row.documents[0].docUrl" target="_blank" underline="never" type="primary">查看网页</el-link>
            <el-link v-if="row.documents[0].sourceUrl" :href="row.documents[0].sourceUrl" target="_blank" underline="never" type="warning" style="margin-left:8px;font-size:12px;">来源</el-link>
          </div>
          <span v-else style="color:var(--el-text-color-placeholder);">无</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openMedia(row)">媒体</el-button>
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

  <el-drawer v-model="advSearchVisible" title="高级搜索" size="400px">
      <el-form label-position="top">
        <el-form-item label="品牌 (Brands)">
          <el-select
            v-model="advParams.brands"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="请选择或输入品牌"
            style="width:100%"
          >
            <el-option v-for="item in brandOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>

        <el-form-item label="生产企业 (Manufacturers)">
          <el-select
            v-model="advParams.manufacturers"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="请选择或输入企业名称"
            style="width:100%"
          >
            <el-option v-for="item in manufacturerOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>

        <el-form-item label="车辆类型 (Vehicle Types)">
          <el-select
            v-model="advParams.vehicleTypes"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="请选择或输入车辆类型"
            style="width:100%"
          >
            <el-option v-for="item in vehicleTypeOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>

        <el-form-item label="燃料类型 (Fuel Types)">
          <el-select
            v-model="advParams.fuelTypes"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="请选择或输入燃料类型"
            style="width:100%"
          >
            <el-option v-for="item in fuelTypeOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>

        <el-form-item label="数据来源 (Data Source)">
          <el-select
            v-model="advParams.sourceTypes"
            multiple
            placeholder="请选择数据来源"
            style="width:100%"
          >
            <el-option v-for="item in sourceTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div style="flex: auto">
          <el-button @click="resetAdvSearch">重置</el-button>
          <el-button type="primary" @click="onAdvSearch">确认搜索</el-button>
        </div>
      </template>
    </el-drawer>

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
              <el-link :href="row.docUrl" target="_blank" underline="never">打开</el-link>
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

  <el-dialog v-model="previewDocVisible" title="文档预览" width="980px">
    <iframe v-if="previewDocUrl" :src="previewDocUrl" style="width:100%;height:70vh;border:none;" />
  </el-dialog>
</template>
