<template>
  <div class="home-page">
    <section class="dashboard-head">
      <div>
        <span class="head-kicker">{{ greeting }}</span>
        <h1>继续记录你的思考</h1>
        <p>写下一篇新文章，或从最近的内容继续整理。</p>
      </div>
      <div class="head-actions">
        <router-link to="/ai" class="action secondary">
          <el-icon><MagicStick /></el-icon>AI 工作台
        </router-link>
        <router-link to="/editor" class="action primary">
          <el-icon><EditPen /></el-icon>开始写作
        </router-link>
      </div>
    </section>

    <section class="overview-band">
      <div class="metric">
        <span>我的文章</span>
        <strong>{{ total }}</strong>
      </div>
      <div class="metric">
        <span>最近更新</span>
        <strong class="metric-date">{{ latestDate }}</strong>
      </div>
      <router-link to="/User/1/list" class="discover-link">
        <el-icon><Compass /></el-icon>
        <span><b>发现新内容</b><small>浏览社区中的文章</small></span>
        <el-icon><ArrowRight /></el-icon>
      </router-link>
    </section>

    <section class="recent-section">
      <div class="section-heading">
        <div>
          <span>YOUR WRITING</span>
          <h2>最近文章</h2>
        </div>
        <router-link to="/User/1/articles">查看全部 <el-icon><ArrowRight /></el-icon></router-link>
      </div>

      <div v-if="loading" class="loading-list">
        <el-skeleton :rows="3" animated />
      </div>
      <div v-else-if="recentArticles.length" class="article-list">
        <button v-for="article in recentArticles" :key="article.id" @click="openArticle(article.id)">
          <span class="article-icon"><el-icon><Document /></el-icon></span>
          <span class="article-copy">
            <b>{{ article.title }}</b>
            <small>{{ article.summary || '暂无摘要' }}</small>
          </span>
          <time>{{ formatDate(article.gmtModified || article.gmtCreate) }}</time>
          <el-icon class="row-arrow"><ArrowRight /></el-icon>
        </button>
      </div>
      <div v-else class="empty-writing">
        <el-icon><Document /></el-icon>
        <div><b>还没有文章</b><span>第一篇不必完美，只需要开始。</span></div>
        <router-link to="/editor">写第一篇</router-link>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Compass, Document, EditPen, MagicStick } from '@element-plus/icons-vue'
import { getMyArticles } from '@/api/article.js'

const router = useRouter()
const loading = ref(true)
const recentArticles = ref([])
const total = ref(0)

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 11) return '早上好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const latestDate = computed(() => {
  const article = recentArticles.value[0]
  return article ? formatDate(article.gmtModified || article.gmtCreate) : '--'
})

onMounted(async () => {
  try {
    const res = await getMyArticles({ page: 1, size: 4 })
    recentArticles.value = res.data?.articles || []
    total.value = res.data?.total || 0
  } catch {
    recentArticles.value = []
  } finally {
    loading.value = false
  }
})

function formatDate(value) {
  if (!value) return '--'
  return new Intl.DateTimeFormat('zh-CN', { month: 'short', day: 'numeric' }).format(new Date(value))
}

function openArticle(id) {
  router.push(`/article/${id}`)
}
</script>

<style scoped>
.home-page { max-width: 1040px; margin: 0 auto; }

.dashboard-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 32px;
  padding: 34px 0 30px;
  border-bottom: 1px solid var(--c-border);
}

.head-kicker { display: block; margin-bottom: 8px; color: var(--c-primary); font-size: 12px; font-weight: 700; }
.dashboard-head h1 { font-family: Georgia, "Microsoft YaHei", serif; font-size: 36px; line-height: 1.2; font-weight: 600; color: var(--c-text); }
.dashboard-head p { margin-top: 10px; color: var(--c-text-secondary); font-size: 15px; }
.head-actions { display: flex; gap: 8px; flex-shrink: 0; }

