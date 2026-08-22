import request from './request'
import type { ApiResponse, RecommendExhibition } from '../types'

/**
 * 获取首页“艺览智荐”列表。
 * 登录用户由请求拦截器自动携带 token，游客则由后端降级为热门推荐。
 */
export function getRecommendations(limit = 4): Promise<ApiResponse<RecommendExhibition[]>> {
  return request.get('/api/app/recommend', { params: { limit } }).then((res) => res.data)
}
