<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { Edit, Delete, Plus } from '@element-plus/icons-vue'
import {
  teamListService,
  teamAddService,
  teamUpdateService,
  teamDeleteService
} from '@/api/team'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()

const pageNum = ref(1)
const total = ref(0)
const pageSize = ref(5)
const keyword = ref('')
const teams = ref([])
const visibleDrawer = ref(false)
const drawerTitle = ref('')
const formRef = ref(null)

const teamModel = ref({
  name: '',
  nameEn: '',
  city: '',
  conference: '',
  division: '',
  logoUrl: ''
})

const rules = {
  name: [
    { required: true, message: '请输入球队名', trigger: 'blur' }
  ]
}

const getTeams = async () => {
  const params = {
    pageNum: pageNum.value,
    pageSize: pageSize.value,
    keyword: keyword.value ? keyword.value : null
  }
  const res = await teamListService(params)
  total.value = res.data.total
  teams.value = res.data.items
}

onMounted(() => {
  getTeams()
})

const handleCurrentChange = (val) => {
  pageNum.value = val
  getTeams()
}

const handleSizeChange = (val) => {
  pageSize.value = val
  getTeams()
}

const resetSearch = () => {
  keyword.value = ''
  pageNum.value = 1
  getTeams()
}

const showAddDrawer = () => {
  visibleDrawer.value = true
  drawerTitle.value = '添加球队'
  teamModel.value = {
    name: '',
    nameEn: '',
    city: '',
    conference: '',
    division: '',
    logoUrl: ''
  }
  nextTick(() => formRef.value?.clearValidate())
}

const showEditDrawer = (row) => {
  visibleDrawer.value = true
  drawerTitle.value = '修改球队'
  teamModel.value = { ...row }
  nextTick(() => formRef.value?.clearValidate())
}

const handleConfirm = async () => {
  await formRef.value.validate()
  if (teamModel.value.id) {
    await teamUpdateService(teamModel.value)
    ElMessage.success('修改球队成功！')
  } else {
    await teamAddService(teamModel.value)
    ElMessage.success('添加球队成功！')
  }
  visibleDrawer.value = false
  getTeams()
}

const deleteTeam = (row) => {
  ElMessageBox.confirm(`您确定要删除球队 [${row.name}] 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await teamDeleteService(row.id)
    ElMessage.success('删除成功')
    getTeams()
  })
}

const handleLogoSuccess = (response) => {
  teamModel.value.logoUrl = response.data
  ElMessage.success('队标上传成功！')
}
</script>

<template>
  <el-card class="page-container">
    <template #header>
      <div class="header">
        <span>球队管理</span>
        <el-button type="primary" @click="showAddDrawer">添加球队</el-button>
      </div>
    </template>

    <el-form inline class="search-form">
      <el-form-item label="关键字：">
        <el-input
          v-model="keyword"
          clearable
          placeholder="球队名 / 城市"
          style="width: 200px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="getTeams">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="teams" style="width: 100%">
      <el-table-column type="index" width="80" />
      <el-table-column label="队标" width="80">
        <template #default="{ row }">
          <img
            v-if="row.logoUrl"
            :src="row.logoUrl"
            class="team-logo"
            alt="队标"
          />
          <div v-else class="team-logo-placeholder">无</div>
        </template>
      </el-table-column>
      <el-table-column label="球队名" prop="name" />
      <el-table-column label="英文名" prop="nameEn" />
      <el-table-column label="城市" prop="city" />
      <el-table-column label="联盟" prop="conference">
        <template #default="{ row }">
          <el-tag v-if="row.conference === '东部'" type="primary">东部</el-tag>
          <el-tag v-else-if="row.conference === '西部'" type="success">西部</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="分区" prop="division" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button :icon="Edit" circle type="primary" plain @click="showEditDrawer(row)" />
          <el-button :icon="Delete" circle type="danger" plain @click="deleteTeam(row)" />
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
      <el-form ref="formRef" :model="teamModel" :rules="rules" label-width="100px">
        <el-form-item label="球队名" prop="name">
          <el-input v-model="teamModel.name" placeholder="请输入球队名" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="英文名">
          <el-input v-model="teamModel.nameEn" placeholder="请输入英文名" maxlength="80" show-word-limit />
        </el-form-item>
        <el-form-item label="城市">
          <el-input v-model="teamModel.city" placeholder="请输入城市" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="联盟">
          <el-select v-model="teamModel.conference" clearable placeholder="请选择联盟" style="width: 100%">
            <el-option label="东部" value="东部" />
            <el-option label="西部" value="西部" />
          </el-select>
        </el-form-item>
        <el-form-item label="分区">
          <el-input v-model="teamModel.division" placeholder="如 太平洋区" maxlength="20" show-word-limit />
        </el-form-item>
        <el-form-item label="队标">
          <el-upload
            class="avatar-uploader"
            action="/api/upload"
            :show-file-list="false"
            :headers="{ Authorization: userStore.token }"
            :on-success="handleLogoSuccess"
          >
            <img v-if="teamModel.logoUrl" :src="teamModel.logoUrl" class="avatar" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
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

.team-logo {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.team-logo-placeholder {
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
