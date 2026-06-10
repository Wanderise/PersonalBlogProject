<template>
  <div class="chat-view">
    <div class="chat-topbar" v-if="title">
      <el-icon class="topbar-toggle" @click="$emit('toggle-sidebar')"><Expand /></el-icon>
      <span class="topbar-title">{{ title }}</span>
    </div>

    <div class="chat-messages" ref="msgContainer">
      <div v-if="!messages.length && !streaming" class="chat-welcome">
        <div class="welcome-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 2a4 4 0 0 1 4 4v.5a4 4 0 0 1-2.5 3.7c-1.2.5-2.5.5-3.7 0A4 4 0 0 1 8 6.5V6a4 4 0 0 1 4-4z"/>
            <path d="M8 14a4 4 0 0 0-4 4v2h16v-2a4 4 0 0 0-4-4H8z"/>
            <circle cx="12" cy="12" r="3"/>
          </svg>
        </div>
        <h3>{{ agentGreeting }}</h3>
        <p>开始一段新的对话吧，可以附带文章或文件作为上下文</p>
      </div>

      <div
        v-for="(msg, i) in messages"
        :key="i"
        class="chat-msg"
        :class="{ user: msg.role === 'user', assistant: msg.role === 'assistant' }"
      >
        <div class="msg-avatar">
          <span v-if="msg.role === 'user'" class="avatar-text">你</span>
          <span v-else class="avatar-text ai">AI</span>
        </div>
        <div class="msg-bubble">
          <div class="markdown-body" v-html="renderMarkdown(msg.content)" />
          <div v-if="msg.attachments?.length" class="msg-attach-tags">
            <span v-for="(att, j) in msg.attachments" :key="j" class="msg-attach-tag">
              <el-icon><Document v-if="att.type === 'article'" /><FolderOpened v-else /></el-icon>
              {{ att.name }}
            </span>
          </div>
        </div>
      </div>

      <div v-if="streaming" class="chat-msg assistant">
        <div class="msg-avatar"><span class="avatar-text ai">AI</span></div>
        <div class="msg-bubble streaming">
          <span class="stream-cursor" v-if="!streamContent">思考中...</span>
          <div class="markdown-body" v-else v-html="renderMarkdown(streamContent)" />
          <span class="typing-cursor" v-if="streamContent">|</span>
        </div>
      </div>

      <div ref="scrollAnchor"></div>
    </div>

    <div class="chat-input-area">
      <div class="attach-chips" v-if="attachments.length || pendingFiles.length">
        <div v-for="(att, i) in attachments" :key="'att-'+i" class="attach-chip">
          <el-icon v-if="att.type === 'article'"><Document /></el-icon>
          <el-icon v-else><FolderOpened /></el-icon>
          <span class="chip-name">{{ att.name }}</span>
          <el-icon class="chip-remove" @click="removeAttachment(i)"><CircleClose /></el-icon>
        </div>
        <div v-for="(file, i) in pendingFiles" :key="'pf-'+i" class="attach-chip pending">
          <el-icon><FolderOpened /></el-icon>
          <span class="chip-name">{{ file.name }}</span>
          <span class="pending-badge">待发送</span>
          <el-icon class="chip-remove" @click="removePendingFile(i)"><CircleClose /></el-icon>
        </div>
      </div>
      <div class="input-wrapper">
        <el-popover
          v-model:visible="showAttachMenu"
          placement="top-start"
          :width="170"
          trigger="click"
        >
          <template #reference>
            <el-button class="attach-btn" text circle size="small" :disabled="streaming">
              <el-icon :size="18"><CirclePlus /></el-icon>
            </el-button>
          </template>
          <div class="attach-menu">
            <div class="attach-option" @click="openArticleSelector">
              <el-icon :size="16"><Document /></el-icon>
              <span>选择文章</span>
            </div>
            <div class="attach-option" @click="openFilePicker">
              <el-icon :size="16"><FolderOpened /></el-icon>
              <span>上传文件</span>
            </div>
          </div>
        </el-popover>
        <textarea
          ref="inputRef"
          v-model="inputText"
          class="chat-textarea"
          placeholder="输入消息，Enter 发送，Shift+Enter 换行"
          rows="1"
          @keydown.enter.exact.prevent="handleEnter"
          @input="autoResize"
          :disabled="streaming"
        ></textarea>
        <el-button
          class="send-btn"
          :type="canSend ? 'primary' : 'default'"
          circle
          size="small"
          :disabled="!canSend || streaming"
          :loading="uploading"
          @click="send"
        >
          <el-icon v-if="!uploading"><Promotion /></el-icon>
        </el-button>
      </div>
      <p class="input-hint">AI 回答可能存在错误，请注意甄别。可附带文章/文件作为上下文</p>

      <input
        ref="fileInputRef"
        type="file"
        accept=".pdf,.doc,.docx,.txt"
        multiple
        style="display:none"
        @change="handleFileChange"
      />
    </div>

    <!-- 文章选择弹窗 -->
    <el-dialog v-model="showArticleDialog" title="选择文章作为上下文" width="560px" :close-on-click-modal="false">
      <el-input
        v-model="articleSearch"
        placeholder="搜索文章标题..."
        clearable
        class="article-search"
        :prefix-icon="Search"
      />
      <div class="article-list" v-loading="articleLoading">
        <el-checkbox-group v-model="selectedArticleIds">
          <div
            v-for="article in filteredArticles"
            :key="article.id"
            class="article-item"
            :class="{ checked: selectedArticleIds.includes(article.id) }"
          >
            <el-checkbox :value="article.id">
              <span class="article-title">{{ article.title }}</span>
              <span class="article-summary">{{ article.summary?.slice(0, 60) || '暂无摘要' }}</span>
            </el-checkbox>
          </div>
        </el-checkbox-group>
        <div v-if="!filteredArticles.length" class="article-empty">
          {{ articles.length ? '无匹配文章' : '暂无文章，请先发布' }}
        </div>
      </div>
      <template #footer>
        <el-button @click="showArticleDialog = false">取消</el-button>
        <el-button type="primary" :disabled="!selectedArticleIds.length" @click="confirmArticles">
          确认添加 ({{ selectedArticleIds.length }})
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Expand, Promotion, CirclePlus, Document, FolderOpened, CircleClose, Search } from '@element-plus/icons-vue'
import { marked } from 'marked'
import { getMyArticles } from '@/api/article.js'
import { uploadRagFiles, submitRagArticles } from '@/api/ai.js'

