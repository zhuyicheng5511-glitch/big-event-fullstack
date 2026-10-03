import request from '@/utils/request'

// 得分排行榜 params: { seasonId, teamId, position, limit }
export const rankPointsService = (params) => {
    return request.get('/rank/points', { params })
}
