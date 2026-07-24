import request from '@/utils/request'

// 1. 获取文章列表 (分页 + 条件筛选)
export const articleListService = (params) => {
    return request.get('/article', { params: params })
}

// 2. 发布/保存文章
export const articleAddService = (articleData) => {
    return request.post('/article', articleData)
}

// 3. 修改文章
export const articleUpdateService = (articleData) => {
    return request.put('/article', articleData)
}

// 4. 删除文章
export const articleDeleteService = (id) => {
    return request.delete(`/article?id=${id}`)
}