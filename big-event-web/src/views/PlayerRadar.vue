<script setup>
import { ref, onMounted } from 'vue'
import { playerListService } from '@/api/player'
import { seasonListService } from '@/api/season'
import PlayerRadar from '@/components/PlayerRadar.vue'
import { ElMessage } from 'element-plus'

// 表单状态
const playerId = ref('')
const seasonId = ref('')

// 查询条件：只有点「查询」才会写入（与其他页面保持一致的按钮驱动模式）
const query = ref(null)

const players = ref([])
const seasons = ref([])

const loadOptions = async () => {
  try {
    // ⚠️ pageSize 不能超过后端上限 100（PlayerController 上有 @Max(100)）。
    // 传 500 会让接口直接校验失败返回 code=1，Promise.all 随即 reject，
    // 两个下拉框都会是空的 —— 用户选不到球员，下方雷达图就永远渲染不出来。
    const [playerRes, seasonRes] = await Promise.all([
      playerListService({ pageNum: 1, pageSize: 100 }),
      seasonListService({ pageNum: 1, pageSize: 100 })
    ])
    players.value = playerRes.data.items || []
    seasons.value = seasonRes.data.items || []
  } catch (e) {
    // 具体错误提示已由 request 拦截器弹出，这里只防止未捕获的 Promise rejection
    console.error('加载球员/赛季选项失败', e)
  }
}

const handleQuery = () => {
  if (!playerId.value || !seasonId.value) {
    ElMessage.warning('请选择球员和赛季')
    return
  }
  query.value = {
    playerId: Number(playerId.value),
    seasonId: Number(seasonId.value)
  }
}

const resetSearch = () => {
  playerId.value = ''
  seasonId.value = ''
  query.value = null
}

onMounted(loadOptions)
</script>

<template>
  <el-card class="page-container">
    <template #header>
      <div class="header">
        <span>球员雷达图</span>
      </div>
    </template>

    <el-form inline class="search-form">
      <el-form-item label="球员：">
        <el-select v-model="playerId" placeholder="选择球员" filterable style="width: 200px">
          <el-option v-for="p in players" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="赛季：">
        <el-select v-model="seasonId" placeholder="选择赛季" style="width: 150px">
          <el-option v-for="s in seasons" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </el-form-item>
    </el-form>

    <PlayerRadar
      v-if="query"
      :player-id="query.playerId"
      :season-id="query.seasonId"
    />
    <el-empty v-else description="请选择球员和赛季后点击查询" />
  </el-card>
</template>

<style lang="scss" scoped>
.page-container {
  min-height: 100%;
  box-sizing: border-box;
  .header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  .search-form {
    margin-bottom: 20px;
  }
}
</style>
