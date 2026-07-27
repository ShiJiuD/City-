import request from './request'
import type { ApiResponse, LoginData, LoginDTO, RegisterDTO, SendCodeDTO, VerifyCodeDTO, ResetPasswordDTO } from '../types'

// ===== C端用户 =====

/** 用户注册 */
export function registerUser(data: RegisterDTO): Promise<ApiResponse<LoginData>> {
  return request.post('/api/app/register', data).then(res => res.data)
}

/** 用户登录 */
export function loginUser(data: LoginDTO): Promise<ApiResponse<LoginData>> {
  return request.post('/api/app/login', data).then(res => res.data)
}

// ===== B端管理员 =====

/** 管理员登录 */
export function loginAdmin(data: LoginDTO): Promise<ApiResponse<LoginData>> {
  return request.post('/api/admin/login', data).then(res => res.data)
}

// ===== 忘记密码（公共） =====

/** 发送验证码 */
export function sendCode(data: SendCodeDTO): Promise<ApiResponse<null>> {
  return request.post('/api/send-code', data).then(res => res.data)
}

/** 校验验证码 */
export function verifyCode(data: VerifyCodeDTO): Promise<ApiResponse<null>> {
  return request.post('/api/verify-code', data).then(res => res.data)
}

/** 重置密码 */
export function resetPassword(data: ResetPasswordDTO): Promise<ApiResponse<null>> {
  return request.put('/api/reset-password', data).then(res => res.data)
}