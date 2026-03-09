import { requestJson } from './client'
import type { Page, VehicleModel, VehicleUpsertRequest } from './types'

export interface VehicleSearchParams {
  q?: string
  brands?: string[]
  manufacturers?: string[]
  vehicleTypes?: string[]
  fuelTypes?: string[]
  sourceTypes?: string[]
  page?: number
  size?: number
  sort?: string
}

export interface VehicleFacets {
  brands: string[]
  manufacturers: string[]
  vehicleTypes: string[]
  fuelTypes: string[]
}

export async function searchVehicles(params: VehicleSearchParams): Promise<Page<VehicleModel>> {
  const query = new URLSearchParams()
  if (params.q) query.append('q', params.q)
  if (params.brands) params.brands.forEach(b => query.append('brands', b))
  if (params.manufacturers) params.manufacturers.forEach(m => query.append('manufacturers', m))
  if (params.vehicleTypes) params.vehicleTypes.forEach(t => query.append('vehicleTypes', t))
  if (params.fuelTypes) params.fuelTypes.forEach(f => query.append('fuelTypes', f))
  if (params.sourceTypes) params.sourceTypes.forEach(s => query.append('sourceTypes', s))
  
  query.append('page', (params.page || 0).toString())
  query.append('size', (params.size || 20).toString())
  if (params.sort) query.append('sort', params.sort)
  
  return requestJson<Page<VehicleModel>>('GET', `/api/admin/vehicles?${query.toString()}`)
}

export async function getVehicle(id: number): Promise<VehicleModel> {
  return requestJson<VehicleModel>('GET', `/api/admin/vehicles/${id}`)
}

export async function createVehicle(req: VehicleUpsertRequest): Promise<VehicleModel> {
  return requestJson<VehicleModel>('POST', '/api/admin/vehicles', req)
}

export async function updateVehicle(id: number, req: VehicleUpsertRequest): Promise<VehicleModel> {
  return requestJson<VehicleModel>('PUT', `/api/admin/vehicles/${id}`, req)
}

export async function deleteVehicle(id: number): Promise<void> {
  await requestJson('DELETE', `/api/admin/vehicles/${id}`)
}

export async function getVehicleFacets(): Promise<VehicleFacets> {
  return requestJson<VehicleFacets>('GET', '/api/admin/vehicles/facets')
}
