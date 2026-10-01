import request from '@/utils/request'

// 1. 球队列表（分页 + keyword 模糊搜索球队名/城市）params: { pageNum, pageSize, keyword }
export const teamListService = (params) => {
    return request.get('/team', { params })
}

// 2. 新增球队
export const teamAddService = (data) => {
    return request.post('/team', data)
}

// 3. 修改球队（id 在请求体中）
export const teamUpdateService = (data) => {
    return request.put('/team', data)
}

// 4. 删除球队
export const teamDeleteService = (id) => {
    return request.delete(`/team/${id}`)
}
