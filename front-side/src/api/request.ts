import axios from 'axios'

const request = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000,
})

// ===== 请求拦截器：自动带 token =====
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ===== 响应拦截器：统一错误处理 =====
request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body.code !== 1) {
      return Promise.reject(new Error(body.msg || '请求失败'))
    }
    return response
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      if (window.location.hash !== '#/login') {
        window.location.href = '/#/login'
      }
    }
    return Promise.reject(error)
  }
)

export default request