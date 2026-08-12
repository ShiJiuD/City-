import request from './request'
import type { ApiResponse } from '../types'

/** 收藏列表项（后端 FavoriteListVO） */
export interface FavoriteListVO {
  id: number
  targetType: number    // 1=展览 2=美术馆
  targetId: number
  title?: string        // 展览标题（type=1）
  galleryName?: string  // 美术馆名称（type=1 联表）
  name?: string         // 美术馆名称（type=2）
  coverImage: string    // 封面图
  address?: string      // 地址（type=2）
  exhibitionCount?: number // 在展数量（type=2）
  startDate?: string    // 开始日期（type=1）
  endDate?: string      // 结束日期（type=1）
  price?: number        // 票价（type=1）
  favoriteTime: string  // 收藏时间
}

/** 收藏分页 */
export interface FavoritePageVO {
  total: number
  pages: number
  current: number
  size: number
  records: FavoriteListVO[]
}

/** 收藏数量（后端 FavoriteCountVO） */
export interface FavoriteCountVO {
  exhibitionCount: number
  galleryCount: number
  totalCount: number
}

/** 收藏检查（后端 FavoriteCheckVO） */
export interface FavoriteCheckVO {
  isFavorited: boolean
}

/** 添加/取消收藏 DTO */
export interface FavoriteDTO {
  targetType: number
  targetId: number
}

/** 获取收藏列表 */
export function getFavoriteList(params: {
  targetType: number
  keyword?: string
  page?: number
  pageSize?: number
}): Promise<ApiResponse<FavoritePageVO>> {
  return request.get('/api/app/favorite/list', { params }).then((res) => res.data)
}

/** 收藏数量统计 */
export function getFavoriteCount(): Promise<ApiResponse<FavoriteCountVO>> {
  return request.get('/api/app/favorite/count').then((res) => res.data)
}

/** 添加收藏 */
export function addFavorite(data: FavoriteDTO): Promise<ApiResponse<null>> {
  return request.post('/api/app/favorite', data).then((res) => res.data)
}

/** 取消收藏 */
export function cancelFavorite(data: FavoriteDTO): Promise<ApiResponse<null>> {
  return request.delete('/api/app/favorite', { data }).then((res) => res.data)
}

/** 检查收藏状态 */
export function checkFavorite(params: {
  targetType: number
  targetId: number
}): Promise<ApiResponse<FavoriteCheckVO>> {
  return request.get('/api/app/favorite/check', { params }).then((res) => res.data)
}
