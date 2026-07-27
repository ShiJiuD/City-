import request from './request'
import type { ApiResponse, UserProfile, AdminProfile } from '../types'

/** 获取C端用户个人信息（刷新页面时调用） */
export function getUserProfile(): Promise<ApiResponse<UserProfile>> {
  return request.get('/api/app/profile').then(res => res.data)
}

/** 获取B端管理员个人信息（刷新页面时调用） */
export function getAdminProfile(): Promise<ApiResponse<AdminProfile>> {
  return request.get('/api/admin/profile').then(res => res.data)
}