const props = defineProps({
  messages: { type: Array, default: () => [] },
  streaming: { type: Boolean, default: false },
  streamContent: { type: String, default: '' },
  title: { type: String, default: '' },
  agent: { type: String, default: 'general' },
  selectedKbIds: { type: Array, default: () => [] }
})

const emit = defineEmits(['toggle-sidebar', 'send', 'update:streaming', 'update:streamContent'])

const inputText = ref('')
const inputRef = ref(null)
const msgContainer = ref(null)
const scrollAnchor = ref(null)
const fileInputRef = ref(null)

const attachments = ref([])
const pendingFiles = ref([]) // 待发送文件，发送时才上传
const showAttachMenu = ref(false)
const showArticleDialog = ref(false)
const articleSearch = ref('')
const articleLoading = ref(false)
const selectedArticleIds = ref([])
const articles = ref([])
const uploading = ref(false)

const canSend = computed(() => {
  return (inputText.value.trim() || attachments.value.length || pendingFiles.value.length) && !props.streaming && !uploading.value
})

const agentGreeting = computed(() => {
  const map = { general: '你好！我是通用助手', coder: '你好！我是代码专家', writer: '你好！我是写作助手' }
  return map[props.agent] || '你好！有什么可以帮你的？'
})

const filteredArticles = computed(() => {
  if (!articleSearch.value) return articles.value
  const kw = articleSearch.value.toLowerCase()
  return articles.value.filter(a => a.title.toLowerCase().includes(kw))
})

function renderMarkdown(text) {
  if (!text) return ''
  return marked(text, { breaks: true })
}

function autoResize() {
  const el = inputRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 160) + 'px'
}

function scrollBottom() {
  nextTick(() => {
    scrollAnchor.value?.scrollIntoView({ behavior: 'smooth' })
  })
}

async function handleEnter() {
  if (!canSend.value) return
  const text = inputText.value.trim()

  // 发送时先上传待发送文件（有KB则索引，无KB仅上传到R2显示在附件中）
  if (pendingFiles.value.length) {
    uploading.value = true
    const kbId = props.selectedKbIds?.length ? props.selectedKbIds[0] : null
    try {
      if (kbId) {
        const fetchRes = await uploadRagFiles(pendingFiles.value, kbId)
        if (!fetchRes.ok) throw new Error('Upload failed')
        const body = await fetchRes.json()
        if (body.code !== 200) throw new Error(body.msg || 'Upload failed')
      }
      for (const file of pendingFiles.value) {
        attachments.value.push({ type: 'file', name: file.name })
      }
    } catch {
      ElMessage.error('文件上传失败')
    }
    pendingFiles.value = []
    uploading.value = false
  }

  // 允许只发文件不发文本
  if (!text && !attachments.value.length) return

  emit('send', { text, attachments: [...attachments.value] })
  inputText.value = ''
  attachments.value = []
  pendingFiles.value = []
  nextTick(() => {
    if (inputRef.value) { inputRef.value.style.height = 'auto' }
  })
}

