import { createRouter, createWebHistory } from 'vue-router'
import LoginVue from '@/views/Login.vue'
import LayoutVue from '@/views/Layout.vue'
import ArticleCategoryVue from '@/views/ArticleCategory.vue'
import ArticleManageVue from '@/views/ArticleManage.vue'
import UserAvatarVue from '@/views/UserAvatar.vue'
import UserPasswordVue from '@/views/UserPassword.vue'
import UserInfoVue from '@/views/UserInfo.vue' // 1. 引入基本资料组件

// 引入 Pinia 的用户仓库
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/login', component: LoginVue },
    {
      path: '/',
      component: LayoutVue,
      redirect: '/article/manage',
      children: [
        { path: '/article/category', component: ArticleCategoryVue },
        { path: '/article/manage', component: ArticleManageVue },
        { path: '/user/avatar', component: UserAvatarVue },
        { path: '/user/password', component: UserPasswordVue },
        { path: '/user/info', component: UserInfoVue } // 2. 注册基本资料子路由
      ]
    }
  ]
})

// 全局前置路由守卫
router.beforeEach((to, from) => {
  const userStore = useUserStore()
  if (to.path !== '/login' && !userStore.token) {
    return '/login'
  }
  if (to.path === '/login' && userStore.token) {
    return '/'
  }
  return true
})

export default router