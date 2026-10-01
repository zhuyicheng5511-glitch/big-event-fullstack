import request from '@/utils/request'

// 1. 球员列表（分页 + name 模糊搜索 + teamId 精确筛选）params: { pageNum, pageSize, name, teamId }
export const playerListService = (params) => {
    return request.get('/player', { params })
}

// 2. 新增球员
export const playerAddService = (data) => {
    return request.post('/player', data)
}

// 3. 修改球员（id 在请求体中）
export const playerUpdateService = (data) => {
    return request.put('/player', data)
}

// 4. 删除球员
export const playerDeleteService = (id) => {
    return request.delete(`/player/${id}`)
}
