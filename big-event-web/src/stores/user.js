import { defineStore } from 'pinia'
import { ref } from 'vue'

// 🌟 重点：前面必须写 export 关键字，且名称必须是 useUserStore
export const useUserStore = defineStore('user', () => {
    const token = ref(localStorage.getItem('token') || '')
    const userInfo = ref({}) // 存放用户详细资料

    const setToken = (newToken) => {
        token.value = newToken
        localStorage.setItem('token', newToken)
    }

    const removeToken = () => {
        token.value = ''
        localStorage.removeItem('token')
    }

    const setUserInfo = (newInfo) => {
        userInfo.value = newInfo
    }

    const removeUserInfo = () => {
        userInfo.value = {}
    }

    return { token, userInfo, setToken, removeToken, setUserInfo, removeUserInfo }
})
