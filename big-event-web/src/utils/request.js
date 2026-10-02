import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/stores/user'

// 统一配置 Axios
const request = axios.create({
    baseURL: '/api', // 会被 vite.config.js 代理转发到 8080
    timeout: 5000
})

// 1. 请求拦截器
request.interceptors.request.use(
    config => {
        // 从本地存储获取 Token
        const token = localStorage.getItem('token')
        if (token) {
            config.headers.Authorization = token
        }
        return config
    },
    error => Promise.reject(error)
)

// 2. 响应拦截器
request.interceptors.response.use(
    result => {
        // 如果后端返回 code 0，代表成功，直接返回业务数据
        if (result.data.code === 0) {
            return result.data
        }
        // 如果返回 code 1，代表业务失败，直接弹窗警告
        ElMessage.error(result.data.message || '服务异常')
        return Promise.reject(result.data)
    },
    error => {
        // 401 = 未登录 / 登录已过期。必须【先清 token 再跳转】：
        // 否则路由守卫看到 token 仍在，会把 /login 弹回 '/'，形成无限重定向。
        if (error.response && error.response.status === 401) {
            const userStore = useUserStore()
            userStore.removeToken()
            userStore.removeUserInfo()
            ElMessage.error('登录已过期，请重新登录')
            router.push('/login')
        } else {
            ElMessage.error('服务异常')
        }
        return Promise.reject(error)
    }
)

export default request