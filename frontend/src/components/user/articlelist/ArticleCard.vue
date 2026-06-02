<template>
  <el-card class="article-card" shadow="hover" @click="$emit('click')">
    <div class="card-cover">
      <el-image
        v-if="coverUrl"
        :src="coverUrl"
        fit="cover"
        class="cover-img"
        lazy
      >
        <template #error>
          <div class="cover-fallback">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="36" height="36">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/>
            </svg>
          </div>
        </template>
      </el-image>
      <div v-else class="cover-fallback">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="36" height="36">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
          <polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/>
        </svg>
      </div>
      <div class="cover-gradient"></div>
    </div>

    <div class="card-body">
      <h3 class="card-title">{{ article.title || '无标题' }}</h3>
      <p class="card-excerpt">{{ excerpt }}</p>
      <div class="card-meta">
        <span class="card-author" v-if="article.writerName">{{ article.writerName }}</span>
        <span class="card-date">{{ formatDate(article.gmtCreate) }}</span>
        <div class="card-tags" v-if="article.tag && article.tag.length">
          <span class="mini-tag" v-for="tag in article.tag.slice(0, 3)" :key="tag">{{ tag }}</span>
        </div>
      </div>
    </div>

    <div v-if="showActions" class="card-actions" @click.stop>
      <el-button size="small" text @click="$emit('edit')">
        <el-icon><Edit /></el-icon> 编辑
      </el-button>
      <el-popconfirm title="确定删除这篇文章？" @confirm="$emit('delete')">
        <template #reference>
          <el-button size="small" type="danger" text>
            <el-icon><Delete /></el-icon> 删除
          </el-button>
        </template>
      </el-popconfirm>
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import { Edit, Delete } from '@element-plus/icons-vue'

const props = defineProps({
  article: { type: Object, required: true },
  coverUrl: { type: String, default: '' },
  showActions: { type: Boolean, default: false }
})

defineEmits(['click', 'edit', 'delete'])

const excerpt = computed(() => {
  const text = props.article.content || props.article.summary || ''
  return text.replace(/[#*`>\[\]!()\n]/g, ' ').replace(/\s+/g, ' ').trim().substring(0, 120) + (text.length > 120 ? '...' : '')
})

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
</script>

<style scoped>
.article-card {
  border-radius: var(--radius);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid var(--c-border);
  padding: 0;
}

.article-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--c-shadow-lg);
  border-color: transparent;
}

.card-cover {
  height: 180px;
  position: relative;
  background: linear-gradient(135deg, #f5f3ff 0%, #ede9fe 100%);
  overflow: hidden;
}

.cover-img {
  width: 100%;
  height: 100%;
  transition: transform 0.5s ease;
}

.article-card:hover .cover-img {
  transform: scale(1.06);
}

.cover-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c4b5fd;
}

.cover-gradient {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 48px;
  background: linear-gradient(transparent, rgba(0,0,0,0.02));
  pointer-events: none;
}

.card-body {
  padding: 18px 20px 0;
}

.card-title {
  font-size: 16px;
  font-weight: 650;
  color: var(--c-text);
  margin: 0 0 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-excerpt {
  font-size: 13px;
  color: var(--c-text-secondary);
  line-height: 1.6;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 14px;
  padding-bottom: 16px;
  font-size: 12px;
  color: var(--c-text-muted);
}

.card-author { font-weight: 500; }

.card-date { margin-left: auto; }

.card-tags {
  display: flex;
  gap: 4px;
}

.mini-tag {
  padding: 1px 8px;
  border-radius: 4px;
  font-size: 11px;
  background: var(--c-primary-light);
  color: var(--c-primary);
  font-weight: 500;
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
  padding: 8px 14px 12px;
  border-top: 1px solid var(--c-border);
}
</style>
