export interface AuthLoginRequest {
  username: string
  password: string
}

export interface AuthLoginResponse {
  token: string
  userId: number
  username: string
  roles: string[]
}

export interface AuthMeResponse {
  userId: number
  username: string
  roles: string[]
}
