import axios from 'axios'
import { useAuth } from '@/composables/useAuth.js'

const service = axios.create({
  baseURL: 'http://localhost:8080/',
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
        // 只有登录接口的401才说明凭证失效，其他接口401是权限问题（如KB不属当前用户）
        const isAuthEndpoint = error.config.url?.includes('/user/login')
        if (isAuthEndpoint) {
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
