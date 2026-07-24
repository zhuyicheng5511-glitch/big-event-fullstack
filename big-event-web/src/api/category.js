import request from '@/utils/request'

// 1. 获取文章分类列表
export const categoryListService = () => {
    return request.get('/category')
}

// 2. 新增文章分类
export const categoryAddService = (categoryData) => {
    return request.post('/category', categoryData)
}

// 3. 修改文章分类
export const categoryUpdateService = (categoryData) => {
    return request.put('/category', categoryData)
}

// 4. 删除文章分类
export const categoryDeleteService = (id) => {
    return request.delete(`/category?id=${id}`)
}