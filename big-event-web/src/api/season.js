import request from '@/utils/request'

// 1. 赛季列表（分页 + name 模糊搜索）params: { pageNum, pageSize, name }
export const seasonListService = (params) => {
    return request.get('/season', { params })
}

// 2. 新增赛季
export const seasonAddService = (data) => {
    return request.post('/season', data)
}

// 3. 修改赛季（id 在请求体中）
export const seasonUpdateService = (data) => {
    return request.put('/season', data)
}

// 4. 删除赛季
export const seasonDeleteService = (id) => {
    return request.delete(`/season/${id}`)
}
