<template>
  <div class="article-detail" v-loading="loading">
    <div v-if="article.id" class="detail-content">
      <div class="detail-header">
        <h1 class="detail-title">{{ article.title }}</h1>
        <div class="detail-meta">
          <span class="meta-author" v-if="article.writerName">
            <div class="author-avatar">{{ article.writerName.charAt(0) }}</div>
            {{ article.writerName }}
          </span>
          <span class="meta-sep">·</span>
          <span class="meta-date">
            <el-icon><Clock /></el-icon> {{ formatDate(article.gmtCreate) }}
          </span>
          <span class="meta-update" v-if="article.gmtModified !== article.gmtCreate">
            （更新于 {{ formatDate(article.gmtModified) }}）
          </span>
        </div>
        <div class="detail-tags" v-if="article.tag && article.tag.length">
          <el-tag v-for="tag in article.tag" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
        </div>
      </div>

      <div class="detail-actions" v-if="isAuthor">
        <el-button @click="handleEdit">
          <el-icon><Edit /></el-icon> 编辑
        </el-button>
        <el-button @click="openVersionHistory">
          <el-icon><Clock /></el-icon> 版本历史
        </el-button>
        <el-popconfirm title="确定删除？" @confirm="handleDelete">
          <template #reference>
            <el-button type="danger">
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

    <!-- 版本历史弹窗 -->
    <el-dialog v-model="versionDialogVisible" title="版本历史" width="680px" :close-on-click-modal="false">
      <div v-loading="versionLoading" class="version-dialog-body">
        <div v-if="versions.length" class="version-list">
          <div
            v-for="v in versions"
            :key="v.id"
            class="version-item"
            :class="{ expanded: expandedVersion === v.id }"
          >
            <div class="version-header" @click="toggleVersion(v)">
              <span class="version-badge">v{{ formatVersion(v.version) }}</span>
              <span class="version-title">{{ v.title }}</span>
              <span class="version-date">{{ formatDate(v.createTime) }}</span>
              <el-icon class="version-arrow" v-if="expandedVersion !== v.id"><ArrowDown /></el-icon>
              <el-icon class="version-arrow" v-else><ArrowUp /></el-icon>
            </div>
            <div v-if="expandedVersion === v.id" class="version-content">
              <div v-loading="previewLoading && previewVersionId === v.id" class="version-preview">
                <div v-if="previewContent" class="markdown-body" v-html="previewContent"></div>
                <div v-else class="preview-empty">加载中...</div>
              </div>
              <div class="version-actions">
                <el-button
                  type="primary"
                  size="small"
                  :loading="rollbackLoading === v.id"
                  @click.stop="handleRollback(v)"
                >
                  回滚到此版本
                </el-button>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="version-empty">暂无历史版本</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Clock, Edit, Delete, ArrowDown, ArrowUp } from '@element-plus/icons-vue'
import { getArticleById, deleteArticle, getArticleVersions, getArticleVersion, rollbackArticleVersion } from '@/api/article.js'
import { marked } from 'marked'
import { ElMessageBox } from 'element-plus'

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

function handleEdit() { router.push(`/editor?edit=${article.id}`) }

async function handleDelete() {
  try {
    await deleteArticle(article.id)
    ElMessage.success('已删除')
    router.back()
  } catch {
    ElMessage.error('删除失败')
  }
}

/* ========== 版本管理 ========== */
const versionDialogVisible = ref(false)
const versionLoading = ref(false)
const versions = ref([])
const expandedVersion = ref(null)
const previewVersionId = ref(null)
const previewContent = ref('')
const previewLoading = ref(false)
const rollbackLoading = ref(null)

function formatVersion(num) {
  if (num == null) return '0.0'
  return Number(num).toFixed(2)
}

async function openVersionHistory() {
  versionDialogVisible.value = true
  versionLoading.value = true
  try {
    const res = await getArticleVersions(article.id)
    versions.value = res.data || []
  } catch { ElMessage.error('加载版本历史失败') }
  versionLoading.value = false
}

async function toggleVersion(v) {
  if (expandedVersion.value === v.id) {
    expandedVersion.value = null
    previewContent.value = ''
    return
  }
  expandedVersion.value = v.id
  previewVersionId.value = v.id
  previewContent.value = ''
  previewLoading.value = true
  try {
    const res = await getArticleVersion(article.id, v.id)
    const data = res.data
    previewContent.value = data.content ? marked(data.content) : ''
  } catch { previewContent.value = '<p class="preview-error">加载失败，请重试</p>' }
  previewLoading.value = false
}

