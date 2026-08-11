import request from './request'
import type {
  ApiResponse,
  ExhibitionListItem,
  ExhibitionListParams,
  GalleryPageVO,
  GalleryPageParams,
} from '../types'

// ===== 展览与美术馆接口 =====
// 对应文档：04展览展馆列表接口文档.md

// ========== 展览 ==========

/** 正在展出列表（start_date <= 今天 <= end_date），无需登录 */
export function getCurrentExhibitions(
  params?: ExhibitionListParams,
): Promise<ApiResponse<ExhibitionListItem[]>> {
  return request.get('/api/app/exhibitions/current', { params }).then((res) => res.data)
}

/** 即将展出列表（start_date > 今天），无需登录 */
export function getFutureExhibitions(
  params?: ExhibitionListParams,
): Promise<ApiResponse<ExhibitionListItem[]>> {
  return request.get('/api/app/exhibitions/future', { params }).then((res) => res.data)
}

/** 往期展出列表（end_date < 今天），无需登录 */
export function getPastExhibitions(
  params?: ExhibitionListParams,
): Promise<ApiResponse<ExhibitionListItem[]>> {
  return request.get('/api/app/exhibitions/past', { params }).then((res) => res.data)
}

// ========== 美术馆 ==========

/** 美术馆分页列表（仅返回营业中 status=0 的美术馆，按展览数量降序），无需登录 */
export function getGalleriesPage(
  params?: GalleryPageParams,
): Promise<ApiResponse<GalleryPageVO>> {
  return request.get('/api/app/galleries/page', { params }).then((res) => res.data)
}

// ========== 详情 ==========

/** 展馆详情 */
export interface GalleryExhibitionItem {
  id: number
  title: string
  posterImage: string
  startDate: string
  endDate: string
  price: number
}

export interface GalleryDetailVO {
  id: number
  name: string
  coverImage: string
  address: string
  intro: string
  exhibitionCount: number
  currentExhibitions: GalleryExhibitionItem[]
  pastExhibitions: GalleryExhibitionItem[]
}

export function getGalleryDetail(id: number): Promise<ApiResponse<GalleryDetailVO>> {
  return request.get(`/api/detail/gallery/${id}`).then((res) => res.data)
}

// ========== 展览详情 ==========

export interface ExhibitionWorkVO {
  id: number
  title: string
  artist: string
  image: string
  description: string
}

export interface ExhibitionDetailVO {
  id: number
  title: string
  subtitle: string
  posterImage: string
  galleryName: string
  galleryAddress: string
  startDate: string
  endDate: string
  price: number
  description: string
  works: ExhibitionWorkVO[]
}

export function getExhibitionDetail(id: number): Promise<ApiResponse<ExhibitionDetailVO>> {
  return request.get(`/api/detail/exhibition/${id}`).then((res) => res.data)
}
