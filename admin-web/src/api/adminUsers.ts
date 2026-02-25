import { requestJson } from './client'
import type { Page } from './types'

export interface AdminUser {
  id: number
  username?: string | null
  phone?: string | null
  wxOpenid?: string | null
  wxUnionid?: string | null
  status: string
  roles: string[]
  createdAt?: string | null
  updatedAt?: string | null
}

export interface AdminUserCreateRequest {
  username: string
  password: string
  phone?: string | null
  status?: string | null
  roles?: string[]
}

export interface AdminUserUpdateRequest {
  username?: string | null
  phone?: string | null
  status?: string | null
}

export async function searchUsers(q: string, page: number, size: number): Promise<Page<AdminUser>> {
  const params = new URLSearchParams()
  if (q.trim()) params.set('q', q.trim())
  params.set('page', String(page))
  params.set('size', String(size))
  return requestJson<Page<AdminUser>>('GET', `/api/admin/users?${params.toString()}`)
}

export async function createUser(payload: AdminUserCreateRequest): Promise<AdminUser> {
  return requestJson<AdminUser>('POST', '/api/admin/users', payload)
}

export async function updateUser(id: number, payload: AdminUserUpdateRequest): Promise<AdminUser> {
  return requestJson<AdminUser>('PUT', `/api/admin/users/${id}`, payload)
}

export async function updateUserRoles(id: number, roles: string[]): Promise<AdminUser> {
  return requestJson<AdminUser>('PUT', `/api/admin/users/${id}/roles`, { roles })
}

export async function resetUserPassword(id: number, password: string): Promise<void> {
  await requestJson<void>('POST', `/api/admin/users/${id}/reset-password`, { password })
}

export async function revokeUserSessions(id: number): Promise<void> {
  await requestJson<void>('POST', `/api/admin/users/${id}/sessions/revoke`)
}