async function handleRollback(v) {
  try {
    await ElMessageBox.confirm(
      `确定回滚到 v${formatVersion(v.version)}「${v.title}」？当前内容将被直接覆盖。`,
      '确认回滚',
      { type: 'warning', confirmButtonText: '确定回滚' }
    )
  } catch { return }

  rollbackLoading.value = v.id
  try {
    await rollbackArticleVersion(article.id, v.id)
    ElMessage.success('已回滚，页面即将刷新')

    // Reload article
    const res = await getArticleById(article.id)
    Object.assign(article, res.data)

    versionDialogVisible.value = false
    expandedVersion.value = null
    previewContent.value = ''
    versions.value = []
  } catch {
    ElMessage.error('回滚失败')
  }
  rollbackLoading.value = null
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
</script>

<style scoped>
.article-detail {
  max-width: 780px;
  margin: 0 auto;
  padding: 32px 0 60px;
}

.detail-header { margin-bottom: 28px; }

.detail-title {
  font-size: 36px;
  font-weight: 750;
  color: var(--c-text);
  letter-spacing: -0.025em;
  line-height: 1.3;
  margin: 0 0 18px;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--c-text-muted);
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.meta-author {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--c-text-secondary);
  font-weight: 500;
}

.author-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--c-primary), var(--c-primary-dark));
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.meta-sep { color: var(--c-border); }

.meta-date, .meta-update {
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
  margin-bottom: 32px;
  padding: 14px 18px;
  background: var(--c-primary-light);
  border-radius: var(--radius-sm);
  border: 1px solid rgba(124, 58, 237, 0.1);
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
  padding: 44px 48px;
  border: 1px solid var(--c-border);
  box-shadow: var(--c-shadow);
}

.markdown-body {
  font-size: 16px;
  line-height: 2;
  color: var(--c-text);
}

.markdown-body :deep(h1) { font-size: 1.8em; font-weight: 700; margin: 1.2em 0 0.6em; letter-spacing: -0.02em; }
.markdown-body :deep(h2) { font-size: 1.45em; font-weight: 650; margin: 1.4em 0 0.5em; padding-bottom: 0.3em; border-bottom: 2px solid var(--c-border-light); }
.markdown-body :deep(h3) { font-size: 1.2em; font-weight: 600; margin: 1.1em 0 0.4em; }
.markdown-body :deep(p) { margin: 0.9em 0; }
.markdown-body :deep(pre) { background: #1e1e2e; color: #cdd6f4; border-radius: var(--radius-sm); padding: 20px 24px; overflow-x: auto; margin: 1.2em 0; }
.markdown-body :deep(code) { font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace; font-size: 0.88em; background: #f4f4f5; padding: 2px 6px; border-radius: 4px; }
.markdown-body :deep(pre code) { background: none; padding: 0; color: inherit; }
.markdown-body :deep(blockquote) { margin: 1.2em 0; padding: 8px 20px; color: var(--c-text-secondary); border-left: 3px solid var(--c-primary); background: var(--c-primary-light); border-radius: 0 var(--radius-sm) var(--radius-sm) 0; }
.markdown-body :deep(img) { max-width: 100%; border-radius: var(--radius-sm); }
.markdown-body :deep(ul), .markdown-body :deep(ol) { padding-left: 1.8em; margin: 0.6em 0; }
.markdown-body :deep(li) { margin: 0.4em 0; }
.markdown-body :deep(table) { border-collapse: collapse; width: 100%; margin: 1.2em 0; }
.markdown-body :deep(th), .markdown-body :deep(td) { border: 1px solid var(--c-border); padding: 10px 14px; text-align: left; }
.markdown-body :deep(th) { background: #fafafa; font-weight: 600; }
.markdown-body :deep(a) { color: var(--c-primary); text-decoration: underline; text-underline-offset: 2px; }
.markdown-body :deep(hr) { border: none; border-top: 1px solid var(--c-border); margin: 2em 0; }

.empty-state { padding: 120px 0; text-align: center; }

/* ========== 版本历史 ========== */

.version-dialog-body { min-height: 200px; }

.version-list { display: flex; flex-direction: column; gap: 8px; }

.version-item {
  border: 1px solid var(--c-border);
  border-radius: var(--radius-sm);
  overflow: hidden;
  transition: border-color 0.15s;
}

.version-item:hover { border-color: var(--c-primary); }

.version-item.expanded { border-color: var(--c-primary); }

.version-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.15s;
}

.version-header:hover { background: #faf9fc; }

.version-badge {
  flex-shrink: 0;
  padding: 2px 10px;
  border-radius: 6px;
  background: var(--c-primary);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  font-family: 'SF Mono', Consolas, monospace;
}

.version-title {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
  color: var(--c-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.version-date {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--c-text-muted);
}

.version-arrow {
  flex-shrink: 0;
  color: var(--c-text-muted);
  font-size: 14px;
}

.version-content {
  border-top: 1px solid var(--c-border);
  padding: 16px;
}

.version-preview {
  max-height: 360px;
  overflow-y: auto;
  padding: 16px;
  background: #fafafc;
  border-radius: var(--radius-sm);
  margin-bottom: 12px;
}

.version-preview .markdown-body { font-size: 14px; line-height: 1.8; }

.preview-empty { color: var(--c-text-muted); font-size: 13px; text-align: center; padding: 24px; }

.preview-error { color: #ef4444; text-align: center; }

.version-actions { display: flex; justify-content: flex-end; }

.version-empty { text-align: center; padding: 48px; color: var(--c-text-muted); font-size: 14px; }
</style>
