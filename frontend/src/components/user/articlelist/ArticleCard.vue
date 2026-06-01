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
            <el-icon :size="32"><Document /></el-icon>
          </div>
        </template>
      </el-image>
      <div v-else class="cover-fallback">
        <el-icon :size="32"><Document /></el-icon>
      </div>
    </div>

    <div class="card-body">
      <h3 class="card-title">{{ article.title || '无标题' }}</h3>
      <p class="card-excerpt">{{ excerpt }}</p>

      <div class="card-meta">
        <div class="card-tags" v-if="article.tag && article.tag.length">
          <el-tag v-for="tag in article.tag" :key="tag" size="small" type="info">{{ tag }}</el-tag>
        </div>
        <div class="card-info">
          <span class="card-date">{{ formatDate(article.gmtCreate) }}</span>
          <span class="card-author" v-if="article.writerName">{{ article.writerName }}</span>
        </div>
      </div>
    </div>

    <div v-if="showActions" class="card-actions" @click.stop>
      <el-button size="small" type="primary" text @click="$emit('edit')">
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
import { Document, Edit, Delete } from '@element-plus/icons-vue'

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
  transition: all 0.3s ease;
}

.article-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--c-shadow-lg);
}

.card-cover {
  height: 180px;
  background: linear-gradient(135deg, #eef2ff 0%, #f0f4ff 100%);
  overflow: hidden;
}

.cover-img {
  width: 100%;
  height: 100%;
  transition: transform 0.4s ease;
}

.article-card:hover .cover-img {
  transform: scale(1.05);
}

.cover-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--c-text-muted);
}

.card-body {
  padding: 16px 20px 0;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
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
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
  padding-bottom: 16px;
}

.card-tags { display: flex; gap: 4px; flex-wrap: wrap; }

.card-info {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--c-text-muted);
  margin-left: auto;
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
  padding: 8px 16px 12px;
  border-top: 1px solid var(--c-border);
  margin-top: 4px;
}
</style>
