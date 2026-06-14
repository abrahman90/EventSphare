import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '../api'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(JSON.parse(localStorage.getItem('es_user') || 'null'))
  const token = ref(localStorage.getItem('es_token') || null)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const isOrganizer = computed(() => user.value?.role === 'ORGANIZER')

  async function login(credentials) {
    const { data } = await authApi.login(credentials)
    const { token: tok, ...userData } = data.data
    token.value = tok
    user.value = userData
    localStorage.setItem('es_token', tok)
    localStorage.setItem('es_user', JSON.stringify(userData))
    return userData
  }

  async function register(payload) {
    const { data } = await authApi.register(payload)
    return data
  }

  async function fetchMe() {
    const { data } = await authApi.me()
    user.value = data.data
    localStorage.setItem('es_user', JSON.stringify(data.data))
    return data.data
  }

  function logout() {
    token.value = null
    user.value = null
    localStorage.removeItem('es_token')
    localStorage.removeItem('es_user')
  }

  return { user, token, isLoggedIn, isAdmin, isOrganizer, login, register, fetchMe, logout }
})
