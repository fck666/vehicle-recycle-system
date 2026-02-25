import { requestJson } from './client'
import type { MaterialPrice, MaterialTemplate } from './types'

export async function listMaterialPrices(): Promise<MaterialPrice[]> {
  return requestJson<MaterialPrice[]>('GET', '/api/material-prices')
}

export async function upsertMaterialPrice(payload: { type: string; pricePerKg: number; currency?: string; unit?: string }): Promise<MaterialPrice> {
  return requestJson<MaterialPrice>('POST', '/api/material-prices', payload)
}

export async function listMaterialTemplates(): Promise<MaterialTemplate[]> {
  return requestJson<MaterialTemplate[]>('GET', '/api/material-templates')
}

export async function upsertMaterialTemplate(payload: {
  vehicleType: string
  steelRatio: number
  aluminumRatio: number
  copperRatio: number
  recoveryRatio: number
}): Promise<MaterialTemplate> {
  return requestJson<MaterialTemplate>('POST', '/api/material-templates', payload)
}

export async function deleteMaterialTemplate(vehicleType: string): Promise<void> {
  await requestJson('DELETE', `/api/material-templates/${encodeURIComponent(vehicleType)}`)
}
