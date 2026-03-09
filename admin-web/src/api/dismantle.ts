import { requestJson } from './client'
import type { VehicleDismantleRecord } from './types'

export async function getDismantleRecords(vehicleId: number): Promise<VehicleDismantleRecord[]> {
  return requestJson<VehicleDismantleRecord[]>('GET', `/api/admin/vehicle-dismantle/vehicle/${vehicleId}`)
}

export async function createDismantleRecord(data: VehicleDismantleRecord): Promise<VehicleDismantleRecord> {
  return requestJson<VehicleDismantleRecord>('POST', '/api/admin/vehicle-dismantle', data)
}

export async function deleteDismantleRecord(id: number): Promise<void> {
  await requestJson('DELETE', `/api/admin/vehicle-dismantle/${id}`)
}