.action {
  height: 40px;
  padding: 0 15px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 13px;
  font-weight: 600;
}
.action.primary { background: var(--c-primary); color: #fff; }
.action.primary:hover { background: var(--c-primary-dark); }
.action.secondary { background: #fff; color: var(--c-text-secondary); border: 1px solid var(--c-border); }
.action.secondary:hover { color: var(--c-primary); border-color: #afd5ca; }

.overview-band {
  display: grid;
  grid-template-columns: 160px 190px minmax(240px, 1fr);
  align-items: stretch;
  border-bottom: 1px solid var(--c-border);
}
.metric { padding: 24px 22px 24px 0; display: flex; flex-direction: column; gap: 5px; }
.metric + .metric { padding-left: 24px; border-left: 1px solid var(--c-border); }
.metric span { color: var(--c-text-muted); font-size: 11px; font-weight: 600; }
.metric strong { color: var(--c-text); font-size: 28px; line-height: 1.2; }
.metric .metric-date { font-size: 20px; }

.discover-link {
  margin: 14px 0 14px 24px;
  padding: 13px 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--c-text-secondary);
  background: var(--c-accent-light);
  border-radius: 8px;
}
.discover-link > span { flex: 1; display: flex; flex-direction: column; }
.discover-link b { color: var(--c-text); font-size: 13px; }
.discover-link small { color: var(--c-text-muted); font-size: 11px; }
.discover-link:hover { color: var(--c-accent); }

.recent-section { padding: 38px 0; }
.section-heading { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 18px; }
.section-heading span { color: var(--c-text-muted); font-size: 10px; font-weight: 700; }
.section-heading h2 { margin-top: 3px; color: var(--c-text); font-size: 21px; }
.section-heading a { display: inline-flex; align-items: center; gap: 4px; color: var(--c-primary); font-size: 12px; font-weight: 600; }

.article-list { border-top: 1px solid var(--c-border); }
.article-list button {
  width: 100%;
  min-height: 76px;
  padding: 12px 8px;
  border: 0;
  border-bottom: 1px solid var(--c-border-light);
  background: transparent;
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr) 82px 20px;
  align-items: center;
  gap: 12px;
  text-align: left;
  cursor: pointer;
}
.article-list button:hover { background: #f8faf8; }
.article-icon { width: 34px; height: 34px; display: grid; place-items: center; border-radius: 6px; background: var(--c-primary-light); color: var(--c-primary); }
.article-copy { min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.article-copy b { overflow: hidden; color: var(--c-text); font-size: 14px; text-overflow: ellipsis; white-space: nowrap; }
.article-copy small { overflow: hidden; color: var(--c-text-muted); font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.article-list time { color: var(--c-text-muted); font-size: 11px; text-align: right; }
.row-arrow { color: var(--c-text-muted); }
.loading-list { padding: 18px 0; }

.empty-writing { min-height: 120px; padding: 20px; border: 1px dashed var(--c-border); display: flex; align-items: center; gap: 14px; color: var(--c-text-muted); }
.empty-writing > .el-icon { font-size: 24px; color: var(--c-primary); }
.empty-writing div { flex: 1; display: flex; flex-direction: column; }
.empty-writing b { color: var(--c-text); font-size: 14px; }
.empty-writing span { font-size: 12px; }
.empty-writing a { color: var(--c-primary); font-size: 13px; font-weight: 600; }

@media (max-width: 700px) {
  .dashboard-head { align-items: flex-start; flex-direction: column; padding-top: 18px; }
  .dashboard-head h1 { font-size: 29px; }
  .head-actions { width: 100%; }
  .action { flex: 1; justify-content: center; }
  .overview-band { grid-template-columns: 1fr 1fr; }
  .discover-link { grid-column: 1 / -1; margin: 0 0 18px; }
  .article-list button { grid-template-columns: 34px minmax(0, 1fr) 18px; }
  .article-list time { display: none; }
}
</style>
