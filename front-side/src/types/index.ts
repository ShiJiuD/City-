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