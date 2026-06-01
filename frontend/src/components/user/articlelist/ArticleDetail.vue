<template>
  <div class="article-detail" v-loading="loading">
    <div v-if="article.id" class="detail-content">
      <div class="detail-header">
        <h1 class="detail-title">{{ article.title }}</h1>
        <div class="detail-meta">
          <span class="meta-author" v-if="article.writerName">
            <el-icon><User /></el-icon> {{ article.writerName }}
          </span>
          <span class="meta-date">
            <el-icon><Clock /></el-icon> {{ formatDate(article.gmtCreate) }}
          </span>
          <span class="meta-date" v-if="article.gmtModified !== article.gmtCreate">
            （更新于 {{ formatDate(article.gmtModified) }}）
          </span>
        </div>
        <div class="detail-tags" v-if="article.tag && article.tag.length">
          <el-tag v-for="tag in article.tag" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
        </div>
      </div>

      <div class="detail-actions" v-if="isAuthor">
        <el-button size="small" @click="handleEdit">
          <el-icon><Edit /></el-icon> 编辑
        </el-button>
        <el-popconfirm title="确定删除？" @confirm="handleDelete">
          <template #reference>
            <el-button size="small" type="danger">
              <el-icon><Delete /></el-icon> 删除
            </el-button>
          </template>
        </el-popconfirm>
      </div>

      <div v-if="imageUrls.length" class="detail-gallery">
        <el-image
          v-for="(url, idx) in imageUrls"
          :key="idx"
          :src="url"
          fit="contain"
          :preview-src-list="imageUrls"
          :initial-index="idx"
          class="gallery-img"
          lazy
        />
      </div>

      <div class="detail-body">
        <div class="markdown-body" v-html="renderedContent" />
      </div>
    </div>

    <div v-else-if="!loading" class="empty-state">
      <el-empty description="文章不存在" />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Clock, Edit, Delete } from '@element-plus/icons-vue'
import { getArticleById, deleteArticle } from '@/api/article.js'
import { marked } from 'marked'

const route = useRoute()
const router = useRouter()
const loading = ref(true)

const article = reactive({
  id: 0, title: '', content: '', tag: [],
  image: [], imageUrls: [],
  writerId: 0, writerName: '',
  gmtCreate: '', gmtModified: ''
})

const isAuthor = computed(() => {
  try {
    const user = JSON.parse(localStorage.getItem('user') || '{}')
    return user.id && user.id === article.writerId
  } catch { return false }
})

const imageUrls = computed(() => article.imageUrls || [])
const renderedContent = computed(() => marked(article.content || ''))

onMounted(async () => {
  try {
    const res = await getArticleById(route.params.id)
    Object.assign(article, res.data)
  } catch {
    ElMessage.error('加载文章失败')
  } finally {
    loading.value = false
  }
})

function handleEdit() {
  router.push(`/editor?edit=${article.id}`)
}

async function handleDelete() {
  try {
    await deleteArticle(article.id)
    ElMessage.success('已删除')
    router.back()
  } catch {
    ElMessage.error('删除失败')
  }
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
</script>

<style scoped>
.article-detail {
  max-width: 820px;
  margin: 0 auto;
  padding: 24px 0;
}

.detail-header {
  margin-bottom: 28px;
}

.detail-title {
  font-size: 34px;
  font-weight: 700;
  color: var(--c-text);
  letter-spacing: -0.02em;
  line-height: 1.35;
  margin: 0 0 16px;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 20px;
  font-size: 14px;
  color: var(--c-text-muted);
  margin-bottom: 14px;
}

.meta-author, .meta-date {
  display: flex;
  align-items: center;
  gap: 4px;
}

.detail-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.detail-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 28px;
  padding: 12px 16px;
  background: var(--c-primary-light);
  border-radius: var(--radius-sm);
}

.detail-gallery {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
  margin-bottom: 36px;
}

.gallery-img {
  border-radius: var(--radius-sm);
  overflow: hidden;
  cursor: pointer;
  border: 1px solid var(--c-border);
}

.detail-body {
  background: var(--c-surface);
  border-radius: var(--radius);
  padding: 40px;
  border: 1px solid var(--c-border);
  box-shadow: var(--c-shadow);
}

.markdown-body {
  font-size: 16px;
  line-height: 1.9;
  color: var(--c-text);
}

.markdown-body :deep(h1) { font-size: 2em; margin: 0.67em 0; }
.markdown-body :deep(h2) { font-size: 1.5em; margin: 0.75em 0 0.5em; padding-bottom: 0.3em; border-bottom: 1px solid var(--c-border); }
.markdown-body :deep(h3) { font-size: 1.25em; margin: 0.8em 0 0.4em; }
.markdown-body :deep(p) { margin: 0.8em 0; }
.markdown-body :deep(pre) { background: #f6f8fa; border-radius: var(--radius-sm); padding: 16px; overflow-x: auto; }
.markdown-body :deep(code) { font-family: 'SF Mono', Consolas, monospace; font-size: 0.9em; background: #f6f8fa; padding: 2px 6px; border-radius: 4px; }
.markdown-body :deep(pre code) { background: none; padding: 0; }
.markdown-body :deep(blockquote) { margin: 1em 0; padding: 0 1em; color: var(--c-text-secondary); border-left: 4px solid var(--c-primary); }
.markdown-body :deep(img) { max-width: 100%; border-radius: var(--radius-sm); }
.markdown-body :deep(ul), .markdown-body :deep(ol) { padding-left: 2em; margin: 0.5em 0; }
.markdown-body :deep(li) { margin: 0.3em 0; }
.markdown-body :deep(table) { border-collapse: collapse; width: 100%; margin: 1em 0; }
.markdown-body :deep(th), .markdown-body :deep(td) { border: 1px solid var(--c-border); padding: 8px 12px; text-align: left; }
.markdown-body :deep(th) { background: #f6f8fa; font-weight: 600; }
.markdown-body :deep(a) { color: var(--c-primary); }

.empty-state {
  padding: 120px 0;
  text-align: center;
}
</style>
