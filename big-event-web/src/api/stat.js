import request from '@/utils/request'

// 球员单场数据列表（明细分页，单页最多 100 条）
// params: { pageNum, pageSize, gameId, playerId, teamId }
export const playerGameStatListService = (params) => {
    return request.get('/player-game-stat', { params })
}

// 球员汇总统计（场均数据与命中率，后端在数据库里聚合，返回全部场次的结果）
// params: { playerId, seasonId }
// ⚠️ 需要「场均 / 汇总」类数据时一律用这个接口，不要用上面的明细接口自己平均：
//    明细单页最多 100 条，球员赛季场次超过 100 时算出来的场均数据是错的。
export const playerStatSummaryService = (params) => {
    return request.get('/player-game-stat/summary', { params })
}
