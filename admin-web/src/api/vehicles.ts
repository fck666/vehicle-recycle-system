import { requestJson } from './client'
import type { Page, VehicleModel, VehicleUpsertRequest } from './types'

export async function searchVehicles(q: string, page: number, size: number): Promise<Page<VehicleModel>> {
  const params = new URLSearchParams()
  if (q.trim()) params.set('q', q.trim())
  params.set('page', String(page))
  params.set('size', String(size))
  return requestJson<Page<VehicleModel>>('GET', `/api/admin/vehicles?${params.toString()}`)
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
