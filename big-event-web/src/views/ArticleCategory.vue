<script setup>
import { ref, onMounted } from 'vue'
import { Edit, Delete } from '@element-plus/icons-vue'
import { categoryListService, categoryAddService, categoryUpdateService, categoryDeleteService } from '@/api/category'
import { ElMessage, ElMessageBox } from 'element-plus'

// 分类列表数据
const categorys = ref([])

// 获取所有分类
const getCategorys = async () => {
  const res = await categoryListService()
  categorys.value = res.data
}

onMounted(() => {
  getCategorys()
})

// 控制弹窗显示
const dialogVisible = ref(false)
// 弹窗标题 (添加分类 / 修改分类)
const dialogTitle = ref('')

// 弹窗表单数据
const categoryModel = ref({
  categoryName: '',
  categoryAlias: ''
})

// 表单校验规则
const rules = {
  categoryName: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { pattern: /^\S{1,10}$/, message: '分类名必须是1-10位的非空字符', trigger: 'blur' }
  ],
  categoryAlias: [
    { required: true, message: '请输入分类别名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9]{1,15}$/, message: '分类别名必须是1-15位的字母或数字', trigger: 'blur' }
  ]
}

const formRef = ref(null)

// 打开弹窗 (row有值代表修改，没值代表新增)
const showDialog = (row) => {
  dialogVisible.value = true
  if (row) {
    dialogTitle.value = '修改分类'
    // 回显数据 (浅拷贝，防止修改弹窗数据时直接影响表格)
    categoryModel.value = { ...row }
  } else {
    dialogTitle.value = '添加分类'
    categoryModel.value = { categoryName: '', categoryAlias: '' }
  }
}

// 弹窗确定按钮点击事件（新增或修改）
const handleConfirm = async () => {
  // 1. 表单预校验
  await formRef.value.validate()

  // 2. 判断是修改还是新增
  if (categoryModel.value.id) {
    // 有 id 说明是修改
    await categoryUpdateService(categoryModel.value)
    ElMessage.success('修改分类成功！')
  } else {
    // 无 id 说明是新增
    await categoryAddService(categoryModel.value)
    ElMessage.success('添加分类成功！')
  }

  // 3. 关闭弹窗并重新获取列表
  dialogVisible.value = false
  getCategorys()
}

// 删除分类逻辑
const deleteCategory = (row) => {
  ElMessageBox.confirm(
      `您确定要删除 [${row.categoryName}] 分类吗?`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
  ).then(async () => {
    // 调用删除接口
    await categoryDeleteService(row.id)
    ElMessage.success('删除成功')
    // 重新刷新列表
    getCategorys()
  })
}
</script>

<template>
  <el-card class="page-container">
    <!-- 卡片头部 -->
    <template #header>
      <div class="header">
        <span>资讯分类</span>
        <el-button type="primary" @click="showDialog()">添加分类</el-button>
      </div>
    </template>

    <!-- 表格数据展示 -->
    <el-table :data="categorys" style="width: 100%">
      <el-table-column label="序号" width="100" type="index" />
      <el-table-column label="分类名称" prop="categoryName" />
      <el-table-column label="分类别名" prop="categoryAlias" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button :icon="Edit" circle type="primary" plain @click="showDialog(row)" />
          <el-button :icon="Delete" circle type="danger" plain @click="deleteCategory(row)" />
        </template>
      </el-table-column>
      <!-- 无数据时的展示 -->
      <template #empty>
        <el-empty description="没有数据" />
      </template>
    </el-table>

    <!-- 新增/修改弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="30%">
      <el-form ref="formRef" :model="categoryModel" :rules="rules" label-width="100px" style="padding-right: 30px">
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="categoryModel.categoryName" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="分类别名" prop="categoryAlias">
          <el-input v-model="categoryModel.categoryAlias" placeholder="请输入分类别名" />
        </el-form-item>
      </el-form>
      <template #footer>
                <span class="dialog-footer">
                    <el-button @click="dialogVisible = false">取消</el-button>
                    <el-button type="primary" @click="handleConfirm"> 确定 </el-button>
                </span>
      </template>
    </el-dialog>
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
}
</style>