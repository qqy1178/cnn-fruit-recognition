import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: '/api',
  // ⚠️ 识别要 5-8 秒（Python 每次重新加载模型），超时必须给足，
  //    否则前端会先超时、后端还在算，用户看到"失败"但结果其实已经入库了。
  timeout: 120000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

request.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body.code === 200) return body.data
    ElMessage.error(body.msg || '请求失败')
    return Promise.reject(new Error(body.msg || '请求失败'))
  },
  (err) => {
    const status = err.response?.status
    const msg = err.response?.data?.msg
    if (status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      router.push('/login')
      ElMessage.error(msg || '登录已过期，请重新登录')
    } else {
      ElMessage.error(msg || '网络错误，请检查后端是否启动')
    }
    return Promise.reject(err)
  }
)

export default request
