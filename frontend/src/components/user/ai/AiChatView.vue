<template>
  <div class="chat-view">
    <div class="chat-topbar" v-if="title">
      <el-button class="topbar-toggle" text circle aria-label="切换侧栏" @click="$emit('toggle-sidebar')">
        <el-icon><Expand /></el-icon>
      </el-button>
      <div class="topbar-copy">
        <span class="topbar-label">当前对话</span>
        <span class="topbar-title">{{ title }}</span>
      </div>
      <div class="topbar-context" :class="{ active: selectedKbIds.length }">
        <el-icon><Collection /></el-icon>
        {{ selectedKbIds.length ? `${selectedKbIds.length} 个知识库` : '未启用知识库' }}
      </div>
    </div>

    <div class="chat-messages" ref="msgContainer">
      <div v-if="!messages.length && !streaming" class="chat-welcome">
        <div class="welcome-icon">
          <el-icon><MagicStick /></el-icon>
        </div>
        <h3>{{ agentGreeting }}</h3>
        <p>把正在思考的问题交给我，或者从下面选一个开始。</p>
        <div class="prompt-list">
          <button @click="applyPrompt('帮我梳理最近文章中的核心观点')">梳理文章观点</button>
          <button @click="applyPrompt('根据知识库内容，为我生成一份学习计划')">生成学习计划</button>
          <button @click="applyPrompt('检查我的内容结构，并给出改进建议')">改进内容结构</button>
        </div>
      </div>

      <template v-for="(rawMsg, i) in messages" :key="i">
        <div
          class="chat-msg"
          :class="{ user: rawMsg.role === 'user', assistant: rawMsg.role === 'assistant' }"
        >
          <div class="msg-avatar">
            <span v-if="rawMsg.role === 'user'" class="avatar-text">你</span>
            <span v-else class="avatar-text ai">AI</span>
          </div>
          <template v-for="msg in [enrichMessage(rawMsg)]" :key="i + '-e'">
            <div class="msg-bubble">
              <div v-if="msg.attachments?.length && isFileOnlyMessage(msg)" class="file-preview-cards">
                <div v-for="(att, j) in msg.attachments" :key="j" class="file-preview-card"
                  @click="openAttachment(att)" :title="att.type==='article'?'查看文章':'预览文件'">
                  <div class="file-preview-icon" :class="'ext-' + fileIcon(att.name)">
                    <el-icon v-if="att.type === 'article'" :size="28"><Document /></el-icon>
                    <el-icon v-else-if="fileIcon(att.name) === 'pdf'" :size="28"><Document /></el-icon>
                    <el-icon v-else-if="fileIcon(att.name) === 'docx'" :size="28"><Document /></el-icon>
                    <el-icon v-else-if="fileIcon(att.name) === 'txt'" :size="28"><Tickets /></el-icon>
                    <el-icon v-else-if="fileIcon(att.name) === 'md'" :size="28"><Notebook /></el-icon>
                    <el-icon v-else :size="28"><FolderOpened /></el-icon>
                  </div>
                  <div class="file-preview-info">
                    <span class="file-preview-name">{{ att.name }}</span>
                    <span class="file-preview-action">
                      {{ msg._parsedAttach ? '重新上传后可预览' : (att.type === 'article' ? '点击查看文章' : '点击预览文件') }}
                    </span>
                  </div>
                </div>
              </div>
              <div v-else class="markdown-body" v-html="renderMarkdown(msg.content)" />
              <div v-if="msg.attachments?.length && !isFileOnlyMessage(msg)" class="msg-attach-tags">
                <span v-for="(att, j) in msg.attachments" :key="j" class="msg-attach-tag clickable"
                  @click="openAttachment(att)" :title="att.type==='article'?'查看文章':'查看文件'">
                  <el-icon v-if="att.type === 'article'"><Document /></el-icon>
                  <el-icon v-else-if="fileIcon(att.name) === 'pdf'" color="#ef4444"><Document /></el-icon>
                  <el-icon v-else-if="fileIcon(att.name) === 'docx'" color="#3b82f6"><Document /></el-icon>
                  <el-icon v-else-if="fileIcon(att.name) === 'txt'" color="#22c55e"><Tickets /></el-icon>
                  <el-icon v-else-if="fileIcon(att.name) === 'md'" color="#d6674f"><Notebook /></el-icon>
                  <el-icon v-else><FolderOpened /></el-icon>
                  {{ att.name }}
                </span>
              </div>
            </div>
          </template>
        </div>
      </template>

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
      <div class="attach-chips" v-if="attachments.length || pendingFiles.length || pendingArticles.length">
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
        <div v-for="(art, i) in pendingArticles" :key="'pa-'+i" class="attach-chip pending">
          <el-icon><Document /></el-icon>
          <span class="chip-name">{{ art.name }}</span>
          <span class="pending-badge">待发送</span>
          <el-icon class="chip-remove" @click="removePendingArticle(i)"><CircleClose /></el-icon>
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
          placeholder="写下你的问题..."
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
      <!-- 上传进度条 -->
      <div v-if="uploading" class="upload-progress-bar">
        <div class="upload-progress-fill" :style="{ width: uploadProgress + '%' }"></div>
        <span class="upload-progress-text">{{ uploadProgress >= 100 ? '上传完成' : '上传中...' }}</span>
      </div>

      <p class="input-hint">内容由 AI 生成，请结合实际情况判断。</p>

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
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Expand, Promotion, CirclePlus, Document, FolderOpened, CircleClose, Search, Collection, MagicStick } from '@element-plus/icons-vue'
import { marked } from 'marked'
import { getMyArticles } from '@/api/article.js'
import { uploadRagFiles, submitRagArticles, getKnowledgeBaseDocuments } from '@/api/ai.js'
import { API_BASE } from '@/api/request.js'

