import { requestJson } from './client'
import type { VehicleDocument, VehicleImage } from './types'

export async function updateVehicleImage(vehicleId: number, imageId: number, payload: { imageName?: string | null; sortOrder?: number | null }): Promise<VehicleImage> {
  return requestJson<VehicleImage>('PUT', `/api/admin/vehicles/${vehicleId}/images/${imageId}`, payload)
}

export async function deleteVehicleImage(vehicleId: number, imageId: number): Promise<void> {
  await requestJson<void>('DELETE', `/api/admin/vehicles/${vehicleId}/images/${imageId}`)
}

export async function createVehicleDocument(vehicleId: number, payload: { docType?: string | null; docName?: string | null; docUrl: string; sha256?: string | null; sourceUrl?: string | null; fetchedAt?: string | null }): Promise<VehicleDocument> {
  return requestJson<VehicleDocument>('POST', `/api/admin/vehicles/${vehicleId}/documents`, payload)
}

export async function deleteVehicleDocument(vehicleId: number, docId: number): Promise<void> {
  await requestJson<void>('DELETE', `/api/admin/vehicles/${vehicleId}/documents/${docId}`)
}

export async function getSignedUrl(url: string): Promise<string> {
  return requestJson<string>('GET', `/api/admin/vehicles/media/sign?url=${encodeURIComponent(url)}`)
}

export async function getHtmlContent(url: string): Promise<string> {
  return requestJson<string>('GET', `/api/admin/vehicles/media/html?url=${encodeURIComponent(url)}`)
}
