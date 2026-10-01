import request from '@/utils/request'

// 1. 比赛列表（分页 + seasonId/teamId(主或客)/status 筛选）params: { pageNum, pageSize, seasonId, teamId, status }
export const gameListService = (params) => {
    return request.get('/game', { params })
}

// 2. 新增比赛
export const gameAddService = (data) => {
    return request.post('/game', data)
}

// 3. 修改比赛（id 在请求体中）
export const gameUpdateService = (data) => {
    return request.put('/game', data)
}

// 4. 删除比赛
export const gameDeleteService = (id) => {
    return request.delete(`/game/${id}`)
}
