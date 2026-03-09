import { requestJson } from './client'
import type { MaterialPrice, MaterialRatioItem, MaterialSourceConfig, MaterialSourceSuggestResult, MaterialTemplate } from './types'

export async function listMaterialPrices(): Promise<MaterialPrice[]> {
  return requestJson<MaterialPrice[]>('GET', '/api/material-prices')
}

export async function upsertMaterialPrice(payload: { type: string; pricePerKg: number; currency?: string; unit?: string; effectiveDate?: string }): Promise<MaterialPrice> {
  return requestJson<MaterialPrice>('POST', '/api/material-prices', payload)
}

export async function getMaterialPriceHistory(type: string, from: string, to: string): Promise<MaterialPrice[]> {
  const params = new URLSearchParams()
  params.set('from', from)
  params.set('to', to)
  return requestJson<MaterialPrice[]>('GET', `/api/material-prices/${encodeURIComponent(type)}/history?${params.toString()}`)
}

export async function listMaterialTemplates(): Promise<MaterialTemplate[]> {
  return requestJson<MaterialTemplate[]>('GET', '/api/material-templates')
}

export async function upsertMaterialTemplate(payload: {
  vehicleType: string
  recoveryRatio: number
  materials: MaterialRatioItem[]
}): Promise<MaterialTemplate> {
  return requestJson<MaterialTemplate>('POST', '/api/material-templates', payload)
}

export async function deleteMaterialTemplate(vehicleType: string): Promise<void> {
  await requestJson('DELETE', `/api/material-templates/${encodeURIComponent(vehicleType)}`)
}

export async function listMaterialSources(): Promise<MaterialSourceConfig[]> {
  return requestJson<MaterialSourceConfig[]>('GET', '/api/material-sources')
}

export async function suggestMaterialSources(keyword: string): Promise<MaterialSourceSuggestResult[]> {
  const params = new URLSearchParams()
  params.set('keyword', keyword)
  return requestJson<MaterialSourceSuggestResult[]>('GET', `/api/material-sources/suggest?${params.toString()}`)
}

export async function upsertMaterialSource(payload: {
  type: string
  displayName: string
  sourceName?: string
  sourceUrl: string
  parseKeyword?: string
  enabled?: boolean
}): Promise<MaterialSourceConfig> {
  return requestJson<MaterialSourceConfig>('POST', '/api/material-sources', payload)
}
