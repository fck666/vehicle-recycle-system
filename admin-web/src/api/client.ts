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
  const token = getToken()
  const headers: Record<string, string> = {}
  if (body) headers['Content-Type'] = 'application/json'
  if (token) headers['Authorization'] = `Bearer ${token}`

  const resp = await fetch(url, {
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
  const t = localStorage.getItem('admin_token')
  return t && t.trim() ? t : null
}

export function setToken(token: string | null) {
  if (!token || !token.trim()) {
    localStorage.removeItem('admin_token')
    return
  }
  localStorage.setItem('admin_token', token.trim())
}
