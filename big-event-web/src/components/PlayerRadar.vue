<script setup>
import { ref, onMounted, watch, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { playerGameStatListService } from '@/api/stat'

const props = defineProps({
  playerId: { type: Number, required: true },
  seasonId: { type: Number, required: true }
})

const chartRef = ref(null)
let chartInstance = null

const computeRadar = async () => {
  const res = await playerGameStatListService({
    pageNum: 1,
    pageSize: 100,
    playerId: props.playerId
  })
  const list = res.data?.items || []
  if (list.length === 0) return null

  let totals = { pts: 0, reb: 0, ast: 0, stl: 0, blk: 0, fgm: 0, fga: 0, ftm: 0, fta: 0, min: 0 }
  list.forEach(item => {
    totals.pts += item.points || 0
    totals.reb += item.rebounds || 0
    totals.ast += item.assists || 0
    totals.stl += item.steals || 0
    totals.blk += item.blocks || 0
    totals.fgm += item.fgm || 0
    totals.fga += item.fga || 0
    totals.ftm += item.ftm || 0
    totals.fta += item.fta || 0
    totals.min += item.minutes || 0
  })

  const games = list.length
  const avgPts = totals.pts / games
  const avgReb = totals.reb / games
  const avgAst = totals.ast / games
  const avgStl = totals.stl / games
  const avgBlk = totals.blk / games
  const fgPct = totals.fga > 0 ? (totals.fgm / totals.fga) * 100 : 0
  const ftPct = totals.fta > 0 ? (totals.ftm / totals.fta) * 100 : 0

  // 把各项指标归一化到 0-100 用于雷达展示（非真实数值比例，仅为了可视化）
  return [
    { name: '得分', max: 30, value: Math.min(avgPts, 30) },
    { name: '篮板', max: 10, value: Math.min(avgReb, 10) },
    { name: '助攻', max: 10, value: Math.min(avgAst, 10) },
    { name: '抢断', max: 3, value: Math.min(avgStl, 3) },
    { name: '盖帽', max: 3, value: Math.min(avgBlk, 3) },
    { name: '投篮%', max: 100, value: fgPct },
    { name: '罚球%', max: 100, value: ftPct }
  ]
}

const renderChart = async () => {
  if (!chartInstance) return

  let radarData = null
  try {
    radarData = await computeRadar()
  } catch (e) {
    // 不捕获的话 Promise 会静默 reject，图表保持空白且没有任何提示，
    // 表现就是"页面不显示数据"。热图组件（ShotChart.vue）已有同样的保护。
    console.error('加载球员雷达图失败', e)
    chartInstance.setOption({ title: { text: '加载失败，请重试', left: 'center' } }, true)
    return
  }

  if (!radarData) {
    chartInstance.setOption({ title: { text: '暂无数据', left: 'center' } }, true)
    return
  }

  const option = {
    title: { text: '球员能力雷达图', left: 'center' },
    tooltip: { trigger: 'item' },
    radar: {
      indicator: radarData.map(i => ({ name: i.name, max: i.max })),
      radius: '65%'
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: radarData.map(i => i.value),
            name: '综合能力',
            areaStyle: { opacity: 0.3 }
          }
        ]
      }
    ]
  }

  chartInstance.setOption(option, true)
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
