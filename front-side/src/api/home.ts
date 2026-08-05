import request from './request'
import type { ApiResponse, HomeData, Gallery, GalleryQuery } from '../types'

/** 获取首页聚合数据（Banner + 热门展览 + 热门美术馆） */
export function getHomeData(city?: string): Promise<ApiResponse<HomeData>> {
  return request.get('/api/app/home', { params: { city } }).then(res => res.data)
}

/** 获取美术馆列表（支持城市筛选和名称搜索） */
export function getGalleries(params?: GalleryQuery): Promise<ApiResponse<Gallery[]>> {
  return request.get('/api/app/galleries', { params }).then(res => res.data)
}
