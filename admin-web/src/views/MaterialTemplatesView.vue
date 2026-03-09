<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useAuthStore } from '../stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { MaterialRatioItem, MaterialTemplate } from '../api/types'
import { deleteMaterialTemplate, listMaterialPrices, listMaterialSources, listMaterialTemplates, upsertMaterialTemplate } from '../api/material'
import { getVehicleFacets } from '../api/vehicles'

const OTHERS = 'others'

const loading = ref(false)
const items = ref<MaterialTemplate[]>([])
const materialOptions = ref<Array<{ type: string; label: string }>>([])
const vehicleTypeOptions = ref<string[]>([])

const dialogVisible = ref(false)
const form = reactive<{
  vehicleType: string
  materials: MaterialRatioItem[]
  recoveryRatio: number | null
}>({ vehicleType: '', materials: [], recoveryRatio: null })

const auth = useAuthStore()
const canEdit = computed(() => (auth.me?.roles ?? []).some(r => ['ADMIN', 'OPERATOR'].includes(r)))

async function load() {
  loading.value = true
  try {
    const [templates, prices, sources, facets] = await Promise.all([
      listMaterialTemplates(),
      listMaterialPrices(),
      listMaterialSources(),
      getVehicleFacets(),
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
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.vehicleType = ''
  form.materials = []
  form.recoveryRatio = null
  dialogVisible.value = true
}

function openEdit(row: MaterialTemplate) {
  form.vehicleType = row.vehicleType
  form.materials = (row.materials || []).map(m => ({ materialType: m.materialType, ratio: m.ratio }))
  form.recoveryRatio = row.recoveryRatio
  dialogVisible.value = true
}

function addMaterial() {
  form.materials.push({ materialType: '', ratio: 0 })
}

function removeMaterial(index: number) {
  form.materials.splice(index, 1)
}

const ratioSum = computed(() => form.materials.reduce((sum, it) => sum + (it.ratio || 0), 0))
const ratioWithoutOthers = computed(() =>
  form.materials
    .filter(m => m.materialType.trim().toLowerCase() !== OTHERS)
    .reduce((sum, it) => sum + (it.ratio || 0), 0),
)
const hasOthers = computed(() => form.materials.some(m => m.materialType.trim().toLowerCase() === OTHERS))
const othersRatio = computed(() => Math.max(0, Number((1 - ratioWithoutOthers.value).toFixed(4))))

async function submit() {
  const normalized = form.materials
    .filter(m => m.materialType.trim() && m.ratio != null && m.ratio >= 0)
    .map(m => ({ materialType: m.materialType.trim().toLowerCase(), ratio: m.ratio }))
  const dedup = new Map<string, number>()
  normalized.forEach(m => dedup.set(m.materialType, m.ratio))
  const materialPayload = [...dedup.entries()].map(([materialType, ratio]) => ({ materialType, ratio }))
  const withoutOthers = materialPayload.filter(x => x.materialType !== OTHERS)
  const sumWithoutOthers = withoutOthers.reduce((acc, x) => acc + x.ratio, 0)
  if (sumWithoutOthers > 1) {
    ElMessage.warning('除其余外的材料占比总和不能超过1')
    return
  }
  const hasOthersInPayload = materialPayload.some(x => x.materialType === OTHERS)
  const finalPayload = hasOthersInPayload
    ? [...withoutOthers, ...(othersRatio.value > 0 ? [{ materialType: OTHERS, ratio: othersRatio.value }] : [])]
    : materialPayload
  const sum = finalPayload.reduce((acc, x) => acc + x.ratio, 0)
  if (!form.vehicleType.trim() || form.recoveryRatio == null || finalPayload.length === 0 || sum <= 0 || sum > 1) {
    ElMessage.warning('请检查车型、材料占比，且占比总和需大于0且不超过1')
    return
  }
  await upsertMaterialTemplate({
    vehicleType: form.vehicleType.trim(),
    recoveryRatio: form.recoveryRatio,
    materials: finalPayload,
  })
  ElMessage.success('已保存')
  dialogVisible.value = false
  load()
}

async function onDelete(row: MaterialTemplate) {
  await ElMessageBox.confirm(`确认删除模板 ${row.vehicleType} 吗？`, '删除确认', { type: 'warning' })
  await deleteMaterialTemplate(row.vehicleType)
  ElMessage.success('已删除')
  load()
}

function materialLabel(type: string) {
  const hit = materialOptions.value.find(x => x.type === type)
  return hit ? hit.label : type
}

function materialText(row: MaterialTemplate) {
  return (row.materials || []).map(m => `${materialLabel(m.materialType)}:${m.ratio}`).join('，')
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
      <el-table-column prop="vehicleType" label="车型类型" width="160" />
      <el-table-column label="材料占比" min-width="380">
        <template #default="{ row }">{{ materialText(row) }}</template>
      </el-table-column>
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
      <el-form-item label="车型类型" required>
        <el-select v-model="form.vehicleType" filterable allow-create default-first-option style="width:100%;">
          <el-option v-for="v in vehicleTypeOptions" :key="v" :label="v" :value="v" />
        </el-select>
      </el-form-item>
      <el-form-item label="材料占比" required>
        <div style="display:flex;flex-direction:column;gap:8px;width:100%;">
          <div v-for="(m, idx) in form.materials" :key="idx" style="display:flex;gap:8px;align-items:center;">
            <el-select v-model="m.materialType" filterable allow-create default-first-option style="width:320px;">
              <el-option v-for="opt in materialOptions" :key="opt.type" :label="opt.label" :value="opt.type" />
            </el-select>
            <el-input-number
              v-if="m.materialType.trim().toLowerCase() !== OTHERS"
              v-model="m.ratio"
              :min="0"
              :max="1"
              :precision="4"
              :step="0.01"
            />
            <el-input v-else :model-value="othersRatio.toFixed(4)" disabled />
            <el-button type="danger" plain @click="removeMaterial(idx)">删除</el-button>
          </div>
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <el-button @click="addMaterial">新增材料</el-button>
            <span>占比总和：{{ ratioSum.toFixed(4) }}<template v-if="hasOthers">（其余自动补齐：{{ othersRatio.toFixed(4) }}）</template></span>
          </div>
        </div>
      </el-form-item>
      <el-form-item label="回收系数" required>
        <el-input-number v-model="form.recoveryRatio" :min="0" :max="1" :precision="4" :step="0.01" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>
