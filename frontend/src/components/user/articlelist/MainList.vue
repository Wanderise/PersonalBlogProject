<script setup>
import { onMounted, ref, computed } from "vue"
import { Search } from '@element-plus/icons-vue'
import { getArticleList } from "@/api/article.js"

const searchKey = ref('')
const list = ref([])

const filteredList = computed(() => {
  if (!searchKey.value.trim()) return list.value
  const keyword = searchKey.value.toLowerCase()
  return list.value.filter(item =>
    (item.title && item.title.toLowerCase().includes(keyword)) ||
    (item.content && item.content.toLowerCase().includes(keyword))
  )
})

const fetchData = async () => {
  try {
    const res = await getArticleList()
    list.value = res.data
  } catch (error) {
    console.log(error)
  }
}

onMounted(() => {
  fetchData()
})

function stripMarkdown(text) {
  if (!text) return ''
  return text
    .replace(/#{1,6}\s/g, '')
    .replace(/\*\*(.+?)\*\*/g, '$1')
    .replace(/\*(.+?)\*/g, '$1')
    .replace(/```[\s\S]*?```/g, '')
    .replace(/`(.+?)`/g, '$1')
    .replace(/\[(.+?)\]\(.+?\)/g, '$1')
    .replace(/!\[.*?\]\(.*?\)/g, '')
    .replace(/>\s/g, '')
    .replace(/[-*+]\s/g, '')
    .replace(/\n+/g, ' ')
    .trim()
    .substring(0, 200)
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
</script>

<template>
  <div class="list-page">
    <div class="page-header">
      <h2>文章列表</h2>
      <el-input
        v-model="searchKey"
        placeholder="搜索文章标题或内容..."
        :prefix-icon="Search"
        class="search-input"
        clearable
      />
    </div>

    <div v-if="filteredList.length === 0" class="empty-state">
      <p>暂无文章</p>
    </div>

    <div v-else class="card-grid">
      <el-card
        v-for="item in filteredList"
        :key="item.id"
        class="article-card"
        shadow="hover"
      >
        <template #header>
          <div class="card-header">
            <h3 class="card-title">{{ item.title || '无标题' }}</h3>
            <span class="card-date">{{ formatDate(item.gmtCreate) }}</span>
          </div>
        </template>
        <p class="card-content">{{ stripMarkdown(item.content) }}</p>
        <div class="card-tags" v-if="item.tag && item.tag.length">
          <el-tag
            v-for="tag in item.tag"
            :key="tag"
            size="small"
            class="tag-item"
            type="info"
          >
            {{ tag }}
          </el-tag>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.list-page {
  max-width: 960px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28px;
  gap: 20px;
}

.page-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #1a1a2e;
  flex-shrink: 0;
}

.search-input {
  max-width: 320px;
}

.card-grid {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.article-card {
  border-radius: 10px;
  transition: transform 0.15s;
}

.article-card:hover {
  transform: translateY(-1px);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-size: 17px;
  font-weight: 600;
  color: #1a1a2e;
}

.card-date {
  font-size: 12px;
  color: #999;
  flex-shrink: 0;
  margin-left: 16px;
}

.card-content {
  font-size: 14px;
  color: #666;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-tags {
  margin-top: 14px;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.tag-item {
  border-radius: 4px;
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: #bbb;
  font-size: 16px;
}
</style>
