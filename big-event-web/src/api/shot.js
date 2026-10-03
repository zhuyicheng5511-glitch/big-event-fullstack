import request from '@/utils/request'

// 投篮热图聚合 params: { playerId, seasonId, zone }
export const shotChartService = (params) => {
    return request.get('/shot/chart', { params })
}
