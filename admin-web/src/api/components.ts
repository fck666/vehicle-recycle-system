import { requestJson } from './client'
import type { ComponentDict } from './types'

export async function listComponents(): Promise<ComponentDict[]> {
  return requestJson('GET', '/api/admin/components')
}

export async function createComponent(payload: Partial<ComponentDict>): Promise<ComponentDict> {
  return requestJson('POST', '/api/admin/components', payload)
}

export async function updateComponent(id: number, payload: Partial<ComponentDict>): Promise<ComponentDict> {
  return requestJson('PUT', `/api/admin/components/${id}`, payload)
}

export async function deleteComponent(id: number): Promise<void> {
  return requestJson('DELETE', `/api/admin/components/${id}`)
}