import request from './request'
import type {
  ApiResponse,
  OrderPageVO,
  OrderVO,
  OrderListParams,
  CreateOrderDTO,
  CreateOrderVO,
} from '../types'

// ===== 订单模块接口 =====
// 对应文档：03订单接口文档.md

/** 获取我的订单列表（分页 + 状态筛选） */
export function getOrders(params?: OrderListParams): Promise<ApiResponse<OrderPageVO>> {
  return request.get('/api/app/orders', { params }).then((res) => res.data)
}

/** 获取订单详情（根据订单编号） */
export function getOrderDetail(orderNo: string): Promise<ApiResponse<OrderVO>> {
  return request.get(`/api/app/orders/${orderNo}`).then((res) => res.data)
}

/** 创建订单（提交下单） */
export function createOrder(data: CreateOrderDTO): Promise<ApiResponse<CreateOrderVO>> {
  return request.post('/api/app/orders', data).then((res) => res.data)
}

/** 取消订单（仅待支付状态可取消） */
export function cancelOrder(orderNo: string): Promise<ApiResponse<null>> {
  return request.put(`/api/app/orders/${orderNo}/cancel`).then((res) => res.data)
}

/** 申请退款（仅已支付状态可申请） */
export function refundOrder(orderNo: string): Promise<ApiResponse<null>> {
  return request.put(`/api/app/orders/${orderNo}/refund`).then((res) => res.data)
}
