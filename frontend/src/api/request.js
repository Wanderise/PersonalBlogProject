import axios from 'axios'
import { useAuth } from '@/composables/useAuth.js'

export const API_BASE = (import.meta.env.VITE_API_BASE || 'http://localhost:8080').replace(/\/$/, '')

const service = axios.create({
  baseURL: API_BASE,
  timeout: 5000
})

service.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  return config
})

service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    return res
  },
  (error) => {
    if (error.response) {
      const { status } = error.response
      if (status === 401) {
        // 登录接口的 401 是凭证错误；其他接口的 401 表示当前登录态已失效。
        const isLoginRequest = error.config.url?.includes('/user/login')
        if (!isLoginRequest) {
          const { clearAuth } = useAuth()
          clearAuth()
          window.location.href = '/login'
        }
      } else if (status >= 500) {
        console.error('服务器内部错误')
      }
    }
    return Promise.reject(error)
  }
)

export default service
