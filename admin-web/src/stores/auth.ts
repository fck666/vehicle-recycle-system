import { defineStore } from 'pinia'
import { logout as apiLogout, me as fetchMe } from '../api/auth'
import type { AuthMeResponse } from '../api/types_auth'
import { getToken, setToken } from '../api/client'

export const useAuthStore = defineStore('auth', {
  state: (): { token: string | null; me: AuthMeResponse | null } => ({
    token: getToken(),
    me: null,
  }),
  actions: {
    setToken(token: string | null) {
      this.token = token
      setToken(token)
    },
    async loadMe() {
      if (!this.token) {
        this.me = null
        return
      }
      this.me = await fetchMe()
    },
    async logout() {
      try {
        await apiLogout()
      } catch {
      }
      this.setToken(null)
      this.me = null
    },
  },
})
