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
// 注意：后端接口是 @DeleteMapping("/{id}")，路径参数形式，必须写成 /article/3
//       不能写成 /article?id=3（那会匹配不到任何处理方法，报「服务器繁忙」）
export const articleDeleteService = (id) => {
    return request.delete(`/article/${id}`)
}