<script setup>
import { ref, computed, nextTick, onMounted } from 'vue'
import { Edit, Delete } from '@element-plus/icons-vue'
import {
  gameListService,
  gameAddService,
  gameUpdateService,
  gameDeleteService
} from '@/api/game'
import { seasonListService } from '@/api/season'
import { teamListService } from '@/api/team'
import { ElMessage, ElMessageBox } from 'element-plus'

const pageNum = ref(1)
const total = ref(0)
const pageSize = ref(5)
const seasonId = ref(null)
const teamId = ref(null)
const status = ref('')
const games = ref([])
const seasons = ref([])
const teams = ref([])

const seasonMap = computed(() => {
  return Object.fromEntries(seasons.value.map(s => [s.id, s]))
})
const teamMap = computed(() => {
  return Object.fromEntries(teams.value.map(t => [t.id, t]))
})

const visibleDrawer = ref(false)
const drawerTitle = ref('')
const formRef = ref(null)

const gameModel = ref({
  seasonId: null,
  homeTeamId: null,
  awayTeamId: null,
  gameDate: null,
  homeScore: null,
  awayScore: null,
  status: '未开始',
  venue: ''
})

const validateAwayTeam = (rule, value, callback) => {
  if (value && value === gameModel.value.homeTeamId) {
    callback(new Error('客队不能与主队相同'))
  } else {
    callback()
  }
}

const rules = {
  seasonId: [{ required: true, message: '请选择赛季', trigger: 'change' }],
  homeTeamId: [{ required: true, message: '请选择主队', trigger: 'change' }],
  awayTeamId: [
    { required: true, message: '请选择客队', trigger: 'change' },
    { validator: validateAwayTeam, trigger: 'change' }
  ],
  gameDate: [{ required: true, message: '请选择比赛时间', trigger: 'change' }],
  status: [{ required: true, message: '请选择比赛状态', trigger: 'change' }]
}

const getSeasons = async () => {
  const res = await seasonListService({ pageNum: 1, pageSize: 100 })
  seasons.value = res.data.items
}

const getTeams = async () => {
  const res = await teamListService({ pageNum: 1, pageSize: 100 })
  teams.value = res.data.items
}

const getGames = async () => {
  const params = {
    pageNum: pageNum.value,
    pageSize: pageSize.value,
    seasonId: seasonId.value ? seasonId.value : null,
    teamId: teamId.value ? teamId.value : null,
    status: status.value ? status.value : null
  }
  const res = await gameListService(params)
  total.value = res.data.total
  games.value = res.data.items
}

onMounted(() => {
  getSeasons()
  getTeams()
  getGames()
})

const handleCurrentChange = (val) => {
  pageNum.value = val
  getGames()
}

const handleSizeChange = (val) => {
  pageSize.value = val
  getGames()
}

const resetSearch = () => {
  seasonId.value = null
  teamId.value = null
  status.value = ''
  pageNum.value = 1
  getGames()
}

const showAddDrawer = () => {
  visibleDrawer.value = true
  drawerTitle.value = '添加比赛'
  gameModel.value = {
    seasonId: null,
    homeTeamId: null,
    awayTeamId: null,
    gameDate: null,
    homeScore: null,
    awayScore: null,
    status: '未开始',
    venue: ''
  }
  nextTick(() => formRef.value?.clearValidate())
}

const showEditDrawer = (row) => {
  visibleDrawer.value = true
  drawerTitle.value = '修改比赛'
  gameModel.value = { ...row }
  nextTick(() => formRef.value?.clearValidate())
}

const handleConfirm = async () => {
  await formRef.value.validate()
  if (gameModel.value.id) {
    await gameUpdateService(gameModel.value)
    ElMessage.success('修改比赛成功！')
  } else {
    await gameAddService(gameModel.value)
    ElMessage.success('添加比赛成功！')
  }
  visibleDrawer.value = false
  getGames()
}