function send() {
  handleEnter()
}

function removeAttachment(i) {
  attachments.value.splice(i, 1)
}

function removePendingFile(i) {
  pendingFiles.value.splice(i, 1)
}

async function openArticleSelector() {
  showAttachMenu.value = false
  if (!getUploadKbId()) return
  articleLoading.value = true
  showArticleDialog.value = true
  try {
    const res = await getMyArticles({ page: 1, size: 200 })
    articles.value = res.data?.articles || []
  } catch { /* ignore */ }
  articleLoading.value = false
}

async function confirmArticles() {
  const kbId = getUploadKbId()
  if (!kbId || !selectedArticleIds.value.length) return

  try {
    const res = await submitRagArticles(selectedArticleIds.value, kbId)
    const data = res.data
    if (res.code !== 200) throw new Error(res.msg)
    const count = Array.isArray(data) ? data.length : selectedArticleIds.value.length
    ElMessage.success(`${count} 篇文章已加入知识库`)

    const selected = articles.value.filter(a => selectedArticleIds.value.includes(a.id))
    for (const article of selected) {
      if (!attachments.value.find(a => a.type === 'article' && a.id === article.id)) {
        attachments.value.push({ type: 'article', id: article.id, name: article.title })
      }
    }
  } catch {
    ElMessage.error('提交失败，请重试')
  }

  selectedArticleIds.value = []
  articleSearch.value = ''
  showArticleDialog.value = false
}

function getUploadKbId() {
  if (!props.selectedKbIds?.length) {
    ElMessage.warning('请先在左侧选择知识库')
    return null
  }
  return props.selectedKbIds[0]
}

function openFilePicker() {
  showAttachMenu.value = false
  fileInputRef.value?.click()
}

function handleFileChange(e) {
  const files = [...e.target.files]
  if (!files.length) return

  const allowed = [
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'text/plain'
  ]
  const invalid = files.filter(f => !allowed.includes(f.type))
  if (invalid.length) {
    ElMessage.warning(`不支持的文件类型：${invalid.map(f => f.name).join('、')}`)
    e.target.value = ''
    return
  }

  // 文件先暂存，发送消息时才上传
  for (const file of files) {
    pendingFiles.value.push(file)
  }
  e.target.value = ''
}

watch(() => props.messages.length, scrollBottom)
watch(() => props.streamContent, scrollBottom)

onMounted(() => {
  if (inputRef.value) inputRef.value.focus()
})

defineExpose({ focus: () => inputRef.value?.focus() })
</script>

<style scoped>
.chat-view {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--c-surface);
}

.chat-topbar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 20px;
  border-bottom: 1px solid var(--c-border);
  background: var(--c-surface);
}

.topbar-toggle {
  cursor: pointer;
  color: var(--c-text-muted);
  font-size: 18px;
}

.topbar-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--c-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.chat-welcome {
  text-align: center;
  padding: 60px 20px;
  margin: auto;
}

.welcome-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  background: var(--c-primary-light);
  color: var(--c-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}

.welcome-icon svg { width: 32px; height: 32px; }

.chat-welcome h3 {
  font-size: 20px;
  font-weight: 700;
  color: var(--c-text);
  margin-bottom: 6px;
}

.chat-welcome p {
  font-size: 14px;
  color: var(--c-text-muted);
}

.chat-msg {
  display: flex;
  gap: 12px;
  max-width: 85%;
}

.chat-msg.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.msg-avatar {
  flex-shrink: 0;
  width: 34px;
  height: 34px;
}

.avatar-text {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  background: var(--c-primary-light);
  color: var(--c-primary);
}

