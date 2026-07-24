<script setup>
import { ref, onMounted } from 'vue'
import { Edit, Delete, Plus } from '@element-plus/icons-vue'
import { categoryListService } from '@/api/category'
import { articleListService, articleAddService, articleUpdateService, articleDeleteService } from '@/api/article'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()

// 数据绑定
const categorys = ref([])
const articles = ref([])

// 分页与条件查询参数
const pageNum = ref(1)
const total = ref(0)
const pageSize = ref(5)
const categoryId = ref('')
const state = ref('')

// 获取分类下拉列表
const getCategorys = async () => {
  const res = await categoryListService()
  categorys.value = res.data
}

// 获取文章列表
const getArticles = async () => {
  const params = {
    pageNum: pageNum.value,
    pageSize: pageSize.value,
    categoryId: categoryId.value ? categoryId.value : null,
    state: state.value ? state.value : null
  }
  const res = await articleListService(params)
  total.value = res.data.total
  articles.value = res.data.items
}

onMounted(() => {
  getCategorys()
  getArticles()
})

const handleCurrentChange = (val) => {
  pageNum.value = val
  getArticles()
}

const handleSizeChange = (val) => {
  pageSize.value = val
  getArticles()
}

const resetSearch = () => {
  categoryId.value = ''
  state.value = ''
  pageNum.value = 1
  getArticles()
}

// ================== 文章详情（点击标题弹窗） ==================
const detailVisible = ref(false) // 详情弹窗控制
const articleDetail = ref({})    // 存放详情数据

const showDetail = (row) => {
  detailVisible.value = true
  articleDetail.value = row
}

// ================== 新增/修改文章（右侧抽屉表单逻辑） ==================
const visibleDrawer = ref(false)
const drawerTitle = ref('')

const articleModel = ref({
  title: '',
  categoryId: '',
  coverImg: '',
  content: '',
  state: ''
})

const handleAvatarSuccess = (response) => {
  articleModel.value.coverImg = response.data
  ElMessage.success('图片上传成功！')
}

// 打开新增抽屉
const showAddDrawer = () => {
  visibleDrawer.value = true
  drawerTitle.value = '添加文章'
  articleModel.value = { title: '', categoryId: '', coverImg: '', content: '', state: '' }
}

// 打开编辑抽屉 (回显数据)
const showEditDrawer = (row) => {
  visibleDrawer.value = true
  drawerTitle.value = '修改文章'
  articleModel.value = { ...row } // 回显数据
}

// 提交发布或修改
const pubArticle = async (clickState) => {
  articleModel.value.state = clickState

  if (!articleModel.value.title || !articleModel.value.categoryId || !articleModel.value.content) {
    ElMessage.error('请填写完整的标题、分类和内容！')
    return
  }

  if (articleModel.value.id) {
    // 有 id，说明是修改
    await articleUpdateService(articleModel.value)
    ElMessage.success('修改文章成功！')
  } else {
    // 无 id，说明是新增
    await articleAddService(articleModel.value)
    ElMessage.success(clickState === '已发布' ? '发布文章成功！' : '草稿保存成功！')
  }

  visibleDrawer.value = false
  getArticles()
}

// ================== 删除文章 ==================
const deleteArticle = (row) => {
  ElMessageBox.confirm(
      `您确定要删除文章 [${row.title}] 吗?`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
  ).then(async () => {
    await articleDeleteService(row.id)
    ElMessage.success('删除成功！')
    getArticles()
  })
}
</script>

