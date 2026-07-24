<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { userUpdateInfoService } from '@/api/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()

// 表单绑定数据
const userInfoModel = ref({
  username: '',
  nickname: '',
  email: ''
})

// 回显 Pinia 仓库中当前登录用户的数据
onMounted(() => {
  // 使用浅拷贝，防止直接修改表单数据时，右上角未提交的昵称跟着变
  userInfoModel.value = { ...userStore.userInfo }
})

// 表单校验规则
const rules = {
  nickname: [
    { required: true, message: '请输入用户昵称', trigger: 'blur' },
    { pattern: /^\S{1,10}$/, message: '昵称必须是1-10位的非空字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入用户邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ]
}

const formRef = ref(null)

// 提交修改
const onSubmit = async () => {
  // 1. 表单预校验
  await formRef.value.validate()

  // 2. 调用修改资料接口
  await userUpdateInfoService(userInfoModel.value)

  // 3. 核心：更新成功后，同步将新数据写入 Pinia 状态仓库，实现页面头部的响应式同步更新
  userStore.setUserInfo(userInfoModel.value)

  ElMessage.success('修改个人资料成功！')
}
</script>

<template>
  <el-card class="page-container">
    <template #header>
      <div class="header">
        <span>基本资料</span>
      </div>
    </template>
    <el-row>
      <el-col :span="12">
        <el-form ref="formRef" :model="userInfoModel" :rules="rules" label-width="100px" size="large">
          <el-form-item label="登录用户名">
            <!-- 用户名禁止修改 -->
            <el-input v-model="userInfoModel.username" disabled />
          </el-form-item>
          <el-form-item label="用户昵称" prop="nickname">
            <el-input v-model="userInfoModel.nickname" placeholder="请输入用户昵称" />
          </el-form-item>
          <el-form-item label="用户邮箱" prop="email">
            <el-input v-model="userInfoModel.email" placeholder="请输入用户邮箱" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="onSubmit">提交修改</el-button>
          </el-form-item>
        </el-form>
      </el-col>
    </el-row>
  </el-card>
</template>

<style lang="scss" scoped>
.page-container {
  min-height: 100%;
  box-sizing: border-box;
}
</style>