const deleteGame = (row) => {
  ElMessageBox.confirm(`您确定要删除比赛 [${seasonMap.value[row.seasonId]?.name ?? ''} ${teamMap.value[row.homeTeamId]?.name ?? ''} vs ${teamMap.value[row.awayTeamId]?.name ?? ''}] 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await gameDeleteService(row.id)
    ElMessage.success('删除成功')
    getGames()
  })
}
</script>

<template>
  <el-card class="page-container">
    <template #header>
      <div class="header">
        <span>比赛管理</span>
        <el-button type="primary" @click="showAddDrawer">添加比赛</el-button>
      </div>
    </template>

    <el-form inline class="search-form">
      <el-form-item label="赛季：">
        <el-select v-model="seasonId" clearable placeholder="请选择赛季" style="width: 200px">
          <el-option v-for="s in seasons" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="球队：">
        <el-select v-model="teamId" clearable filterable placeholder="主/客队" style="width: 200px">
          <el-option v-for="t in teams" :key="t.id" :label="t.name" :value="t.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态：">
        <el-select v-model="status" clearable placeholder="请选择状态" style="width: 200px">
          <el-option label="未开始" value="未开始" />
          <el-option label="进行中" value="进行中" />
          <el-option label="已结束" value="已结束" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="getGames">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="games" style="width: 100%">
      <el-table-column type="index" width="80" />
      <el-table-column label="赛季" prop="seasonId">
        <template #default="{ row }">
          {{ seasonMap[row.seasonId]?.name ?? '—' }}
        </template>
      </el-table-column>
      <el-table-column label="主队" prop="homeTeamId">
        <template #default="{ row }">
          {{ teamMap[row.homeTeamId]?.name ?? '—' }}
        </template>
      </el-table-column>
      <el-table-column label="客队" prop="awayTeamId">
        <template #default="{ row }">
          {{ teamMap[row.awayTeamId]?.name ?? '—' }}
        </template>
      </el-table-column>
      <el-table-column label="比分" width="120" align="center">
        <template #default="{ row }">
          {{ row.homeScore ?? '-' }} : {{ row.awayScore ?? '-' }}
        </template>
      </el-table-column>
      <el-table-column label="比赛时间" prop="gameDate" width="160" />
      <el-table-column label="状态" prop="status" width="100">
        <template #default="{ row }">
          <el-tag
            :type="row.status === '已结束' ? 'success' : row.status === '进行中' ? 'warning' : 'info'"
          >
            {{ row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="场馆" prop="venue" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button :icon="Edit" circle type="primary" plain @click="showEditDrawer(row)" />
          <el-button :icon="Delete" circle type="danger" plain @click="deleteGame(row)" />
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="没有数据" />
      </template>
    </el-table>

    <el-pagination
      class="pagination"
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :page-sizes="[5, 10, 15]"
      layout="jumper, total, sizes, prev, pager, next"
      :total="total"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />

    <el-drawer v-model="visibleDrawer" :title="drawerTitle" size="50%">
      <el-form ref="formRef" :model="gameModel" :rules="rules" label-width="100px">
        <el-form-item label="赛季" prop="seasonId">
          <el-select v-model="gameModel.seasonId" clearable placeholder="请选择赛季" style="width: 100%">
            <el-option v-for="s in seasons" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="主队" prop="homeTeamId">
          <el-select v-model="gameModel.homeTeamId" clearable filterable placeholder="请选择主队" style="width: 100%">
            <el-option v-for="t in teams" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="客队" prop="awayTeamId">
          <el-select v-model="gameModel.awayTeamId" clearable filterable placeholder="请选择客队" style="width: 100%">
            <el-option v-for="t in teams" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="比赛时间" prop="gameDate">
          <el-date-picker
            v-model="gameModel.gameDate"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择比赛时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="主队得分">
          <el-input-number v-model="gameModel.homeScore" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="客队得分">
          <el-input-number v-model="gameModel.awayScore" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="gameModel.status" clearable placeholder="请选择状态" style="width: 100%">
            <el-option label="未开始" value="未开始" />
            <el-option label="进行中" value="进行中" />
            <el-option label="已结束" value="已结束" />
          </el-select>
        </el-form-item>
        <el-form-item label="场馆">
          <el-input v-model="gameModel.venue" placeholder="请输入场馆" maxlength="80" show-word-limit />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleConfirm">保存</el-button>
          <el-button @click="visibleDrawer = false">取消</el-button>
        </el-form-item>
      </el-form>
    </el-drawer>
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
  .pagination {
    margin-top: 20px;
    justify-content: flex-end;
  }
}
</style>
