<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { Edit, Delete } from '@element-plus/icons-vue'
import {
  seasonListService,
  seasonAddService,
  seasonUpdateService,
  seasonDeleteService
} from '@/api/season'
import { ElMessage, ElMessageBox } from 'element-plus'

const pageNum = ref(1)
const total = ref(0)
const pageSize = ref(5)
const name = ref('')
const seasons = ref([])
const visibleDrawer = ref(false)
const drawerTitle = ref('')
const formRef = ref(null)

const seasonModel = ref({
  name: '',
  startDate: null,
  endDate: null,
  isCurrent: 0
})

const validateEndDate = (rule, value, callback) => {
  const start = seasonModel.value.startDate
  if (value && start && value < start) {
    callback(new Error('结束日期不能早于开始日期'))
  } else {
    callback()
  }
}

const rules = {
  name: [
    { required: true, message: '请输入赛季名', trigger: 'blur' },
    {
      pattern: /^\d{4}-\d{2}$/,
      message: '格式必须为 2025-26 这样的年份格式',
      trigger: 'blur'
    }
  ],
  isCurrent: [
    { required: true, message: '请选择是否当前赛季', trigger: 'change' }
  ],
  endDate: [
    { validator: validateEndDate, trigger: 'change' }
  ]
}

const getSeasons = async () => {
  const params = {
    pageNum: pageNum.value,
    pageSize: pageSize.value,
    name: name.value ? name.value : null
  }
  const res = await seasonListService(params)
  total.value = res.data.total
  seasons.value = res.data.items
}

onMounted(() => {
  getSeasons()
})

const handleCurrentChange = (val) => {
  pageNum.value = val
  getSeasons()
}

const handleSizeChange = (val) => {
  pageSize.value = val
  getSeasons()
}

const resetSearch = () => {
  name.value = ''
  pageNum.value = 1
  getSeasons()
}

const showAddDrawer = () => {
  visibleDrawer.value = true
  drawerTitle.value = '添加赛季'
  seasonModel.value = {
    name: '',
    startDate: null,
    endDate: null,
    isCurrent: 0
  }
  nextTick(() => formRef.value?.clearValidate())
}

const showEditDrawer = (row) => {
  visibleDrawer.value = true
  drawerTitle.value = '修改赛季'
  seasonModel.value = { ...row }
  nextTick(() => formRef.value?.clearValidate())
}

const handleConfirm = async () => {
  await formRef.value.validate()
  if (seasonModel.value.id) {
    await seasonUpdateService(seasonModel.value)
    ElMessage.success('修改赛季成功！')
  } else {
    await seasonAddService(seasonModel.value)
    ElMessage.success('添加赛季成功！')
  }
  visibleDrawer.value = false
  getSeasons()
}

const deleteSeason = (row) => {
  ElMessageBox.confirm(`您确定要删除赛季 [${row.name}] 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await seasonDeleteService(row.id)
    ElMessage.success('删除成功')
    getSeasons()
  })
}
</script>

<template>
  <el-card class="page-container">
    <template #header>
      <div class="header">
        <span>赛季管理</span>
        <el-button type="primary" @click="showAddDrawer">添加赛季</el-button>
      </div>
    </template>

    <el-form inline class="search-form">
      <el-form-item label="赛季名：">
        <el-input v-model="name" clearable placeholder="如 2025-26" style="width: 200px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="getSeasons">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="seasons" style="width: 100%">
      <el-table-column type="index" width="80" />
      <el-table-column label="赛季名" prop="name" />
      <el-table-column label="开始日期" prop="startDate" />
      <el-table-column label="结束日期" prop="endDate" />
      <el-table-column label="当前赛季" prop="isCurrent">
        <template #default="{ row }">
          <el-tag :type="row.isCurrent === 1 ? 'success' : 'info'">
            {{ row.isCurrent === 1 ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button :icon="Edit" circle type="primary" plain @click="showEditDrawer(row)" />
          <el-button :icon="Delete" circle type="danger" plain @click="deleteSeason(row)" />
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
      <el-form ref="formRef" :model="seasonModel" :rules="rules" label-width="100px">
        <el-form-item label="赛季名" prop="name">
          <el-input v-model="seasonModel.name" placeholder="如 2025-26" maxlength="20" show-word-limit />
        </el-form-item>
        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker
            v-model="seasonModel.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择开始日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束日期" prop="endDate">
          <el-date-picker
            v-model="seasonModel.endDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择结束日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="当前赛季" prop="isCurrent">
          <el-radio-group v-model="seasonModel.isCurrent">
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
</style>
