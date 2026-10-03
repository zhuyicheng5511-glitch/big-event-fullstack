<script setup>
import { ref, onMounted, watch, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { shotChartService } from '@/api/shot'

const props = defineProps({
  playerId: { type: Number, required: true },
  seasonId: { type: Number, required: true },
  zone: { type: String, default: '' }
})

const chartRef = ref(null)
let chartInstance = null

// ---- 半场尺寸（单位：英尺）----
// x: 0~50 球场宽度；y: 0~47，0 = 底线，47 = 中线
const COURT_W = 50
const COURT_H = 47
const RIM = { x: 25, y: 5.25 }        // 篮筐中心：距底线 5.25 英尺
const THREE_R = 23.75                 // 三分线半径

// 兜底坐标：正常情况下用接口返回的 centerX / centerY
const ZONE_FALLBACK = {
  篮下: { x: 25, y: 2 },
  油漆区: { x: 25, y: 7 },
  中距离: { x: 25, y: 15 },
  左底角三分: { x: 2.5, y: 9 },
  右底角三分: { x: 47.5, y: 9 },
  左翼三分: { x: 7.5, y: 26.5 },
  右翼三分: { x: 42.5, y: 26.5 },
  弧顶三分: { x: 25, y: 30 }
}

/**
 * 画球场底图。
 *
 * 关键点：必须与散点共用【同一套坐标轴坐标系】——所有坐标都通过 api.coord()
 * 从英尺换算成像素。旧实现用的是 graphic + 固定像素坐标，而且除了矩形之外
 * 其余元素都没写 left/top，会被画到画布左上角；再加上像素坐标系与坐标轴
 * 坐标系方向、缩放都不同，底图和数据点完全对不上。
 */
const renderCourt = (params, api) => {
  const pt = (x, y) => api.coord([x, y])
  const ux = (n) => api.size([n, 0])[0]   // n 英尺对应多少横向像素
  const line = { fill: 'none', stroke: '#c8c9cc', lineWidth: 1.5 }
  const children = []

  // 1. 球场边界
  const tl = pt(0, COURT_H)
  const br = pt(COURT_W, 0)
  children.push({
    type: 'rect',
    shape: { x: tl[0], y: tl[1], width: br[0] - tl[0], height: br[1] - tl[1] },
    style: { ...line, lineWidth: 2 }
  })

  // 2. 油漆区：16 英尺宽，到罚球线 19 英尺
  const pr = pt(17, 19)
  const pb = pt(33, 0)
  children.push({
    type: 'rect',
    shape: { x: pr[0], y: pr[1], width: pb[0] - pr[0], height: pb[1] - pr[1] },
    style: { fill: 'rgba(64,158,255,0.06)', stroke: '#c8c9cc', lineWidth: 1.5 }
  })

  // 3. 罚球线圆：半径 6 英尺
  const ft = pt(25, 19)
  children.push({ type: 'circle', shape: { cx: ft[0], cy: ft[1], r: ux(6) }, style: line })

  // 4. 三分线：两条底角直线 + 圆弧。圆弧用折线近似，避免画布 y 轴向下的
  //    角度方向问题（用 atan2 在英尺坐标里推角度，再由 api.coord 换算）。
  const cornerY = Math.sqrt(THREE_R * THREE_R - Math.pow(25 - 3, 2)) + RIM.y
  const a0 = Math.atan2(cornerY - RIM.y, 3 - RIM.x)
  const a1 = Math.atan2(cornerY - RIM.y, 47 - RIM.x)
  const arc = []
  for (let i = 0; i <= 60; i++) {
    const t = a0 + (a1 - a0) * (i / 60)
    arc.push(pt(RIM.x + THREE_R * Math.cos(t), RIM.y + THREE_R * Math.sin(t)))
  }
  children.push({
    type: 'polyline',
    shape: { points: [pt(3, 0), ...arc, pt(47, 0)] },
    style: line
  })

  // 5. 篮板：宽 4 英尺，距底线 4 英尺
  const bb1 = pt(23, 4)
  const bb2 = pt(27, 4)
  children.push({
    type: 'line',
    shape: { x1: bb1[0], y1: bb1[1], x2: bb2[0], y2: bb2[1] },
    style: { stroke: '#909399', lineWidth: 2 }
  })

  // 6. 篮筐
  const rim = pt(RIM.x, RIM.y)
  children.push({
    type: 'circle',
    shape: { cx: rim[0], cy: rim[1], r: Math.max(3, ux(0.75)) },
    style: { fill: '#f56c6c', stroke: '#f56c6c' }
  })

  return { type: 'group', children }
}

const renderChart = async () => {
  if (!chartInstance) return

  const params = {
    playerId: props.playerId,
    seasonId: props.seasonId,
    zone: props.zone || undefined
  }

  try {
    const res = await shotChartService(params)
    const data = res.data || []

    // 直接使用后端返回的 centerX / centerY（权威坐标），不再前端硬编码 + 随机抖动。
    // 抖动会把点挪到假位置上，对数据可视化是误导。
    const scatterData = data.map(item => {
      const fb = ZONE_FALLBACK[item.zone] || { x: 25, y: 25 }
      const x = item.centerX != null ? Number(item.centerX) : fb.x
      const y = item.centerY != null ? Number(item.centerY) : fb.y
      return {
        name: item.zone,
        value: [x, y, item.fgPct],
        attempts: item.attempts,
        made: item.made
      }
    })

    const option = {
      title: { text: '投篮热图', left: 'center', top: 8 },
      tooltip: {
        trigger: 'item',
        formatter: (p) => {
          if (!p.data || !p.data.name) return ''
          return `${p.data.name}<br/>出手：${p.data.attempts}<br/>命中：${p.data.made}<br/>命中率：${p.data.value[2]}%`
        }
      },
      // 固定绘图区尺寸，保持 50:47 的球场宽深比，避免球场被拉伸变形
      grid: { left: 'center', top: 56, width: 450, height: 423 },
      xAxis: { type: 'value', show: false, min: 0, max: COURT_W },
      yAxis: { type: 'value', show: false, min: 0, max: COURT_H },
      series: [
        {
          // 球场底图。custom series 需要至少一条数据才会触发 renderItem，
          // 这里给一个占位项，renderItem 内部忽略它、只画整块球场。
          type: 'custom',
          z: 1,
          silent: true,
          coordinateSystem: 'cartesian2d',
          data: [0],
          renderItem: renderCourt
        },
        {
          type: 'scatter',
          z: 3,
          symbolSize: (val) => Math.max(12, val[2] / 2),
          data: scatterData,
          itemStyle: {
            color: (p) => {
              const pct = p.data.value[2]
              return pct > 50 ? '#f56c6c' : pct > 35 ? '#e6a23c' : '#409eff'
            },
            opacity: 0.9
          },
          label: {
            show: true,
            position: 'bottom',
            fontSize: 11,
            color: '#606266',
            formatter: (p) => p.data.name
          }
        }
      ]
    }

    chartInstance.setOption(option, true)
  } catch (e) {
    // 不捕获的话 Promise 会静默 reject，图表保持空白且没有任何提示
    console.error('加载投篮热图失败', e)
    chartInstance.setOption({ title: { text: '加载失败，请重试', left: 'center', top: 8 } }, true)
  }
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

watch(() => [props.playerId, props.seasonId, props.zone], () => {
  renderChart()
}, { deep: true })
</script>

<template>
  <div ref="chartRef" style="width: 100%; height: 520px;"></div>
</template>
