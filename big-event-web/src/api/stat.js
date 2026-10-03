import request from '@/utils/request'

// 球员单场数据列表（用于雷达图/趋势图聚合）
// params: { pageNum, pageSize, gameId, playerId, teamId }
export const playerGameStatListService = (params) => {
    return request.get('/player-game-stat', { params })
}
