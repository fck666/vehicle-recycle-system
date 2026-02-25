import { requestJson } from './client'
import type { AuthMeResponse, AuthLoginRequest, AuthLoginResponse } from './types_auth'

export async function login(payload: AuthLoginRequest): Promise<AuthLoginResponse> {
  return requestJson<AuthLoginResponse>('POST', '/api/auth/login', payload)
}

export async function me(): Promise<AuthMeResponse> {
  return requestJson<AuthMeResponse>('GET', '/api/auth/me')
}

export async function logout(): Promise<void> {
  await requestJson<void>('POST', '/api/auth/logout')
}
