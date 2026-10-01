<script setup>
import { ref, computed, nextTick, onMounted } from 'vue'
import { Edit, Delete, Plus } from '@element-plus/icons-vue'
import {
  playerListService,
  playerAddService,
  playerUpdateService,
  playerDeleteService
} from '@/api/player'
import { teamListService } from '@/api/team'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()

const pageNum = ref(1)
const total = ref(0)
const pageSize = ref(5)
const name = ref('')
const teamId = ref(null)
const players = ref([])
const teams = ref([])

const teamMap = computed(() => {
  return Object.fromEntries(teams.value.map(t => [t.id, t]))
})

const visibleDrawer = ref(false)
const drawerTitle = ref('')
const formRef = ref(null)

const playerModel = ref({
  name: '',
  nameEn: '',
  teamId: null,
  jerseyNumber: '',
  position: '',
  heightCm: null,
  weightKg: null,
  birthDate: null,
  country: '',
  avatarUrl: '',
  isActive: 1
})

const rules = {
  name: [
    { required: true, message: '请输入球员中文名', trigger: 'blur' }
  ],
  isActive: [
    { required: true, message: '请选择是否现役', trigger: 'change' }
  ]
}

const getTeams = async () => {
  const res = await teamListService({ pageNum: 1, pageSize: 100 })
  teams.value = res.data.items
}

const getPlayers = async () => {
  const params = {
    pageNum: pageNum.value,
    pageSize: pageSize.value,
    name: name.value ? name.value : null,
    teamId: teamId.value ? teamId.value : null
  }
  const res = await playerListService(params)
  total.value = res.data.total
  players.value = res.data.items
}

onMounted(() => {
  getTeams()
  getPlayers()
})

const handleCurrentChange = (val) => {
  pageNum.value = val
  getPlayers()
}

const handleSizeChange = (val) => {
  pageSize.value = val
  getPlayers()
}

const resetSearch = () => {
  name.value = ''
  teamId.value = null
  pageNum.value = 1
  getPlayers()
}

const showAddDrawer = () => {
  visibleDrawer.value = true
  drawerTitle.value = '添加球员'
  playerModel.value = {
    name: '',
    nameEn: '',
    teamId: null,
    jerseyNumber: '',
    position: '',
    heightCm: null,
    weightKg: null,
    birthDate: null,
    country: '',
    avatarUrl: '',
    isActive: 1
  }
  nextTick(() => formRef.value?.clearValidate())
}

const showEditDrawer = (row) => {
  visibleDrawer.value = true
  drawerTitle.value = '修改球员'
  playerModel.value = { ...row }
  nextTick(() => formRef.value?.clearValidate())
}

const handleConfirm = async () => {
  await formRef.value.validate()
  if (playerModel.value.id) {
    await playerUpdateService(playerModel.value)
    ElMessage.success('修改球员成功！')
  } else {
    await playerAddService(playerModel.value)
    ElMessage.success('添加球员成功！')
  }
  visibleDrawer.value = false
  getPlayers()
}

