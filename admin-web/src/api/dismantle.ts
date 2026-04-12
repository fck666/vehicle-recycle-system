import { requestJson } from './client'
import type { VehicleDismantleRecord } from './types'

export async function getDismantleRecords(vehicleId: number): Promise<VehicleDismantleRecord[]> {
  return requestJson<VehicleDismantleRecord[]>('GET', `/api/admin/vehicle-dismantle/vehicle/${vehicleId}`)
}

export async function createDismantleRecord(payload: Partial<VehicleDismantleRecord>): Promise<VehicleDismantleRecord> {
  return requestJson<VehicleDismantleRecord>('POST', '/api/admin/vehicle-dismantle', payload)
}

export async function updateDismantleRecord(id: number, payload: Partial<VehicleDismantleRecord>): Promise<VehicleDismantleRecord> {
  return requestJson<VehicleDismantleRecord>('PUT', `/api/admin/vehicle-dismantle/${id}`, payload)
}

export async function deleteDismantleRecord(id: number): Promise<void> {
  await requestJson('DELETE', `/api/admin/vehicle-dismantle/${id}`)
}
