import { requestJson } from './client'
import type { Page } from './types'

export type MappingStatus = 'UNMAPPED' | 'SUGGESTED' | 'CONFIRMED'

export interface VehicleMappingCandidate {
  externalTrimId: number
  source: string
  sourceTrimId: string
  brand: string
  seriesName?: string | null
  marketName?: string | null
  modelYear?: number | null
  energyType?: string | null
  coverUrl?: string | null
  pageUrl?: string | null
  score: number
  rankNo: number
}

export interface VehicleMappingRow {
  miitVehicleId: number
  brand: string
  model: string
  modelYear: number
  fuelType: string
  batteryKwh?: number | null
  curbWeight?: number | null
  productId?: string | null
  productNo?: string | null
  status: MappingStatus
  externalTrimId?: number | null
  externalMarketName?: string | null
  externalPageUrl?: string | null
  externalCoverUrl?: string | null
  candidates: VehicleMappingCandidate[]
}

export interface ExternalTrim {
  id: number
  source: string
  sourceTrimId: string
  brand: string
  seriesName?: string | null
  marketName?: string | null
  modelYear?: number | null
  energyType?: string | null
  officialPrice?: number | null
  coverUrl?: string | null
  pageUrl?: string | null
}

export async function listVehicleMappings(q: string, status: string, page: number, size: number): Promise<Page<VehicleMappingRow>> {
  const params = new URLSearchParams()
  if (q.trim()) params.set('q', q.trim())
  if (status.trim()) params.set('status', status.trim())
  params.set('page', String(page))
  params.set('size', String(size))
  return requestJson<Page<VehicleMappingRow>>('GET', `/api/admin/vehicle-mappings?${params.toString()}`)
}

export async function recomputeVehicleMapping(vehicleId: number): Promise<void> {
  await requestJson<void>('POST', `/api/admin/vehicle-mappings/${vehicleId}/recompute`)
}

export async function recomputeVehicleMappings(limit = 200): Promise<void> {
  await requestJson<void>('POST', `/api/admin/vehicle-mappings/recompute?limit=${encodeURIComponent(String(limit))}`)
}

export async function confirmVehicleMapping(vehicleId: number, externalTrimId: number): Promise<void> {
  await requestJson<void>('POST', `/api/admin/vehicle-mappings/${vehicleId}/confirm`, { externalTrimId })
}

export async function searchExternalTrims(q: string, page: number, size: number): Promise<Page<ExternalTrim>> {
  const params = new URLSearchParams()
  if (q.trim()) params.set('q', q.trim())
  params.set('page', String(page))
  params.set('size', String(size))
  return requestJson<Page<ExternalTrim>>('GET', `/api/admin/external-trims?${params.toString()}`)
}

