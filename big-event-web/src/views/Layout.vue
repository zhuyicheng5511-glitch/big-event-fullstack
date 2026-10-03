<script setup>
import { onMounted } from 'vue'
import { Basketball, User, Calendar, Timer, Reading, UserFilled, Crop, EditPen, SwitchButton, CaretBottom, Trophy, Aim, PieChart } from '@element-plus/icons-vue'
import { userGetInfoService } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

// 页面加载时自动获取用户信息
const getUserInfo = async () => {
  const res = await userGetInfoService()
  userStore.setUserInfo(res.data) // 将用户信息存入 Pinia 仓库
}

onMounted(() => {
  getUserInfo()
})

// 处理下拉菜单点击事件（如退出登录、更换头像、基本资料等）
const handleCommand = (command) => {
  if (command === 'logout') {
    // 退出登录
    ElMessageBox.confirm('您确定要退出登录吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      userStore.removeToken()
      userStore.removeUserInfo()
      ElMessage.success('已退出登录')
      router.push('/login')
    })
  } else if (command === 'avatar') {
    // === 添加这一行：点击更换头像，跳转到更换头像子页面 ===
    router.push('/user/avatar')
  } else if (command === 'profile') {
    // === 跳转到基本资料子页面（若已实现） ===
    router.push('/user/info')
  } else if (command === 'password') {
    // === 跳转到重置密码子页面（若已实现） ===
    router.push('/user/password')
  }
}
</script>

<template>
  <el-container class="layout-container">
    <!-- 1. 左侧菜单导航栏 -->
    <el-aside width="200px">
      <div class="el-aside__logo"></div>
      <!-- default-active 设置默认激活项 -->
      <el-menu active-text-color="#ffd04b" background-color="#232323" text-color="#fff" router>
        <el-menu-item index="/team">
          <el-icon><Basketball /></el-icon>
          <span>球队</span>
        </el-menu-item>
        <el-menu-item index="/player">
          <el-icon><User /></el-icon>
          <span>球员</span>
        </el-menu-item>
        <el-menu-item index="/season">
          <el-icon><Calendar /></el-icon>
          <span>赛季</span>
        </el-menu-item>
        <el-menu-item index="/game">
          <el-icon><Timer /></el-icon>
          <span>比赛</span>
        </el-menu-item>
        <el-menu-item index="/rank">
          <el-icon><Trophy /></el-icon>
          <span>排行榜</span>
        </el-menu-item>
        <el-menu-item index="/shot-chart">
          <el-icon><Aim /></el-icon>
          <span>投篮热图</span>
        </el-menu-item>
        <el-menu-item index="/player-radar">
          <el-icon><PieChart /></el-icon>
          <span>球员雷达</span>
        </el-menu-item>
        <el-sub-menu index="/article">
          <template #title>
            <el-icon><Reading /></el-icon>
            <span>资讯</span>
          </template>
          <el-menu-item index="/article/category">资讯分类</el-menu-item>
          <el-menu-item index="/article/manage">资讯文章</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/user">
          <template #title>
            <el-icon><UserFilled /></el-icon>
            <span>个人中心</span>
          </template>
          <el-menu-item index="/user/info">基本资料</el-menu-item>
          <el-menu-item index="/user/avatar">更换头像</el-menu-item>
          <el-menu-item index="/user/password">重置密码</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <!-- 2. 右侧主体展示区 -->
    <el-container>
      <!-- 头部 -->
      <el-header>
        <div>
          <!-- 动态展示用户名 -->
          管理员：<strong>{{ userStore.userInfo.nickname || userStore.userInfo.username }}</strong>
        </div>
        <!-- 右侧用户下拉菜单 -->
        <el-dropdown class="el-dropdown-link" @command="handleCommand">
                    <span class="el-dropdown__box">
                        <!-- 动态展示头像，若没有则显示默认灰色头像 -->
                        <el-avatar :src="userStore.userInfo.userPic || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" />
                        <el-icon><CaretBottom /></el-icon>
                    </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile" :icon="User">基本资料</el-dropdown-item>
              <el-dropdown-item command="avatar" :icon="Crop">更换头像</el-dropdown-item>
              <el-dropdown-item command="password" :icon="EditPen">重置密码</el-dropdown-item>
              <el-dropdown-item command="logout" :icon="SwitchButton" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <!-- 中间核心渲染区 -->
      <el-main>
        <!-- 这里用来承载子页面路由 (/article/category 或 /article/manage) -->
        <router-view></router-view>
      </el-main>

      <!-- 底部声明 -->
      <el-footer>Hoops 篮球数据平台 ©2026 Created by 厦门理工学院2024级软件3班朱毅城</el-footer>
    </el-container>
  </el-container>
</template>

<style lang="scss" scoped>
.layout-container {
  height: 100vh;
  .el-aside {
    background-color: #232323;
    &__logo {
      height: 120px;
      background: url('@/assets/logo.png') no-repeat center / 120px auto;
    }
    .el-menu {
      border-right: none;
    }
  }
  .el-header {
    background-color: #fff;
    display: flex;
    align-items: center;
    justify-content: space-between;
    border-bottom: 1px solid #f1f1f1;
    .el-dropdown__box {
      display: flex;
      align-items: center;
      outline: none;
      cursor: pointer;
      .el-avatar {
        margin-right: 5px;
      }
    }
  }
  .el-footer {
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    color: #999;
    border-top: 1px solid #f1f1f1;
  }
}
</style>