<template>
  <div class="list-page">
    <div class="page-header">
      <div class="header-left">
        <h2>发现</h2>
        <span class="header-count" v-if="total">共 {{ total }} 篇文章</span>
      </div>
      <el-input
        v-model="keyword"
        placeholder="搜索文章..."
        :prefix-icon="Search"
        class="search-input"
        clearable
        @input="onSearch"
      />
    </div>

    <div class="tag-filter" v-if="allTags.length">
      <el-tag
        v-for="tag in allTags"
        :key="tag"
        :type="selectedTag === tag ? 'primary' : 'info'"
        :effect="selectedTag === tag ? 'dark' : 'plain'"
        size="large"
        class="filter-tag"
        @click="selectTag(tag)"
      >
        {{ tag }}
      </el-tag>
      <el-button v-if="selectedTag" size="small" text @click="selectTag('')">清除筛选</el-button>
    </div>

    <div v-if="loading" class="loading-area">
      <el-skeleton :rows="3" animated />
    </div>

    <div v-else-if="list.length === 0" class="empty-state">
      <el-empty description="暂无文章" />
    </div>

    <div v-else class="card-grid">
      <ArticleCard
        v-for="item in list"
        :key="item.id"
        :article="item"
        :cover-url="item.imageUrls && item.imageUrls[0]"
        @click="viewArticle(item.id)"
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
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getArticleList } from '@/api/article.js'
import ArticleCard from '@/components/user/articlelist/ArticleCard.vue'

const router = useRouter()
const list = ref([])
const loading = ref(true)
const keyword = ref('')
const selectedTag = ref('')
const currentPage = ref(1)
const total = ref(0)
const pageSize = 12

let searchTimer = null

const allTags = computed(() => {
  const tags = new Set()
  list.value.forEach(item => {
    if (item.tag && Array.isArray(item.tag)) {
      item.tag.forEach(t => tags.add(t))
    }
  })
  return [...tags].sort()
})

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const params = { page: currentPage.value, size: pageSize }
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    if (selectedTag.value) params.tag = selectedTag.value
    const res = await getArticleList(params)
    const body = res.data
    if (Array.isArray(body)) {
      list.value = body
      total.value = body.length
    } else {
      list.value = body.articles || []
      total.value = body.total || 0
    }
  } catch {
    ElMessage.error('加载文章失败')
  } finally {
    loading.value = false
  }
}

function selectTag(tag) {
  selectedTag.value = selectedTag.value === tag ? '' : tag
  currentPage.value = 1
  fetchData()
}

function onSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => { currentPage.value = 1; fetchData() }, 300)
}

function viewArticle(id) {
  router.push(`/article/${id}`)
}
</script>

<style scoped>
.list-page { max-width: 1200px; margin: 0 auto; }

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  gap: 20px;
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.page-header h2 {
  font-size: 24px;
  font-weight: 700;
  color: var(--c-text);
  margin: 0;
}

.header-count {
  font-size: 14px;
  color: var(--c-text-muted);
}

.search-input { max-width: 280px; }

.tag-filter {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 24px;
  padding: 12px 16px;
  background: var(--c-surface);
  border-radius: var(--radius-sm);
  border: 1px solid var(--c-border);
}

.filter-tag {
  cursor: pointer;
  transition: all var(--transition);
}

.filter-tag:hover { transform: translateY(-1px); }

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
