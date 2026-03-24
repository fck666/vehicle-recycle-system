<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useAuthStore } from '../stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { MaterialRatioItem, MaterialTemplate, VehicleModel } from '../api/types'
import { deleteMaterialTemplateById, listMaterialPrices, listMaterialSources, listMaterialTemplates, upsertMaterialTemplate } from '../api/material'
import { getVehicleFacets, searchVehicles } from '../api/vehicles'

const OTHERS = 'others'
const PRICING_MODE_WEIGHT = 'WEIGHT' as const
const PRICING_MODE_FIXED_TOTAL = 'FIXED_TOTAL' as const

const loading = ref(false)
const items = ref<MaterialTemplate[]>([])
const materialOptions = ref<Array<{ type: string; label: string }>>([])
const vehicleTypeOptions = ref<string[]>([])
const vehicleOptions = ref<VehicleModel[]>([])

const dialogVisible = ref(false)
const form = reactive<{
  scopeType: 'VEHICLE_TYPE' | 'VEHICLE'
  scopeValue: string
  materials: MaterialRatioItem[]
  recoveryRatio: number | null
  othersPricePerKgOverride: number | null
}>({ scopeType: 'VEHICLE_TYPE', scopeValue: '', materials: [], recoveryRatio: null, othersPricePerKgOverride: null })

const auth = useAuthStore()
const canEdit = computed(() => (auth.me?.roles ?? []).some(r => ['ADMIN', 'OPERATOR'].includes(r)))