const deletePlayer = (row) => {
  ElMessageBox.confirm(`您确定要删除球员 [${row.name}] 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await playerDeleteService(row.id)
    ElMessage.success('删除成功')
    getPlayers()
  })
}

const handleAvatarSuccess = (response) => {
  playerModel.value.avatarUrl = response.data
  ElMessage.success('头像上传成功！')
}
</script>

<template>
  <el-card class="page-container">
    <template #header>
      <div class="header">
        <span>球员管理</span>
        <el-button type="primary" @click="showAddDrawer">添加球员</el-button>
      </div>
    </template>

    <el-form inline class="search-form">
      <el-form-item label="姓名：">
        <el-input v-model="name" clearable placeholder="请输入姓名" style="width: 200px" />
      </el-form-item>
      <el-form-item label="球队：">
        <el-select v-model="teamId" clearable filterable placeholder="请选择球队" style="width: 200px">
          <el-option v-for="t in teams" :key="t.id" :label="t.name" :value="t.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="getPlayers">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="players" style="width: 100%">
      <el-table-column type="index" width="80" />
      <el-table-column label="头像" width="80">
        <template #default="{ row }">
          <img
            v-if="row.avatarUrl"
            :src="row.avatarUrl"
            class="player-avatar"
            alt="头像"
          />
          <div v-else class="player-avatar-placeholder">无</div>
        </template>
      </el-table-column>
      <el-table-column label="姓名" prop="name" />
      <el-table-column label="球队" prop="teamId">
        <template #default="{ row }">
          {{ teamMap[row.teamId]?.name ?? '—' }}
        </template>
      </el-table-column>
      <el-table-column label="球衣号" prop="jerseyNumber" width="80" />
      <el-table-column label="位置" prop="position" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.position" size="small">{{ row.position }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="身高(cm)" prop="heightCm" width="100" />
      <el-table-column label="体重(kg)" prop="weightKg" width="100" />
      <el-table-column label="出生日期" prop="birthDate" width="120" />
      <el-table-column label="现役" prop="isActive" width="80">
        <template #default="{ row }">
          <el-tag :type="row.isActive === 1 ? 'success' : 'info'">
            {{ row.isActive === 1 ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button :icon="Edit" circle type="primary" plain @click="showEditDrawer(row)" />
          <el-button :icon="Delete" circle type="danger" plain @click="deletePlayer(row)" />
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
      <el-form ref="formRef" :model="playerModel" :rules="rules" label-width="100px">
        <el-form-item label="中文名" prop="name">
          <el-input v-model="playerModel.name" placeholder="请输入球员中文名" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="英文名">
          <el-input v-model="playerModel.nameEn" placeholder="请输入英文名" maxlength="80" show-word-limit />
        </el-form-item>
        <el-form-item label="所属球队">
          <el-select v-model="playerModel.teamId" clearable filterable placeholder="请选择球队" style="width: 100%">
            <el-option v-for="t in teams" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="球衣号">
          <el-input v-model="playerModel.jerseyNumber" placeholder="请输入球衣号" maxlength="5" show-word-limit />
        </el-form-item>
        <el-form-item label="位置">
          <el-select v-model="playerModel.position" clearable placeholder="请选择位置" style="width: 100%">
            <el-option label="PG" value="PG" />
            <el-option label="SG" value="SG" />
            <el-option label="SF" value="SF" />
            <el-option label="PF" value="PF" />
            <el-option label="C" value="C" />
          </el-select>
        </el-form-item>
        <el-form-item label="身高(cm)">
          <el-input-number v-model="playerModel.heightCm" :min="0" :max="300" placeholder="cm" style="width: 100%" />
        </el-form-item>
        <el-form-item label="体重(kg)">
          <el-input-number v-model="playerModel.weightKg" :min="0" :max="200" placeholder="kg" style="width: 100%" />
        </el-form-item>
        <el-form-item label="出生日期">
          <el-date-picker
            v-model="playerModel.birthDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择出生日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="国籍">
          <el-input v-model="playerModel.country" placeholder="请输入国籍" maxlength="30" show-word-limit />
        </el-form-item>
        <el-form-item label="头像">
          <el-upload
            class="avatar-uploader"
            action="/api/upload"
            :show-file-list="false"
            :headers="{ Authorization: userStore.token }"
            :on-success="handleAvatarSuccess"
          >
            <img v-if="playerModel.avatarUrl" :src="playerModel.avatarUrl" class="avatar" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="现役" prop="isActive">
          <el-radio-group v-model="playerModel.isActive">
            <el-radio :value="1">是</el-radio>
            <el-radio :value="0">否</el-radio>
          </el-radio-group>
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

.player-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.player-avatar-placeholder {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #eee;
  color: #999;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.avatar-uploader {
  :deep() {
    .el-upload {
      border: 1px dashed var(--el-border-color);
      border-radius: 6px;
      cursor: pointer;
      position: relative;
      overflow: hidden;
      transition: var(--el-transition-duration-fast);
      &:hover {
        border-color: var(--el-color-primary);
      }
    }
  }
  .avatar-uploader-icon {
    font-size: 28px;
    color: #8c939d;
    width: 178px;
    height: 178px;
    text-align: center;
    line-height: 178px;
  }
  .avatar {
    width: 178px;
    height: 178px;
    display: block;
    object-fit: cover;
  }
}
</style>
