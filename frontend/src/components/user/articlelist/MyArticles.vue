<template>
  <div class="my-articles">
    <div class="page-header">
      <div class="header-left">
        <h2>我的文章</h2>
        <span class="article-count" v-if="total">{{ total }} 篇</span>
      </div>
      <el-button type="primary" @click="$router.push('/editor')">
        <el-icon><Plus /></el-icon> 写文章
      </el-button>
    </div>

    <div v-if="loading" class="loading-area">
      <el-skeleton :rows="3" animated />
    </div>

    <div v-else-if="list.length === 0" class="empty-state">
      <el-empty description="还没有文章">
        <el-button type="primary" @click="$router.push('/editor')">去写第一篇</el-button>
      </el-empty>
    </div>

    <div v-else class="card-grid">
      <ArticleCard
        v-for="item in list"
        :key="item.id"
        :article="item"
        :cover-url="item.imageUrls && item.imageUrls[0]"
        :show-actions="true"
        @click="viewArticle(item.id)"
        @edit="editArticle(item.id)"
        @delete="handleDelete(item.id)"
      />
    </div>

    <div class="pagination" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchData"
        background
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getMyArticles, deleteArticle } from '@/api/article.js'
import ArticleCard from '@/components/user/articlelist/ArticleCard.vue'

const router = useRouter()
const list = ref([])
const loading = ref(true)
const currentPage = ref(1)
const total = ref(0)
const pageSize = 10

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const res = await getMyArticles({ page: currentPage.value, size: pageSize })
    list.value = res.data.articles || []
    total.value = res.data.total || 0
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function viewArticle(id) { router.push(`/article/${id}`) }
function editArticle(id) { router.push(`/editor?edit=${id}`) }

async function handleDelete(id) {
  try {
    await deleteArticle(id)
    ElMessage.success('已删除')
    fetchData()
  } catch {
    ElMessage.error('删除失败')
  }
}
</script>

<style scoped>
.my-articles { max-width: 960px; margin: 0 auto; }

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28px;
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.header-left h2 {
  font-size: 24px;
  font-weight: 700;
  color: var(--c-text);
  margin: 0;
}

.article-count {
  font-size: 14px;
  color: var(--c-text-muted);
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
}

.loading-area { padding: 40px 0; }
.empty-state { padding: 80px 0; }

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 40px;
  padding-bottom: 24px;
}
</style>