const router = useRouter()

function fileIcon(name) {
  if (!name) return ''
  const ext = name.split('.').pop().toLowerCase()
  if (ext === 'pdf') return 'pdf'
  if (ext === 'docx' || ext === 'doc') return 'docx'
  if (ext === 'txt') return 'txt'
  if (ext === 'md') return 'md'
  return ext
}

const FILE_ONLY_RE = /^检索知识库(.+)文件$/

function parseAttachments(content) {
  if (!content) return null
  const m = content.match(FILE_ONLY_RE)
  if (!m) return null
  return m[1].split('、').filter(Boolean).map(name => {
    if (name.startsWith('【文章】')) {
      return { type: 'article', name: name.slice(4) }
    }
    return { type: 'file', name }
  })
}

function enrichMessage(msg) {
  if (msg.attachments?.length) return msg
  const parsed = parseAttachments(msg.content)
  if (!parsed) return msg
  const displayContent = msg.content.replace(FILE_ONLY_RE, '已发送 $1').trim()
  return { ...msg, content: displayContent, attachments: parsed, _parsedAttach: true }
}

function isFileOnlyMessage(msg) {
  if (!msg.attachments?.length) return false
  if (msg._parsedAttach) return true
  if (!msg.content || !msg.content.trim()) return true
  return FILE_ONLY_RE.test(msg.content.trim())
}

const props = defineProps({
  messages: { type: Array, default: () => [] },
  streaming: { type: Boolean, default: false },
  streamContent: { type: String, default: '' },
  title: { type: String, default: '' },
  agent: { type: [String, Number], default: 'general' },
  selectedKbIds: { type: Array, default: () => [] }
})

const emit = defineEmits(['toggle-sidebar', 'send', 'update:streaming', 'update:streamContent', 'kb-refresh'])

const inputText = ref('')
const inputRef = ref(null)
const msgContainer = ref(null)
const scrollAnchor = ref(null)
const fileInputRef = ref(null)

const attachments = ref([])
const pendingFiles = ref([])      // 待发送文件，发送时才上传
const pendingArticles = ref([])   // 待发送文章，发送时才上传
const showAttachMenu = ref(false)
const showArticleDialog = ref(false)
const articleSearch = ref('')
const articleLoading = ref(false)
const selectedArticleIds = ref([])
const articles = ref([])
const uploading = ref(false)
const uploadProgress = ref(0)
let progressTimer = null