async function load() {
  loading.value = true
  try {
    const [templates, prices, sources, facets, vehiclesPage] = await Promise.all([
      listMaterialTemplates(),
      listMaterialPrices(),
      listMaterialSources(),
      getVehicleFacets(),
      searchVehicles({ page: 0, size: 200 }),
    ])
    items.value = templates
    const byType = new Map<string, string>()
    prices.forEach(p => byType.set(p.type, p.type))
    sources.filter(s => s.enabled).forEach(s => byType.set(s.type, `${s.displayName}(${s.type})`))
    materialOptions.value = [...byType.entries()]
      .map(([type, label]) => ({ type, label }))
      .sort((a, b) => a.type.localeCompare(b.type))
    if (!materialOptions.value.some(x => x.type === OTHERS)) {
      materialOptions.value.push({ type: OTHERS, label: '其余(others)' })
    }
    vehicleTypeOptions.value = facets.vehicleTypes ?? []
    vehicleOptions.value = vehiclesPage.content ?? []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.scopeType = 'VEHICLE_TYPE'
  form.scopeValue = ''
  form.materials = []
  form.recoveryRatio = null
  form.othersPricePerKgOverride = null
  dialogVisible.value = true
}

function openEdit(row: MaterialTemplate) {
  form.scopeType = row.scopeType ?? 'VEHICLE_TYPE'
  form.scopeValue = row.scopeValue ?? row.vehicleType ?? ''
  form.materials = (row.materials || []).map(m => ({
    materialType: m.materialType,
    ratio: m.ratio ?? 0,
    pricingMode: m.pricingMode || PRICING_MODE_WEIGHT,
    fixedTotalPrice: m.fixedTotalPrice ?? null,
  }))
  form.recoveryRatio = row.recoveryRatio
  form.othersPricePerKgOverride = row.othersPricePerKgOverride ?? null
  dialogVisible.value = true
}

function addMaterial() {
  form.materials.push({ materialType: '', ratio: 0, pricingMode: PRICING_MODE_WEIGHT, fixedTotalPrice: null })
}

function removeMaterial(index: number) {
  form.materials.splice(index, 1)
}

const ratioSum = computed(() =>
  form.materials
    .filter(m => (m.pricingMode || PRICING_MODE_WEIGHT) === PRICING_MODE_WEIGHT)
    .reduce((sum, it) => sum + (it.ratio || 0), 0),
)
const ratioWithoutOthers = computed(() =>
  form.materials
    .filter(m => (m.pricingMode || PRICING_MODE_WEIGHT) === PRICING_MODE_WEIGHT)
    .filter(m => m.materialType.trim().toLowerCase() !== OTHERS)
    .reduce((sum, it) => sum + (it.ratio || 0), 0),
)
const hasOthers = computed(() =>
  form.materials.some(m => m.materialType.trim().toLowerCase() === OTHERS && (m.pricingMode || PRICING_MODE_WEIGHT) === PRICING_MODE_WEIGHT),
)
const othersRatio = computed(() => Math.max(0, Number((1 - ratioWithoutOthers.value).toFixed(4))))

async function submit() {
  const normalized = form.materials
    .filter(m => m.materialType.trim())
    .map(m => ({
      materialType: m.materialType.trim().toLowerCase(),
      pricingMode: m.pricingMode || PRICING_MODE_WEIGHT,
      ratio: m.ratio,
      fixedTotalPrice: m.fixedTotalPrice,
    }))
    .filter(m => {
      if (m.pricingMode === PRICING_MODE_FIXED_TOTAL) {
        return m.fixedTotalPrice != null && m.fixedTotalPrice > 0
      }
      return m.ratio != null && m.ratio >= 0
    })
  const dedup = new Map<string, typeof normalized[number]>()
  normalized.forEach(m => dedup.set(m.materialType, m))
  const materialPayload = [...dedup.values()]
  const withoutOthers = materialPayload.filter(x => !(x.materialType === OTHERS && x.pricingMode === PRICING_MODE_WEIGHT))
  const sumWithoutOthers = withoutOthers.reduce((acc, x) => acc + (x.ratio || 0), 0)
  if (sumWithoutOthers > 1) {
    ElMessage.warning('除其余外的材料占比总和不能超过1')
    return
  }
  const hasOthersInPayload = materialPayload.some(x => x.materialType === OTHERS && x.pricingMode === PRICING_MODE_WEIGHT)
  const finalPayload: MaterialRatioItem[] = hasOthersInPayload
    ? [...withoutOthers, ...(othersRatio.value > 0 ? [{ materialType: OTHERS, ratio: othersRatio.value, pricingMode: PRICING_MODE_WEIGHT }] : [])]
    : materialPayload
  const sum = finalPayload
    .filter(x => x.pricingMode === PRICING_MODE_WEIGHT)
    .reduce((acc, x) => acc + (x.ratio || 0), 0)
  if (!form.scopeValue.trim() || form.recoveryRatio == null || finalPayload.length === 0 || sum > 1) {
    ElMessage.warning('请检查车型、材料配置，且按重量材料占比总和不超过1')
    return
  }
  const scopeValue = form.scopeValue.trim()
  await upsertMaterialTemplate({
    scopeType: form.scopeType,
    scopeValue,
    vehicleType: form.scopeType === 'VEHICLE_TYPE' ? scopeValue : undefined,
    recoveryRatio: form.recoveryRatio,
    othersPricePerKgOverride: form.othersPricePerKgOverride ?? undefined,
    materials: finalPayload,
  })
  ElMessage.success('已保存')
  dialogVisible.value = false
  load()
}

async function onDelete(row: MaterialTemplate) {
  await ElMessageBox.confirm(`确认删除模板 ${templateScopeText(row)} 吗？`, '删除确认', { type: 'warning' })
  await deleteMaterialTemplateById(row.id)
  ElMessage.success('已删除')
  load()
}

function materialLabel(type: string) {
  const hit = materialOptions.value.find(x => x.type === type)
  return hit ? hit.label : type
}

function materialText(row: MaterialTemplate) {
  return (row.materials || [])
    .map(m => {
      if ((m.pricingMode || PRICING_MODE_WEIGHT) === PRICING_MODE_FIXED_TOTAL) {
        return `${materialLabel(m.materialType)}:固定总价${m.fixedTotalPrice ?? 0}元`
      }
      return `${materialLabel(m.materialType)}:${m.ratio}`
    })
    .join('，')
}

function templateScopeText(row: MaterialTemplate) {
  if (row.scopeType === 'VEHICLE') {
    const hit = vehicleOptions.value.find(v => String(v.id) === row.scopeValue)
    if (hit) return `车型#${hit.id} ${hit.brand} ${hit.model}(${hit.modelYear})`
    return `车型#${row.scopeValue}`
  }
  return `类型:${row.scopeValue || row.vehicleType || ''}`
}

load()
</script>

<template>
  <el-card>
    <template #header>
      <div style="display:flex;align-items:center;justify-content:space-between;">
        <div>估值方式（材料配比模板）</div>
        <div style="display:flex;gap:8px;">
          <el-button :loading="loading" @click="load">刷新</el-button>
          <el-button v-if="canEdit" type="primary" @click="openCreate">新增模板</el-button>
        </div>
      </div>
    </template>

    <el-table :data="items" v-loading="loading" stripe>
      <el-table-column label="模板范围" min-width="220">
        <template #default="{ row }">{{ templateScopeText(row) }}</template>
      </el-table-column>
      <el-table-column label="材料占比" min-width="380">
        <template #default="{ row }">{{ materialText(row) }}</template>
      </el-table-column>
      <el-table-column prop="othersPricePerKgOverride" label="其余单价覆盖(元/kg)" width="170" />
      <el-table-column prop="recoveryRatio" label="回收系数" width="120" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button v-if="canEdit" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="canEdit" size="small" type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="dialogVisible" title="模板编辑" width="760px">
    <el-form label-width="110px">
      <el-form-item label="模板范围" required>
        <el-radio-group v-model="form.scopeType">
          <el-radio-button label="VEHICLE_TYPE">按车型类型</el-radio-button>
          <el-radio-button label="VEHICLE">按具体车型</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="form.scopeType === 'VEHICLE_TYPE'" label="车型类型" required>
        <el-select v-model="form.scopeValue" filterable allow-create default-first-option style="width:100%;">
          <el-option v-for="v in vehicleTypeOptions" :key="v" :label="v" :value="v" />
        </el-select>
      </el-form-item>
      <el-form-item v-else label="具体车型" required>
        <el-select v-model="form.scopeValue" filterable style="width:100%;">
          <el-option
            v-for="v in vehicleOptions"
            :key="v.id"
            :label="`#${v.id} ${v.brand} ${v.model} (${v.modelYear})`"
            :value="String(v.id)"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="材料配置" required>
        <div style="display:flex;flex-direction:column;gap:8px;width:100%;">
          <div v-for="(m, idx) in form.materials" :key="idx" style="display:flex;gap:8px;align-items:center;">
            <el-select v-model="m.materialType" filterable allow-create default-first-option style="width:320px;">
              <el-option v-for="opt in materialOptions" :key="opt.type" :label="opt.label" :value="opt.type" />
            </el-select>
            <el-select v-model="m.pricingMode" style="width:140px;">
              <el-option :value="PRICING_MODE_WEIGHT" label="按重量计价" />
              <el-option :value="PRICING_MODE_FIXED_TOTAL" label="固定总价" />
            </el-select>
            <el-input-number
              v-if="(m.pricingMode || PRICING_MODE_WEIGHT) === PRICING_MODE_WEIGHT && m.materialType.trim().toLowerCase() !== OTHERS"
              v-model="m.ratio"
              :min="0"
              :max="1"
              :precision="4"
              :step="0.01"
            />
            <el-input
              v-else-if="(m.pricingMode || PRICING_MODE_WEIGHT) === PRICING_MODE_WEIGHT"
              :model-value="othersRatio.toFixed(4)"
              disabled
            />
            <el-input-number
              v-else
              v-model="m.fixedTotalPrice"
              :min="0"
              :precision="2"
              :step="10"
              style="width:180px;"
            />
            <el-button type="danger" plain @click="removeMaterial(idx)">删除</el-button>
          </div>
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <el-button @click="addMaterial">新增材料</el-button>
            <span>按重量占比总和：{{ ratioSum.toFixed(4) }}<template v-if="hasOthers">（其余自动补齐：{{ othersRatio.toFixed(4) }}）</template></span>
          </div>
        </div>
      </el-form-item>
      <el-form-item label="回收系数" required>
        <el-input-number v-model="form.recoveryRatio" :min="0" :max="1" :precision="4" :step="0.01" />
      </el-form-item>
      <el-form-item label="其余单价覆盖">
        <el-input-number v-model="form.othersPricePerKgOverride" :min="0" :precision="2" :step="0.1" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>
