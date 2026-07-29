import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { CurrentUser, Role } from '../types'
import { loginUser, loginAdmin } from '../api/auth'
import { getUserProfile, getAdminProfile } from '../api/profile'

export const useAuthStore = defineStore('auth', () => {
  // ===== 状态（只放内存，不持久化！） =====
  const user = ref<CurrentUser | null>(null)
  const token = ref<string>(localStorage.getItem('token') || '')

  // ===== 计算属性 =====
  const isLoggedIn = computed(() => !!token.value && !!user.value)
  const isAdmin = computed(() => user.value?.role === 'admin')

  // ===== 登录 =====
  async function login(role: Role, phone: string, password: string) {
    let data
    if (role === 'admin') {
      const res = await loginAdmin({ phone, password })
      data = res.data
    } else {
      const res = await loginUser({ phone, password })
      data = res.data
    }

    // 1. 存 token 到 localStorage
    localStorage.setItem('token', data.token)
    token.value = data.token

    // 2. 用户信息只放内存
    user.value = {
      id: data.id,
      phone: data.phone,
      role,
      displayName: data.nickname || data.name || '',
      status: 0,
    }
  }

  // ===== 刷新用户信息（页面刷新/重新打开时调用） =====
  async function refreshProfile() {
    const savedToken = localStorage.getItem('token')
    if (!savedToken) return

    token.value = savedToken


    const role = (localStorage.getItem('role') || 'user') as Role

    try {
      if (role === 'admin') {
        const res = await getAdminProfile()
        const profile = res.data
        if (profile.status !== 0) {
          logout(false)
          throw new Error('您的账号已被禁用，请联系管理员')
        }
        user.value = {
          id: profile.id,
          phone: profile.phone,
          role: 'admin',
          displayName: profile.name,
          status: profile.status,
        }
      } else {
        const res = await getUserProfile()
        const profile = res.data
        if (profile.status !== 0) {
          logout(false)
          throw new Error('您的账号已被禁用，请联系管理员')
        }
        user.value = {
          id: profile.id,
          phone: profile.phone,
          role: 'user',
          displayName: profile.nickname,
          status: profile.status,
        }
      }
    } catch {
      // profile 调用失败 → token 无效或账号被封 → 清空并跳登录
      logout(false)
      throw new Error('登录已过期，请重新登录')
    }
  }

  // ===== 退出登录 =====
  function logout(redirect = true) {
    localStorage.removeItem('token')
    localStorage.removeItem('role')
    token.value = ''
    user.value = null
    if (redirect) {
      window.location.href = '/#/login'
    }
  }

  return { user, token, isLoggedIn, isAdmin, login, refreshProfile, logout }
})