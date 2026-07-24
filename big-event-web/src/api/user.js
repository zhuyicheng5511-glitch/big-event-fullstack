import request from '@/utils/request'

// 1. 注册接口 (平级独立)
export const userRegisterService = (registerData) => {
    const params = new URLSearchParams()
    for (let key in registerData) {
        params.append(key, registerData[key])
    }
    return request.post('/user/register', params)
}

// 2. 登录接口 (平级独立)
export const userLoginService = (loginData) => {
    const params = new URLSearchParams()
    for (let key in loginData) {
        params.append(key, loginData[key])
    }
    return request.post('/user/login', params)
}

// 3. 获取用户详细信息 (必须独立在最外层，千万不要塞进上面两个函数的花括号里)
export const userGetInfoService = () => {
    return request.get('/user/userInfo')
}

// 更新用户头像
export const userUpdateAvatarService = (avatarUrl) => {
    // 利用 URLSearchParams 传输 Query 参数
    const params = new URLSearchParams()
    params.append('avatarUrl', avatarUrl)
    return request.patch('/user/updateAvatar', params)
}

// 修改密码 (传入包含 old_pwd, new_pwd, re_pwd 的对象)
export const userUpdatePasswordService = (pwdData) => {
    return request.patch('/user/updatePwd', pwdData)
}

// 修改个人资料 (传入包含 id, nickname, email 的对象)
export const userUpdateInfoService = (infoData) => {
    return request.put('/user/update', infoData)
}