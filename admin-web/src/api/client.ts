export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'

export class HttpError extends Error {
  status: number
  body: unknown

  constructor(message: string, status: number, body: unknown) {
    super(message)
    this.status = status
    this.body = body
  }
}

export async function requestJson<T>(method: HttpMethod, url: string, body?: unknown): Promise<T> {
  // In production, Nginx will proxy /api to backend. In dev, Vite proxy handles it.
  // So we can just use relative path or respect VITE_API_BASE_URL if needed.
  // For now, we assume relative path works fine with proxy.
  
  const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
  const fullUrl = url.startsWith('http') ? url : `${baseUrl}${url}`

  const token = getToken()
  const headers: Record<string, string> = {}
  if (body) headers['Content-Type'] = 'application/json'
  if (token) headers['Authorization'] = `Bearer ${token}`

  const resp = await fetch(fullUrl, {
    method,
    headers: Object.keys(headers).length ? headers : undefined,
    body: body ? JSON.stringify(body) : undefined,
  })

  const text = await resp.text()
  const parsed = text ? safeJsonParse(text) : null
  if (!resp.ok) {
    throw new HttpError(`HTTP ${resp.status}`, resp.status, parsed)
  }
  return parsed as T
}

function safeJsonParse(text: string): unknown {
  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}

export function getToken(): string | null {
  const t = localStorage.getItem('token') || localStorage.getItem('admin_token')
  return t && t.trim() ? t : null
}

export function setToken(token: string | null) {
  if (!token || !token.trim()) {
    localStorage.removeItem('token')
    localStorage.removeItem('admin_token')
    return
  }
  localStorage.setItem('token', token.trim())
}