.avatar-text.ai {
  background: linear-gradient(135deg, #7c3aed, #a78bfa);
  color: #fff;
}

.msg-bubble {
  padding: 12px 16px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.7;
  min-width: 0;
}

.chat-msg.assistant .msg-bubble {
  background: #f5f4f8;
  color: var(--c-text);
  border-bottom-left-radius: 6px;
}

.chat-msg.user .msg-bubble {
  background: var(--c-primary);
  color: #fff;
  border-bottom-right-radius: 6px;
}

.msg-bubble.streaming {
  background: #f5f4f8;
  border-bottom-left-radius: 6px;
}

.stream-cursor {
  color: var(--c-text-muted);
  font-style: italic;
}

.typing-cursor {
  display: inline;
  animation: blink 1s step-end infinite;
  color: var(--c-primary);
  font-weight: 700;
}

@keyframes blink {
  50% { opacity: 0; }
}

.chat-msg.user .msg-bubble :deep(p) { margin: 0; color: #fff; }
.chat-msg.user .msg-bubble :deep(code) { background: rgba(255,255,255,0.2); padding: 2px 6px; border-radius: 4px; color: #fff; }

.msg-bubble :deep(pre) {
  background: #1e1e2e;
  color: #cdd6f4;
  border-radius: 8px;
  padding: 14px 16px;
  overflow-x: auto;
  margin: 8px 0;
}

.msg-bubble :deep(pre code) { background: none; padding: 0; font-size: 13px; }

.msg-bubble :deep(code) {
  background: rgba(124, 58, 237, 0.08);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.9em;
  font-family: 'SF Mono', Consolas, monospace;
}

.msg-bubble :deep(p) { margin: 0.4em 0; }
.msg-bubble :deep(p:first-child) { margin-top: 0; }
.msg-bubble :deep(p:last-child) { margin-bottom: 0; }
.msg-bubble :deep(ul), .msg-bubble :deep(ol) { padding-left: 1.5em; margin: 0.4em 0; }
.msg-bubble :deep(blockquote) {
  margin: 0.5em 0;
  padding: 0 1em;
  border-left: 3px solid var(--c-primary);
  color: var(--c-text-secondary);
}

.chat-input-area {
  padding: 16px 20px 12px;
  border-top: 1px solid var(--c-border);
  background: var(--c-surface);
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 8px 12px;
  border: 1px solid var(--c-border);
  border-radius: 12px;
  background: #fafafc;
  transition: border-color 0.2s;
}

.input-wrapper:focus-within {
  border-color: var(--c-primary);
  box-shadow: 0 0 0 3px var(--c-primary-glow);
}

.chat-textarea {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 14px;
  font-family: var(--font);
  line-height: 1.5;
  resize: none;
  max-height: 160px;
  padding: 4px 0;
  color: var(--c-text);
}

.chat-textarea::placeholder { color: var(--c-text-muted); }

.send-btn { flex-shrink: 0; }

/* ========== 附件 ========== */

.attach-btn {
  flex-shrink: 0;
  color: var(--c-text-muted);
  transition: color 0.15s;
}

.attach-btn:hover { color: var(--c-primary); }

.attach-menu {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.attach-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
  font-size: 14px;
  color: var(--c-text);
}

.attach-option:hover { background: var(--c-primary-light); color: var(--c-primary); }

.attach-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 0 0 10px;
}

.attach-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 8px 4px 10px;
  border-radius: 8px;
  background: var(--c-primary-light);
  color: var(--c-primary);
  font-size: 12px;
  font-weight: 500;
  border: 1px solid rgba(124, 58, 237, 0.15);
}

.attach-chip .chip-name {
  max-width: 140px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chip-remove {
  cursor: pointer;
  font-size: 14px;
  opacity: 0.7;
  transition: opacity 0.15s;
}

.chip-remove:hover { opacity: 1; color: #ef4444; }

.attach-chip.pending {
  background: #fff3cd;
  color: #856404;
  border-color: rgba(133, 100, 4, 0.2);
}

.pending-badge {
  font-size: 10px;
  background: #856404;
  color: #fff;
  padding: 1px 5px;
  border-radius: 3px;
}

/* ========== 消息内附件标签 ========== */

.msg-attach-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid rgba(124, 58, 237, 0.12);
}

.msg-attach-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 11px;
  background: rgba(124, 58, 237, 0.08);
  color: var(--c-primary);
}

/* ========== 文章选择弹窗 ========== */

.article-search { margin-bottom: 14px; }

.article-list {
  max-height: 360px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.article-item {
  padding: 10px 14px;
  border-radius: 8px;
  border: 1px solid transparent;
  transition: all 0.15s;
  cursor: pointer;
}

.article-item:hover { background: #f5f4f8; }

.article-item.checked {
  background: var(--c-primary-light);
  border-color: rgba(124, 58, 237, 0.2);
}

.article-item .article-title {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text);
}

.article-item .article-summary {
  display: block;
  font-size: 12px;
  color: var(--c-text-muted);
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.article-empty {
  text-align: center;
  padding: 32px;
  color: var(--c-text-muted);
  font-size: 14px;
}

.input-hint {
  text-align: center;
  font-size: 11px;
  color: var(--c-text-muted);
  margin: 8px 0 0;
}
</style>
