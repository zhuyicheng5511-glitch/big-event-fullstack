<script setup>
import { ref } from 'vue'
import { Plus, Upload } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { userUpdateAvatarService } from '@/api/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()

// 引入图片上传所需组件的上传引用
const uploadRef = ref(null)

// 临时存放上传成功的图片 URL
const imgUrl = ref(userStore.userInfo.userPic)

// 图片上传成功后的回调（利用我们之前写好的 /upload 接口）
const handleAvatarSuccess = (response) => {
  // 上传接口失败时也返回 HTTP 200，必须判断业务 code，否则会"上传失败却提示成功"
  if (response.code !== 0) {
    ElMessage.error(response.message || '图片上传失败')
    return
  }
  imgUrl.value = response.data
}

// 提交更新头像
const updateAvatar = async () => {
  if (!imgUrl.value) {
    ElMessage.error('请先选择并上传图片！')
    return
  }
  // 1. 调用后端更新头像 API
  await userUpdateAvatarService(imgUrl.value)

  // 2. 核心：同步更新 Pinia 状态仓库中的用户头像
  userStore.userInfo.userPic = imgUrl.value

  ElMessage.success('更换头像成功！')
}
</script>

<template>
  <el-card class="page-container">
    <template #header>
      <div class="header">
        <span>更换头像</span>
      </div>
    </template>
    <el-row>
      <el-col :span="12">
        <!--
           图片上传区
           headers 中必须手动携带 Token 手环
        -->
        <el-upload ref="uploadRef" class="avatar-uploader" action="/api/upload" :show-file-list="false"
                   :headers="{ Authorization: userStore.token }" :on-success="handleAvatarSuccess">
          <img v-if="imgUrl" :src="imgUrl" class="avatar" />
          <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
        </el-upload>
        <br />
        <!-- 底部操作按钮 -->
        <el-button type="primary" :icon="Plus" size="large" @click="uploadRef.$el.querySelector('input').click()">
          选择图片
        </el-button>
        <el-button type="success" :icon="Upload" size="large" @click="updateAvatar">
          上传头像
        </el-button>
      </el-col>
    </el-row>
  </el-card>
</template>

<style lang="scss" scoped>
.page-container {
  min-height: 100%;
  box-sizing: border-box;
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
    width: 278px;
    height: 278px;
    text-align: center;
    line-height: 278px;
  }
  .avatar {
    width: 278px;
    height: 278px;
    display: block;
    object-fit: cover;
  }
}
</style>