// ========== 后端统一响应 ==========
export interface ApiResponse<T = unknown> {
  code: number          // 1=成功, 0=失败
  msg: string
  data: T
}

// ========== 用户角色 ==========
export type Role = 'user' | 'admin'

// ========== C端用户信息 ==========
export interface UserProfile {
  id: number
  phone: string
  nickname: string
  status: number       // 0=正常, 1=禁用
  createTime: string
}

// ========== B端管理员信息 ==========
export interface AdminProfile {
  id: number
  phone: string
  name: string
  status: number       // 0=启用, 1=禁用
  createTime: string
}

// ========== 当前登录用户（内存态） ==========
export interface CurrentUser {
  id: number
  phone: string
  role: Role
  /** 管理员用 name，普通用户用 nickname */
  displayName: string
  status: number
}

// ========== 登录返回 ==========
export interface LoginData {
  id: number
  phone: string
  token: string
  nickname?: string    // 普通用户有
  name?: string        // 管理员有
}

// ========== DTO 请求体 ==========
export interface LoginDTO {
  phone: string
  password: string
}

export interface RegisterDTO {
  phone: string
  password: string
}

export interface SendCodeDTO {
  phone: string
  role: Role
}

export interface VerifyCodeDTO {
  phone: string
  code: string
  role: Role
}

export interface ResetPasswordDTO {
  phone: string
  role: Role
  password: string
}

// ========== 首页 ==========

/** 轮播图 Banner */
export interface Banner {
  id: number
  imageUrl: string
  title: string
}

/** 热门展览 */
export interface HotExhibition {
  id: number
  posterImage: string
  title: string
  subtitle: string
  galleryName: string
  type: number
}

/** 美术馆 */
export interface Gallery {
  id: number
  name: string
  coverImage: string
  address: string
  exhibitionCount: number
  type: number
}

/** 首页聚合数据 */
export interface HomeData {
  banners: Banner[]
  hotExhibitions: HotExhibition[]
  galleries: Gallery[]
}

/** 美术馆列表查询参数 */
export interface GalleryQuery {
  city?: string
  keyword?: string
}

// ========== 订单状态枚举 ==========
/** 订单状态：0=待支付, 1=已支付, 2=已取消, 3=已退款 */
export const OrderStatus = {
  PENDING: 0,   // 待支付
  PAID: 1,      // 已支付
  CANCELLED: 2, // 已取消
  REFUNDED: 3,  // 已退款
} as const

export type OrderStatusType = typeof OrderStatus[keyof typeof OrderStatus]

/** 订单状态标签映射 */
export const OrderStatusLabel: Record<number, string> = {
  [OrderStatus.PENDING]: '待支付',
  [OrderStatus.PAID]: '已支付',
  [OrderStatus.CANCELLED]: '已取消',
  [OrderStatus.REFUNDED]: '已退款',
}

// ========== 展览类型枚举 ==========
/** 展览类型 */
export const ExhibitionType: Record<number, string> = {
  1: '当代展览',
  2: '古典展览',
  3: '雕塑展览',
  4: '摄影展览',
}

// ========== 美术馆类型枚举 ==========
/** 美术馆类型 */
export const GalleryType: Record<number, string> = {
  1: '综合美术馆',
  2: '当代美术馆',
  3: '古典美术馆',
  4: '雕塑美术馆',
  5: '摄影美术馆',
}

// ========== 订单 ==========

/** 订单明细项（接口返回） */
export interface OrderItemVO {
  exhibitionId: number
  exhibitionTitle: string
  posterImage: string
  visitDate: string
  ticketType: string
  quantity: number
  unitPrice: number
}

/** 订单（列表/详情返回） */
export interface OrderVO {
  orderNo: string
  status: OrderStatusType
  totalAmount: number
  createTime: string
  payTime: string | null
  cancelTime: string | null
  refundTime: string | null
  items: OrderItemVO[]
}

/** 订单分页数据 */
export interface OrderPageVO {
  total: number
  pages: number
  current: number
  size: number
  records: OrderVO[]
}

/** 订单列表查询参数 */
export interface OrderListParams {
  status?: OrderStatusType
  page?: number
  size?: number
}

/** 创建订单 - 明细项 */
export interface CreateOrderItem {
  exhibitionId: number
  ticketType: string
  quantity: number
  visitDate: string
}

/** 创建订单 - 请求体 */
export interface CreateOrderDTO {
  items: CreateOrderItem[]
}

/** 创建订单 - 返回的明细项（不含 exhibitionTitle 和 posterImage） */
export interface CreateOrderItemVO {
  exhibitionId: number
  ticketType: string
  quantity: number
  unitPrice: number
  visitDate: string
}

/** 创建订单 - 返回 */
export interface CreateOrderVO {
  orderNo: string
  totalAmount: number
  status: OrderStatusType
  createTime: string
  items: CreateOrderItemVO[]
}

// ========== 展览列表 ==========

/** 展览列表项（当前/即将/往期 接口返回） */
export interface ExhibitionListItem {
  id: number
  posterImage: string
  title: string
  subtitle: string | null
  galleryName: string
  startDate: string
  endDate: string
  type: number
  price: number
}

/** 展览列表查询参数 */
export interface ExhibitionListParams {
  keyword?: string
  type?: number
  city?: string
}

// ========== 美术馆分页 ==========

/** 美术馆分页项 */
export interface GalleryPageItem {
  id: number
  name: string
  coverImage: string
  address: string
  exhibitionCount: number
  type: number
}

/** 美术馆分页数据 */
export interface GalleryPageVO {
  records: GalleryPageItem[]
  total: number
  size: number
  current: number
  pages: number
}

/** 美术馆分页查询参数 */
export interface GalleryPageParams {
  pageNum?: number
  pageSize?: number
  keyword?: string
  type?: number
  city?: string
}