<script setup>
import { ref, onMounted, watch, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { playerStatSummaryService } from '@/api/stat'

const props = defineProps({
  playerId: { type: Number, required: true },
  seasonId: { type: Number, required: true }
})

const chartRef = ref(null)
let chartInstance = null

// 请求序号：快速切换球员/赛季时，只采纳最后一次请求的结果，避免先发后到的响应把图覆盖成旧数据
let requestSeq = 0

const num = (v) => (v == null ? 0 : Number(v))

/**
 * 取球员的汇总数据。
 *
 * 这里必须用后端的聚合接口，而不是拉明细列表在前端自己平均：
 * 明细接口单页最多 100 条，而一名球员一个赛季可能有几百场比赛，
 * 只拿第一页算出来的场均数据是错的（767 场时保罗·乔治场均得分会从 14.30 偏成 13.11）。
 * 同时 seasonId 也要真正传给后端，否则换了赛季数据不变。
 */
const computeRadar = async () => {
  const params = { playerId: props.playerId }
  if (props.seasonId != null) {
    params.seasonId = props.seasonId
  }
  const res = await playerStatSummaryService(params)
  const s = res.data

  // 该球员在该赛季没有比赛数据，后端返回 code 0 + data null
  if (!s || !s.games) return null

  // 各项指标量纲差异很大（得分 0-30、命中率 0-100），
  // 所以每个维度给各自的 max，让雷达图铺满而不是被命中率压扁
  return {
    playerName: s.playerName || '球员',
    games: s.games,
    indicators: [
      { name: '得分', max: 30, value: Math.min(num(s.avgPoints), 30) },
      { name: '篮板', max: 10, value: Math.min(num(s.avgRebounds), 10) },
      { name: '助攻', max: 10, value: Math.min(num(s.avgAssists), 10) },
      { name: '抢断', max: 3, value: Math.min(num(s.avgSteals), 3) },
      { name: '盖帽', max: 3, value: Math.min(num(s.avgBlocks), 3) },
      { name: '投篮%', max: 100, value: num(s.fgPct) },
      { name: '罚球%', max: 100, value: num(s.ftPct) }
    ]
  }
}

const renderChart = async () => {
  if (!chartInstance) return

  const seq = ++requestSeq
  let radarData = null
  try {
    radarData = await computeRadar()
  } catch (e) {
    // 不捕获的话 Promise 会静默 reject，图表保持空白且没有任何提示，
    // 表现就是"页面不显示数据"。热图组件（ShotChart.vue）已有同样的保护。
    if (seq !== requestSeq) return
    console.error('加载球员雷达图失败', e)
    chartInstance.setOption({ title: { text: '加载失败，请重试', left: 'center' } }, { notMerge: true })
    return
  }
  if (seq !== requestSeq) return

  if (!radarData) {
    chartInstance.setOption({ title: { text: '暂无数据', left: 'center' } }, { notMerge: true })
    return
  }

  const option = {
    title: {
      text: '球员能力雷达图',
      // 标注实际场次，用来验证场均数据是基于全部比赛而不是只取了第一页
      subtext: `${radarData.playerName} · 共 ${radarData.games} 场`,
      left: 'center'
    },
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        if (!Array.isArray(params.value)) return params.name
        const lines = radarData.indicators.map((it, i) => `${it.name}：${params.value[i]}`)
        return `${radarData.playerName}<br/>${lines.join('<br/>')}`
      }
    },
    radar: {
      indicator: radarData.indicators.map(i => ({ name: i.name, max: i.max })),
      radius: '65%'
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: radarData.indicators.map(i => i.value),
            name: '综合能力',
            areaStyle: { opacity: 0.3 }
          }
        ]
      }
    ]
  }

  chartInstance.setOption(option, { notMerge: true })
}

onMounted(() => {
  if (chartRef.value) {
    chartInstance = echarts.init(chartRef.value)
    renderChart()
    window.addEventListener('resize', chartInstance.resize)
  }
})

onUnmounted(() => {
  if (chartInstance) {
    window.removeEventListener('resize', chartInstance.resize)
    chartInstance.dispose()
    chartInstance = null
  }
})

watch(() => [props.playerId, props.seasonId], () => {
  renderChart()
}, { deep: true })
</script>

<template>
  <div ref="chartRef" style="width: 100%; height: 400px;"></div>
</template>