<template>
  <el-card class="page-container">
    <template #header>
      <div class="header">
        <span>文章管理</span>
        <el-button type="primary" @click="showAddDrawer">发布文章</el-button>
      </div>
    </template>

    <!-- 筛选表单 -->
    <el-form inline class="search-form">
      <el-form-item label="文章分类：">
        <el-select v-model="categoryId" placeholder="请选择" style="width: 200px">
          <el-option v-for="c in categorys" :key="c.id" :label="c.categoryName" :value="c.id" />
        </el-select>
      </el-form-item>

      <el-form-item label="发布状态：">
        <el-select v-model="state" placeholder="请选择" style="width: 200px">
          <el-option label="已发布" value="已发布" />
          <el-option label="草稿" value="草稿" />
        </el-select>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="getArticles">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 表格 -->
    <el-table :data="articles" style="width: 100%">
      <el-table-column label="文章标题" prop="title">
        <template #default="{ row }">
          <!-- 绑定点击事件，展示文章详情 -->
          <el-link type="primary" :underline="false" @click="showDetail(row)">{{ row.title }}</el-link>
        </template>
      </el-table-column>
      <el-table-column label="分类ID" prop="categoryId" width="100" />
      <el-table-column label="发表时间" prop="createTime" />
      <el-table-column label="状态" prop="state">
        <template #default="{ row }">
          <el-tag :type="row.state === '已发布' ? 'success' : 'info'">{{ row.state }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <!-- 绑定修改和删除事件 -->
          <el-button :icon="Edit" circle type="primary" plain @click="showEditDrawer(row)" />
          <el-button :icon="Delete" circle type="danger" plain @click="deleteArticle(row)" />
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="没有数据" />
      </template>
    </el-table>

    <!-- 分页 -->
    <el-pagination class="pagination" v-model:current-page="pageNum" v-model:page-size="pageSize"
                   :page-sizes="[5, 10, 15]" layout="jumper, total, sizes, prev, pager, next" :total="total"
                   @size-change="handleSizeChange" @current-change="handleCurrentChange" />

    <!-- 4. 右侧抽屉表单（新增/修改共用） -->
    <el-drawer v-model="visibleDrawer" :title="drawerTitle" size="50%">
      <el-form :model="articleModel" label-width="80px">
        <el-form-item label="文章标题">
          <el-input v-model="articleModel.title" placeholder="请输入标题" />
        </el-form-item>

        <el-form-item label="文章分类">
          <el-select v-model="articleModel.categoryId" placeholder="请选择">
            <el-option v-for="c in categorys" :key="c.id" :label="c.categoryName" :value="c.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="文章封面">
          <el-upload class="avatar-uploader" action="/api/upload" :show-file-list="false"
                     :headers="{ Authorization: userStore.token }" :on-success="handleAvatarSuccess">
            <img v-if="articleModel.coverImg" :src="articleModel.coverImg" class="avatar" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>

        <el-form-item label="文章内容">
          <el-input v-model="articleModel.content" type="textarea" :rows="10" placeholder="请输入文章内容" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="pubArticle(articleModel.state ? articleModel.state : '已发布')">保存</el-button>
          <el-button v-if="!articleModel.id" type="info" @click="pubArticle('草稿')">草稿</el-button>
        </el-form-item>
      </el-form>
    </el-drawer>

    <!-- 5. 弹窗：文章详情查看 -->
    <el-dialog v-model="detailVisible" title="文章详情" width="50%">
      <div class="detail-container">
        <h2>{{ articleDetail.title }}</h2>
        <div class="meta">
          <span>发布时间：{{ articleDetail.createTime }}</span>
          <span>状态：<el-tag size="small" :type="articleDetail.state === '已发布' ? 'success' : 'info'">{{ articleDetail.state }}</el-tag></span>
        </div>
        <hr />
        <div v-if="articleDetail.coverImg" class="cover">
          <img :src="articleDetail.coverImg" alt="文章封面" />
        </div>
        <div class="content">
          <p style="white-space: pre-wrap;">{{ articleDetail.content }}</p>
        </div>
      </div>
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
  .search-form {
    margin-bottom: 20px;
  }
  .pagination {
    margin-top: 20px;
    justify-content: flex-end;
  }
}

// 详情弹窗样式
.detail-container {
  h2 {
    margin-bottom: 10px;
  }
  .meta {
    font-size: 13px;
    color: #999;
    display: flex;
    gap: 20px;
    margin-bottom: 15px;
  }
  .cover {
    text-align: center;
    margin: 15px 0;
    img {
      max-width: 100%;
      max-height: 300px;
      object-fit: contain;
      border-radius: 4px;
    }
  }
  .content {
    line-height: 1.8;
    font-size: 15px;
    color: #333;
  }
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