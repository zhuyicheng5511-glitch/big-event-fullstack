<script setup>
import { ref, onMounted } from 'vue'
import { rankPointsService } from '@/api/rank'
import { seasonListService } from '@/api/season'
import { teamListService } from '@/api/team'
import { ElMessage } from 'element-plus'

const seasonId = ref('')
const teamId = ref('')
const position = ref('')
const limit = ref(10)
const loading = ref(false)
const ranks = ref([])

const seasons = ref([])
const teams = ref([])
const positions = [
  { label: '全部', value: '' },
  { label: '控球后卫 PG', value: 'PG' },
  { label: '得分后卫 SG', value: 'SG' },
  { label: '小前锋 SF', value: 'SF' },
  { label: '大前锋 PF', value: 'PF' },
  { label: '中锋 C', value: 'C' }
]

const loadOptions = async () => {
  const [seasonRes, teamRes] = await Promise.all([
    seasonListService({ pageNum: 1, pageSize: 100 }),
    teamListService({ pageNum: 1, pageSize: 100 })
  ])
  seasons.value = seasonRes.data.items || []
  teams.value = teamRes.data.items || []
}

const getRanks = async () => {
  if (!seasonId.value) {
    ElMessage.warning('请选择赛季')
    return
  }
  loading.value = true
  try {
    const res = await rankPointsService({
      seasonId: seasonId.value,
      teamId: teamId.value || undefined,
      position: position.value || undefined,
      limit: limit.value
    })
    ranks.value = res.data || []
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  seasonId.value = ''
  teamId.value = ''
  position.value = ''
  limit.value = 10
  ranks.value = []
}

onMounted(() => {
  loadOptions()
})
</script>

<template>
  <el-card class="page-container">
    <template #header>
      <div class="header">
        <span>得分排行榜</span>
      </div>
    </template>

    <el-form inline class="search-form">
      <el-form-item label="赛季：">
        <el-select v-model="seasonId" placeholder="选择赛季" style="width: 150px">
          <el-option v-for="s in seasons" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="球队：">
        <el-select v-model="teamId" placeholder="全部" clearable style="width: 150px">
          <el-option v-for="t in teams" :key="t.id" :label="t.name" :value="t.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="位置：">
        <el-select v-model="position" placeholder="全部" clearable style="width: 150px">
          <el-option v-for="p in positions" :key="p.value" :label="p.label" :value="p.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="前 N 名：">
        <el-input-number v-model="limit" :min="1" :max="100" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="getRanks">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="ranks" v-loading="loading" style="width: 100%">
      <el-table-column label="排名" prop="rank" width="80" />
      <el-table-column label="球员" prop="playerName" />
      <el-table-column label="球队" prop="teamName" />
      <el-table-column label="总得分" prop="totalPoints" />
      <el-table-column label="场均得分" prop="avgPoints" />
      <el-table-column label="篮板" prop="rebounds" />
      <el-table-column label="助攻" prop="assists" />
      <template #empty>
        <el-empty description="没有数据" />
      </template>
    </el-table>
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
