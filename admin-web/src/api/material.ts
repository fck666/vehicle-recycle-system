import { requestJson, getToken } from './client'
import type { MaterialPrice, MaterialSourceConfig, MaterialSourceSuggestResult } from './types'

export async function listMaterialPrices(): Promise<MaterialPrice[]> {
  return requestJson<MaterialPrice[]>('GET', '/api/material-prices')
}

export async function listRecyclePrices(): Promise<MaterialPrice[]> {
  return requestJson<MaterialPrice[]>('GET', '/api/admin/recycle-prices')
}

export async function listRecycleMaterialTypes(): Promise<string[]> {
  return requestJson<string[]>('GET', '/api/admin/recycle-prices/types')
}

export async function importRecyclePrices(file: File): Promise<void> {
  const formData = new FormData()
  formData.append('file', file)
  
  const token = getToken()
  const res = await fetch('/api/admin/recycle-prices/import', {
    method: 'POST',
    headers: token ? { 'Authorization': `Bearer ${token}` } : undefined,
    body: formData
  })
  
  if (!res.ok) {
    const text = await res.text()
    throw new Error(text || `Upload failed: ${res.status}`)
  }
}

export async function upsertRecyclePrice(payload: { materialName: string; price: number; unit?: string }): Promise<void> {
  const token = getToken()
  const res = await fetch('/api/admin/recycle-prices', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { 'Authorization': `Bearer ${token}` } : {})
    },
    body: JSON.stringify(payload)
  })

  if (!res.ok) {
    const text = await res.text()
    throw new Error(text || `Save failed: ${res.status}`)
  }
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
