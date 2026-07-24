<script setup>
import { ref } from 'vue'
import { User, Lock } from '@element-plus/icons-vue'
import { userRegisterService, userLoginService } from '@/api/user'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user' // 1. 引入仓库

const userStore = useUserStore()
const router = useRouter()

// 控制当前展示的是 登录(false) 还是 注册(true)
const isRegister = ref(false)

// 表单数据绑定
const formModel = ref({
  username: '',
  password: '',
  rePassword: '' // 注册时的二次确认密码
})

// 表单自定义校验：校验两次密码是否一致
const checkRePassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== formModel.value.password) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

// 表单校验规则
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 5, max: 16, message: '长度在 5 到 16 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { pattern: /^\S{5,16}$/, message: '密码必须为5-16位的非空字符', trigger: 'blur' }
  ],
  rePassword: [
    { validator: checkRePassword, trigger: 'blur' }
  ]
}

const form = ref(null)

// 执行注册逻辑
const register = async () => {
  // 1. 进行表单预校验
  await form.value.validate()
  // 2. 调用注册 API
  await userRegisterService(formModel.value)
  ElMessage.success('注册成功！')
  // 3. 注册成功后自动切换回登录状态
  isRegister.value = false
}

// 执行登录逻辑
const login = async () => {
  await form.value.validate()
  const res = await userLoginService(formModel.value)
  ElMessage.success('登录成功！')

  // 2. 将 token 保存到 Pinia 仓库中
  userStore.setToken(res.data)

  // 3. 登录成功后，跳转到主页 /
  router.push('/')
}

// 切换状态时清空表单
const changeState = () => {
  isRegister.value = !isRegister.value
  formModel.value = { username: '', password: '', rePassword: '' }
}
</script>

<template>
  <el-row class="login-page">
    <!-- 左侧宣传海报 -->
    <el-col :span="12" class="bg"></el-col>
    <!-- 右侧表单卡片 -->
    <el-col :span="12" class="form-container">
      <!-- 1. 注册表单 -->
      <el-form v-if="isRegister" ref="form" :model="formModel" :rules="rules" size="large" autocomplete="off" class="form">
        <el-form-item>
          <h1>注册</h1>
        </el-form-item>
        <el-form-item prop="username">
          <el-input v-model="formModel.username" :prefix-icon="User" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="formModel.password" :prefix-icon="Lock" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item prop="rePassword">
          <el-input v-model="formModel.rePassword" :prefix-icon="Lock" type="password" placeholder="请再次输入密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="button" auto-insert-space @click="register">注册</el-button>
        </el-form-item>
        <el-form-item class="flex">
          <el-link type="info" :underline="false" @click="changeState">← 返回登录</el-link>
        </el-form-item>
      </el-form>

      <!-- 2. 登录表单 -->
      <el-form v-else ref="form" :model="formModel" :rules="rules" size="large" autocomplete="off" class="form">
        <el-form-item>
          <h1>登录</h1>
        </el-form-item>
        <el-form-item prop="username">
          <el-input v-model="formModel.username" :prefix-icon="User" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="formModel.password" :prefix-icon="Lock" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item class="flex">
          <el-checkbox>记住我</el-checkbox>
          <el-link type="primary" :underline="false">忘记密码？</el-link>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="button" auto-insert-space @click="login">登录</el-button>
        </el-form-item>
        <el-form-item class="flex">
          <el-link type="info" :underline="false" @click="changeState">注册 →</el-link>
        </el-form-item>
      </el-form>
    </el-col>
  </el-row>
</template>

<style lang="scss" scoped>
.login-page {
  height: 100vh;
  background-color: #fff;
  .bg {
    background: url('@/assets/logo2.jpg') no-repeat center / 240px auto,
    url('@/assets/login_big.jpg') no-repeat center / cover;
    border-radius: 0 20px 20px 0;
  }
  .form-container {
    display: flex;
    flex-direction: column;
    justify-content: center;
    user-select: none;
    .form {
      width: 400px;
      margin: 0 auto;
      .button {
        width: 100%;
      }
      .flex {
        width: 100%;
        display: flex;
        justify-content: space-between;
        :deep(.el-form-item__content) {
          justify-content: space-between;
        }
      }
    }
  }
}
</style>