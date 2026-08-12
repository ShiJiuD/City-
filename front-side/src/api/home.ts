import request from './request'
import type {
  ApiResponse,
  HomeData,
  Gallery,
  GalleryQuery,
  GalleryPageVO,
  GalleryPageParams,
} from '../types'

/** 获取首页聚合数据（Banner + 热门展览 + 热门美术馆） */
export function getHomeData(city?: string): Promise<ApiResponse<HomeData>> {
  return request.get('/api/app/home', { params: { city } }).then((res) => res.data)
}

/**
 * 获取美术馆列表（不分页，用于首页等场景）
 * @deprecated 美术馆列表页请使用分页接口 getGalleriesPage（来自 api/exhibition.ts）
 */
export function getGalleries(params?: GalleryQuery): Promise<ApiResponse<Gallery[]>> {
  return request.get('/api/app/galleries', { params }).then((res) => res.data)
}

/** 获取美术馆分页列表（无需登录），对应 04 文档 /api/app/galleries/page */
export function getGalleriesPage(
  params?: GalleryPageParams,
): Promise<ApiResponse<GalleryPageVO>> {
  return request.get('/api/app/galleries/page', { params }).then((res) => res.data)
}