const canSend = computed(() => {
  return (inputText.value.trim() || attachments.value.length || pendingFiles.value.length || pendingArticles.value.length) && !props.streaming && !uploading.value
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

function applyPrompt(prompt) {
  inputText.value = prompt
  nextTick(() => {
    autoResize()
    inputRef.value?.focus()
  })
}

function scrollBottom() {
  nextTick(() => {
    scrollAnchor.value?.scrollIntoView({ behavior: 'smooth' })
  })
}

async function handleEnter() {
  if (!canSend.value) return
  const text = inputText.value.trim()

  // 发送时先上传待发送文件和文章
  const hasPending = pendingFiles.value.length > 0 || pendingArticles.value.length > 0
  if (hasPending) {
    const initialAttachmentCount = attachments.value.length
    uploading.value = true
    uploadProgress.value = 0
    progressTimer = setInterval(() => {
      if (uploadProgress.value < 90) uploadProgress.value += 5
    }, 100)
    const kbId = props.selectedKbIds?.length ? props.selectedKbIds[0] : null
    const processingIds = []
    try {
      if (kbId) {
        // 上传文件
        if (pendingFiles.value.length) {
          const fetchRes = await uploadRagFiles(pendingFiles.value, kbId)
          if (!fetchRes.ok) throw new Error('Upload failed')
          const body = await fetchRes.json()
          if (body.code !== 200) throw new Error(body.msg || 'Upload failed')
          const docs = body.data || []
          for (const doc of docs) {
            attachments.value.push({ type: 'file', name: doc.title, r2Key: doc.r2Key, id: doc.id })
            if (doc.id && doc.status !== 'READY') processingIds.push(doc.id)
          }
        }
        // 上传文章
        if (pendingArticles.value.length) {
          const articleIds = pendingArticles.value.map(a => a.id)
          const artRes = await submitRagArticles(articleIds, kbId)
          if (artRes.code !== 200) throw new Error(artRes.msg || 'Upload failed')
          for (const doc of (artRes.data || [])) {
            if (doc.id && doc.status !== 'READY') processingIds.push(doc.id)
          }
        }
        if (processingIds.length) await waitForRagReady(kbId, processingIds)
        emit('kb-refresh')
      } else {
        for (const file of pendingFiles.value) {
          attachments.value.push({ type: 'file', name: file.name })
        }
      }
      // 将待发送文件/文章移入附件显示列表
      for (const art of pendingArticles.value) {
        if (!attachments.value.find(a => a.type === 'article' && a.id === art.id)) {
          attachments.value.push(art)
        }
      }
    } catch (error) {
      clearInterval(progressTimer)
      uploading.value = false
      uploadProgress.value = 0
      attachments.value.splice(initialAttachmentCount)
      emit('kb-refresh')
      ElMessage.error(error.message || '上传失败')
      return
    }
    clearInterval(progressTimer)
    uploadProgress.value = 100
    pendingFiles.value = []
    pendingArticles.value = []
    setTimeout(() => { uploading.value = false; uploadProgress.value = 0 }, 500)
  }

  // 允许只发文件不发文本，自动用"检索知识库xxx文件"格式填充以便检索
  if (!text && !attachments.value.length) return
  const finalText = text || ('检索知识库' + attachments.value.map(a => {
    return (a.type === 'article' ? '【文章】' : '') + a.name
  }).join('、') + '文件')

  emit('send', { text: finalText, attachments: [...attachments.value] })
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

async function waitForRagReady(kbId, documentIds) {
  const pending = new Set(documentIds)
  for (let attempt = 0; attempt < 60 && pending.size; attempt++) {
    const response = await getKnowledgeBaseDocuments(kbId)
    const documents = response.data || []
    for (const document of documents) {
      if (!pending.has(document.id)) continue
      if (document.status === 'FAILED') throw new Error(`文档处理失败：${document.title}`)
      if (document.status === 'READY') pending.delete(document.id)
    }
    if (pending.size) await new Promise(resolve => setTimeout(resolve, 1000))
  }
  if (pending.size) throw new Error('文档处理超时，请稍后重试')
}

function removeAttachment(i) {
  attachments.value.splice(i, 1)
}

async function openAttachment(att) {
  if (att.type === 'article' && att.id) {
    router.push(`/article/${att.id}`)
  } else if (att.r2Key) {
    try {
      const BASE = API_BASE
      const res = await fetch(`${BASE}/file/download/url?objectKey=${encodeURIComponent(att.r2Key)}`, {
        headers: { Authorization: `Bearer ${localStorage.getItem('token') || ''}` }
      })
      const body = await res.json()
      if (body.code === 200 && body.data?.downloadUrl) {
        window.open(body.data.downloadUrl, '_blank')
      } else {
        ElMessage.warning('无法获取文件链接')
      }
    } catch {
      ElMessage.error('获取文件失败')
    }
  } else {
    ElMessage.info('文件未上传到知识库，无法预览')
  }
}

function removePendingFile(i) {
  pendingFiles.value.splice(i, 1)
}

function removePendingArticle(i) {
  pendingArticles.value.splice(i, 1)
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

  const selected = articles.value.filter(a => selectedArticleIds.value.includes(a.id))
  for (const article of selected) {
    if (!pendingArticles.value.find(a => a.id === article.id) &&
        !attachments.value.find(a => a.type === 'article' && a.id === article.id)) {
      pendingArticles.value.push({ type: 'article', id: article.id, name: article.title })
    }
  }

  selectedArticleIds.value = []
  articleSearch.value = ''
  showArticleDialog.value = false
  ElMessage.success(`已选择 ${selected.length} 篇文章，发送消息时一并上传`)
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
  if (!props.selectedKbIds?.length) {
    ElMessage.warning('请先在左侧选择知识库')
    return
  }
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
  background: var(--c-surface-soft);
}

.chat-topbar {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 62px;
  padding: 9px 22px;
  border-bottom: 1px solid var(--c-border);
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
}

.topbar-toggle {
  color: var(--c-text-muted);
}

.topbar-copy { min-width: 0; display: flex; flex-direction: column; line-height: 1.25; }
.topbar-label { font-size: 10px; font-weight: 700; color: var(--c-text-muted); }

.topbar-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--c-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.topbar-context {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 9px;
  border: 1px solid var(--c-border);
  border-radius: 6px;
  color: var(--c-text-muted);
  font-size: 11px;
  white-space: nowrap;
}

.topbar-context.active { color: var(--c-primary); background: var(--c-primary-light); border-color: #c7e2d9; }

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 34px 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.chat-welcome {
  width: min(620px, 100%);
  text-align: left;
  padding: 44px 20px;
  margin: auto;
}

.welcome-icon {
  width: 42px;
  height: 42px;
  border-radius: 7px;
  background: var(--c-accent-light);
  color: var(--c-accent);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
  font-size: 21px;
}

.chat-welcome h3 {
  font-family: Georgia, "Microsoft YaHei", serif;
  font-size: 28px;
  font-weight: 600;
  color: var(--c-text);
  margin-bottom: 6px;
}

.chat-welcome p {
  font-size: 14px;
  color: var(--c-text-secondary);
  margin-bottom: 24px;
}

.prompt-list { display: grid; gap: 8px; }
.prompt-list button {
  width: 100%;
  padding: 11px 13px;
  border: 1px solid var(--c-border);
  border-radius: 6px;
  background: #fff;
  color: var(--c-text-secondary);
  text-align: left;
  font: inherit;
  font-size: 13px;
  cursor: pointer;
  transition: border-color var(--transition), color var(--transition), background var(--transition);
}
.prompt-list button:hover { border-color: #afd5ca; color: var(--c-primary); background: var(--c-primary-light); }

.chat-msg {
  display: flex;
  gap: 12px;
  width: min(100%, 900px);
  max-width: none;
  margin: 0 auto;
}

.chat-msg.user {
  justify-content: flex-start;
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
  background: var(--c-accent);
  color: #fff;
}

.msg-bubble {
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.7;
  min-width: 0;
  max-width: min(78%, 720px);
}

.chat-msg.assistant .msg-bubble {
  background: #fff;
  border: 1px solid var(--c-border-light);
  color: var(--c-text);
  border-bottom-left-radius: 6px;
}

.chat-msg.user .msg-bubble {
  background: var(--c-primary);
  color: #fff;
  border-bottom-right-radius: 6px;
}

.msg-bubble.streaming {
  background: #fff;
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
  background: rgba(22, 122, 105, 0.08);
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
  padding: 14px 22px 10px;
  border-top: 1px solid var(--c-border);
  background: rgba(255, 255, 255, 0.96);
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 8px 12px;
  border: 1px solid var(--c-border);
  max-width: 900px;
  min-height: 48px;
  margin: 0 auto;
  border-radius: 8px;
  background: #fff;
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
  max-width: 900px;
  margin: 0 auto;
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
  border: 1px solid rgba(22, 122, 105, 0.15);
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

/* ========== 文件预览卡片（仅文件消息） ========== */

.file-preview-cards {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.file-preview-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(22, 122, 105, 0.12);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.file-preview-card:hover {
  background: rgba(22, 122, 105, 0.06);
  border-color: rgba(22, 122, 105, 0.3);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(22, 122, 105, 0.1);
}

.file-preview-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.file-preview-icon.ext-pdf {
  background: #fef2f2;
  color: #ef4444;
}

.file-preview-icon.ext-docx,
.file-preview-icon.ext-doc {
  background: #eff6ff;
  color: #3b82f6;
}

.file-preview-icon.ext-txt {
  background: #f0fdf4;
  color: #22c55e;
}

.file-preview-icon.ext-md {
  background: #f5f3ff;
  color: var(--c-accent);
}

.file-preview-icon:not(.ext-pdf):not(.ext-docx):not(.ext-doc):not(.ext-txt):not(.ext-md) {
  background: #f5f4f8;
  color: var(--c-text-muted);
}

.file-preview-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.file-preview-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-preview-action {
  font-size: 12px;
  color: var(--c-primary);
  font-weight: 500;
}

.chat-msg.user .file-preview-card {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.2);
}

.chat-msg.user .file-preview-card:hover {
  background: rgba(255, 255, 255, 0.25);
  border-color: rgba(255, 255, 255, 0.4);
}

.chat-msg.user .file-preview-name { color: #fff; }
.chat-msg.user .file-preview-action { color: rgba(255, 255, 255, 0.85); }
.chat-msg.user .file-preview-icon { background: rgba(255, 255, 255, 0.2); color: #fff; }

/* ========== 消息内附件标签 ========== */

.msg-attach-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid rgba(22, 122, 105, 0.12);
}

.msg-attach-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 11px;
  background: rgba(22, 122, 105, 0.08);
  color: var(--c-primary);
}

.msg-attach-tag.clickable {
  cursor: pointer;
  transition: all 0.15s;
}

.msg-attach-tag.clickable:hover {
  background: rgba(22, 122, 105, 0.18);
  transform: translateY(-1px);
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
  border-color: rgba(22, 122, 105, 0.2);
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
  max-width: 900px;
  margin: 7px auto 0;
}

/* ========== 上传进度条 ========== */

.upload-progress-bar {
  position: relative;
  height: 22px;
  background: #e5e7eb;
  border-radius: 11px;
  margin: 8px 0 0;
  overflow: hidden;
  max-width: 900px;
  margin-left: auto;
  margin-right: auto;
}

.upload-progress-fill {
  height: 100%;
  background: var(--c-primary);
  border-radius: 11px;
  transition: width 0.3s ease;
}

@media (max-width: 640px) {
  .chat-topbar { min-height: 56px; padding: 7px 10px; }
  .topbar-label { display: none; }
  .topbar-context { padding: 4px 7px; }
  .chat-messages { padding: 22px 12px; gap: 16px; }
  .chat-welcome { padding: 24px 6px; }
  .chat-welcome h3 { font-size: 23px; }
  .chat-msg { gap: 7px; }
  .msg-avatar, .avatar-text { width: 28px; height: 28px; }
  .msg-bubble { max-width: calc(100% - 38px); padding: 10px 12px; }
  .chat-input-area { padding: 10px 10px 7px; }
  .input-hint { display: none; }
}

.upload-progress-text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: #374151;
  font-weight: 500;
}
</style>
