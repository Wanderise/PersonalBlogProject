<template>
  <aside class="ai-sidebar" :class="{ collapsed: !open }">
    <div class="sidebar-inner">
      <div class="sidebar-head">
        <div class="head-brand">
          <div class="brand-dot"></div>
          <span>AI Studio</span>
        </div>
        <el-button class="close-btn" text circle size="small" @click="$emit('toggle')">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
      </div>

      <el-button class="new-chat-btn" size="large" @click="$emit('new')">
        <el-icon><ChatDotRound /></el-icon>
        开始对话
      </el-button>

      <div class="sidebar-section">
        <div class="section-head">
          <span class="section-title">助手 <b>{{ agents.length }}</b></span>
          <el-button class="add-agent-btn" text size="small" @click="showAddAgent = true">
            <el-icon><Plus /></el-icon>
          </el-button>
        </div>
        <div class="agent-list">
          <div
            v-for="agent in agents"
            :key="agent.id"
            class="agent-card"
            :class="{ active: selectedAgent === agent.id }"
            @click="$emit('agentChange', agent.id)"
          >
            <span class="agent-icon">{{ agent.icon || '🤖' }}</span>
            <div class="agent-info">
              <span class="agent-name">{{ agent.name }}</span>
              <span class="agent-desc">{{ agent.systemPrompt?.slice(0, 20) || '自定义助手' }}</span>
            </div>
            <el-icon class="agent-del" @click.stop="removeAgent(agent.id)"><Delete /></el-icon>
          </div>
        </div>
      </div>

      <div class="sidebar-section">
        <div class="section-head">
          <span class="section-title">知识库 <b>{{ kbs.length }}</b></span>
          <el-button class="add-agent-btn" text size="small" @click="showAddKb = true">
            <el-icon><Plus /></el-icon>
          </el-button>
        </div>
        <div class="kb-list" v-if="kbs.length">
          <div
            v-for="kb in kbs"
            :key="kb.id"
            class="kb-card"
          >
            <el-checkbox
              :model-value="selectedKbIds.includes(kb.id)"
              @change="toggleKb(kb.id)"
            />
            <div class="kb-info" @click="toggleKb(kb.id)">
              <span class="kb-name">{{ kb.name }}</span>
              <span class="kb-meta">{{ kb.docCount || 0 }} 篇文档</span>
            </div>
            <el-icon class="kb-manage" @click.stop="openKbManager(kb)"><Setting /></el-icon>
            <el-icon class="kb-del" @click.stop="removeKb(kb.id)"><Delete /></el-icon>
          </div>
        </div>
        <div v-else class="conv-empty">暂无知识库</div>
      </div>

      <!-- 知识库文档管理弹窗 -->
      <el-dialog v-model="showKbManager" width="880px" :close-on-click-modal="false" class="kb-manager-dialog">
        <template #header>
          <div class="kb-mgr-header">
            <el-icon :size="18"><FolderOpened /></el-icon>
            <span>{{ managingKb?.name || '' }}</span>
            <span class="kb-mgr-count">{{ documents.length }} 篇文档</span>
          </div>
        </template>
        <div class="kb-mgr-body" v-loading="docLoading">
          <!-- 左侧文档列表 -->
          <div class="kb-mgr-list">
            <div v-if="documents.length === 0 && !docLoading" class="doc-empty">暂无文档</div>
            <div
              v-for="doc in documents" :key="doc.id"
              class="doc-item" :class="{ active: activeDoc?.id === doc.id }"
              @click="selectDoc(doc)"
            >
              <div class="doc-icon">
                <el-icon v-if="doc.fileType === 'pdf'" color="#ef4444"><Document /></el-icon>
                <el-icon v-else-if="doc.fileType === 'docx' || doc.fileType === 'doc'" color="#3b82f6"><Document /></el-icon>
                <el-icon v-else-if="doc.fileType === 'txt'" color="#22c55e"><Tickets /></el-icon>
                <el-icon v-else-if="doc.fileType === 'md'" color="#d6674f"><Notebook /></el-icon>
                <el-icon v-else color="#6b7280"><FolderOpened /></el-icon>
              </div>
              <div class="doc-body">
                <span class="doc-name">{{ doc.title }}</span>
                <span class="doc-meta">{{ doc.fileType }} · {{ formatTime(doc.gmtCreate) }}</span>
              </div>
              <span class="doc-status" :class="(doc.status || '').toLowerCase()">{{ statusLabel(doc.status) }}</span>
              <div class="doc-actions">
                <el-button v-if="doc.status === 'FAILED'" text circle size="small" title="重试" @click.stop="retryDoc(doc.id)">
                  <el-icon><RefreshRight /></el-icon>
                </el-button>
                <el-button text circle size="small" title="删除" @click.stop="removeDoc(doc.id)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>
          </div>
          <!-- 右侧内容预览 -->
          <div class="kb-mgr-preview">
            <div v-if="!activeDoc" class="preview-placeholder">
              <el-icon :size="40" color="#c4b5fd"><Reading /></el-icon>
              <p>点击左侧文档查看内容</p>
            </div>
            <div v-else-if="previewLoading" class="preview-placeholder">
              <el-icon :size="24" class="is-loading"><Loading /></el-icon>
              <p>加载中...</p>
            </div>
            <div v-else class="preview-content">
              <div class="preview-head">
                <span class="preview-title">{{ activeDoc.title }}</span>
                <el-button size="small" text @click="openOrigin(activeDoc)">
                  <el-icon><Link /></el-icon> 打开原文件
                </el-button>
              </div>
              <div class="preview-body">
                <div v-if="previewError" class="preview-error">{{ previewError }}</div>
                <pre v-else-if="previewContent" class="preview-text">{{ previewContent }}</pre>
                <div v-else class="preview-placeholder-inner">(空文档)</div>
              </div>
            </div>
          </div>
        </div>
      </el-dialog>

      <div class="sidebar-section conversations">
        <div class="section-head conversation-head">
          <span class="section-title">最近对话 <b>{{ conversations.length }}</b></span>
        </div>
        <div class="conv-list" v-if="conversations.length">
          <div
            v-for="conv in conversations"
            :key="conv.id"
            class="conv-item"
            :class="{ active: conv.id === activeId }"
            @click="$emit('select', conv.id)"
          >
            <el-icon class="conv-icon"><ChatLineSquare /></el-icon>
            <div class="conv-body">
              <span class="conv-title" v-if="editingId !== conv.id">{{ conv.title }}</span>
              <input
                v-else
                ref="editInput"
                class="conv-input"
                :value="conv.title"
                @blur="finishRename(conv)"
                @keyup.enter="finishRename(conv)"
              />
              <span class="conv-time">{{ formatTime(conv.gmtCreate) }}</span>
            </div>
            <div class="conv-actions">
              <el-icon class="action-icon" @click.stop="startRename(conv)"><EditPen /></el-icon>
              <el-icon class="action-icon" @click.stop="removeConv(conv.id)"><Delete /></el-icon>
            </div>
          </div>
        </div>
        <div v-else class="conv-empty">暂无对话记录</div>
      </div>
    </div>

    <!-- 创建 Agent 弹窗 -->
    <el-dialog v-model="showAddAgent" title="创建 Agent" width="420px" :close-on-click-modal="false">
      <el-form :model="agentForm" label-position="top">
        <el-form-item label="名称" required>
          <el-input v-model="agentForm.name" placeholder="例如：代码助手" maxlength="20" />
        </el-form-item>
        <el-form-item label="系统指令" required>
          <el-input
            v-model="agentForm.systemPrompt"
            type="textarea"
            :rows="4"
            placeholder="例如：你是一名资深 Java 开发工程师..."
          />
        </el-form-item>
        <el-form-item label="图标（emoji）">
          <el-input v-model="agentForm.icon" placeholder="🤖" maxlength="4" class="icon-input" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddAgent = false">取消</el-button>
        <el-button type="primary" :disabled="!agentForm.name.trim() || !agentForm.systemPrompt.trim()" @click="saveAgent">创建</el-button>
      </template>
    </el-dialog>

    <!-- 创建知识库弹窗 -->
    <el-dialog v-model="showAddKb" title="创建知识库" width="400px" :close-on-click-modal="false">
      <el-form :model="kbForm" label-position="top">
        <el-form-item label="名称" required>
          <el-input v-model="kbForm.name" placeholder="例如：Java 学习笔记" maxlength="50" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="kbForm.description"
            type="textarea"
            :rows="3"
            placeholder="可选描述..."
            maxlength="200"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddKb = false">取消</el-button>
        <el-button type="primary" :disabled="!kbForm.name.trim()" @click="saveKb">创建</el-button>
      </template>
    </el-dialog>
  </aside>
</template>

<script setup>
import { ref, reactive, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, ChatDotRound, ChatLineSquare, EditPen, Delete, Plus, Setting, Document, Tickets, Notebook, FolderOpened, Reading, Loading, Link, RefreshRight } from '@element-plus/icons-vue'
import { createAgent, deleteAgent, getKnowledgeBases, createKnowledgeBase, deleteKnowledgeBase, getKnowledgeBaseDocuments, deleteKnowledgeBaseDocument, retryKnowledgeBaseDocument } from '@/api/ai.js'
import { API_BASE } from '@/api/request.js'

const props = defineProps({
  open: { type: Boolean, default: true },
  activeId: { type: [String, Number], default: null },
  conversations: { type: Array, default: () => [] },
  agents: { type: Array, default: () => [] },
  selectedAgent: { type: [String, Number], default: null },
  knowledgeBases: { type: Array, default: () => [] },
  selectedKbIds: { type: Array, default: () => [] }
})

const emit = defineEmits(['toggle', 'select', 'new', 'rename', 'delete', 'agentChange', 'refreshAgents', 'kbsChange', 'refreshKbs'])

const editingId = ref(null)
const showAddAgent = ref(false)
const agentForm = reactive({ name: '', systemPrompt: '', icon: '🤖' })

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function statusLabel(status) {
  return { PROCESSING: '处理中', READY: '可用', FAILED: '失败' }[status] || status || '未知'
}

function startRename(conv) {
  editingId.value = conv.id
  nextTick(() => {
    const input = document.querySelector('.conv-input')
    if (input) { input.focus(); input.select() }
  })
}

function finishRename(conv) {
  const input = document.querySelector('.conv-input')
  if (input && input.value.trim()) {
    emit('rename', { id: conv.id, title: input.value.trim() })
  }
  editingId.value = null
}

function removeConv(id) {
  ElMessageBox.confirm('确定删除此对话？', '提示', { type: 'warning' }).then(() => {
    emit('delete', id)
  }).catch(() => {})
}

async function saveAgent() {
  try {
    await createAgent({
      name: agentForm.name.trim(),
      systemPrompt: agentForm.systemPrompt.trim(),
      icon: agentForm.icon.trim() || '🤖'
    })
    ElMessage.success('Agent 已创建')
    showAddAgent.value = false
    Object.assign(agentForm, { name: '', systemPrompt: '', icon: '🤖' })
    emit('refreshAgents')
  } catch {
    ElMessage.error('创建失败')
  }
}

async function removeAgent(id) {
  try {
    await deleteAgent(id)
    ElMessage.success('已删除')
    emit('refreshAgents')
  } catch {
    ElMessage.error('删除失败')
  }
}

/* ========== 知识库 ========== */
const kbs = ref([])
const showAddKb = ref(false)
const kbForm = reactive({ name: '', description: '' })

function toggleKb(id) {
  const ids = [...props.selectedKbIds]
  const idx = ids.indexOf(id)
  if (idx >= 0) ids.splice(idx, 1)
  else ids.push(id)
  emit('kbsChange', ids)
}

async function loadKbs() {
  try {
    const res = await getKnowledgeBases()
    kbs.value = res.data || []
  } catch { /* ignore */ }
}

async function saveKb() {
  try {
    await createKnowledgeBase({ name: kbForm.name.trim(), description: kbForm.description.trim() })
    ElMessage.success('知识库已创建')
    showAddKb.value = false
    Object.assign(kbForm, { name: '', description: '' })
    emit('refreshKbs')
  } catch {
    ElMessage.error('创建失败')
  }
}

async function removeKb(id) {
  try {
    await ElMessageBox.confirm('删除知识库将同时删除其中的所有文档和向量数据', '警告', { type: 'warning' })
    await deleteKnowledgeBase(id)
    kbs.value = kbs.value.filter(kb => kb.id !== id)
    emit('kbsChange', props.selectedKbIds.filter(kbId => kbId !== id))
    if (managingKb.value?.id === id) {
      showKbManager.value = false
      managingKb.value = null
      documents.value = []
      activeDoc.value = null
      previewContent.value = ''
      previewError.value = ''
      previewUrl.value = ''
    }
    ElMessage.success('已删除')
    emit('refreshKbs')
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error('删除失败')
  }
}

/* ========== 知识库文档管理 ========== */
const showKbManager = ref(false)
const managingKb = ref(null)
const documents = ref([])
const docLoading = ref(false)
const activeDoc = ref(null)
const previewLoading = ref(false)
const previewContent = ref('')
const previewUrl = ref('')
const previewError = ref('')

async function openKbManager(kb) {
  managingKb.value = kb
  activeDoc.value = null
  previewContent.value = ''
  previewError.value = ''
  showKbManager.value = true
  await loadDocuments(kb.id)
}

async function loadDocuments(kbId) {
  docLoading.value = true
  try {
    const res = await getKnowledgeBaseDocuments(kbId)
    documents.value = res.data || []
  } catch {
    documents.value = []
  }
  docLoading.value = false
}

const isTextFile = (doc) => doc?.fileType === 'txt' || doc?.fileType === 'md'

async function selectDoc(doc) {
  activeDoc.value = doc
  previewLoading.value = true
  previewError.value = ''
  previewContent.value = ''
  previewUrl.value = ''
  const BASE = API_BASE
  try {
    const dlRes = await fetch(`${BASE}/file/download/url?objectKey=${encodeURIComponent(doc.r2Key)}`, {
      headers: { Authorization: `Bearer ${localStorage.getItem('token') || ''}` }
    })
    const dlBody = await dlRes.json()
    if (dlBody.code !== 200 || !dlBody.data?.downloadUrl) throw new Error('获取下载链接失败')
    // 只有文本文件尝试读取内容，PDF/Word等二进制文件只展示信息
    if (isTextFile(doc)) {
      try {
        const fileRes = await fetch(dlBody.data.downloadUrl)
        if (!fileRes.ok) throw new Error(`HTTP ${fileRes.status}`)
        const text = await fileRes.text()
        previewContent.value = text.length > 50000 ? text.substring(0, 50000) + '\n\n... (内容过长已截断)' : text
      } catch {
        previewContent.value = '(该文本文件无法在线预览，请点击"打开原文件"查看)'
      }
    } else {
      previewContent.value = '(PDF/Word 文件无法在线预览，请点击下方"打开原文件"在新标签查看)'
    }
  } catch (e) {
    previewError.value = '无法加载文档信息: ' + e.message
  }
  previewLoading.value = false
}

function openOrigin(doc) {
  if (!doc?.r2Key) return
  const BASE = API_BASE
  fetch(`${BASE}/file/download/url?objectKey=${encodeURIComponent(doc.r2Key)}`, {
    headers: { Authorization: `Bearer ${localStorage.getItem('token') || ''}` }
  }).then(r => r.json()).then(body => {
    if (body.data?.downloadUrl) window.open(body.data.downloadUrl, '_blank')
  })
}

async function removeDoc(docId) {
  if (!managingKb.value) return
  try {
    await deleteKnowledgeBaseDocument(managingKb.value.id, docId)
    ElMessage.success('已删除')
    if (activeDoc.value?.id === docId) {
      activeDoc.value = null
      previewContent.value = ''
    }
    documents.value = documents.value.filter(d => d.id !== docId)
    const kb = kbs.value.find(k => k.id === managingKb.value.id)
    if (kb && kb.docCount > 0) kb.docCount--
    emit('refreshKbs')
  } catch {
    ElMessage.error('删除失败')
  }
}

async function retryDoc(docId) {
  if (!managingKb.value) return
  try {
    await retryKnowledgeBaseDocument(managingKb.value.id, docId)
    const doc = documents.value.find(item => item.id === docId)
    if (doc) doc.status = 'PROCESSING'
    ElMessage.success('已重新提交处理')
  } catch {
    ElMessage.error('重试失败')
  }
}

function refreshKbs(data) {
  kbs.value = data || []
}

watch(() => props.knowledgeBases, refreshKbs, { immediate: true })

defineExpose({ loadKbs, refreshKbs })
</script>

<style scoped>
.ai-sidebar {
  width: 296px;
  min-width: 296px;
  background: #f7f9f7;
  border-right: 1px solid var(--c-border);
  display: flex;
  flex-direction: column;
  transition: width 0.22s ease, min-width 0.22s ease, transform 0.22s ease;
  overflow: hidden;
}

.ai-sidebar.collapsed {
  width: 0;
  min-width: 0;
  border-right: none;
}

.sidebar-inner {
  width: 296px;
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 18px 14px 14px;
}

.sidebar-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  padding: 0 4px;
}

.head-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  color: var(--c-text);
}

.brand-dot {
  width: 9px;
  height: 22px;
  border-radius: 3px;
  background: var(--c-accent);
}

.new-chat-btn {
  width: 100%;
  margin-bottom: 22px;
  border-radius: var(--radius-sm);
  height: 40px;
  font-weight: 600;
  color: #fff;
  background: var(--c-primary);
  border-color: var(--c-primary);
}

.new-chat-btn:hover { color: #fff; background: var(--c-primary-dark); border-color: var(--c-primary-dark); }

.sidebar-section { margin-bottom: 18px; }

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.section-title {
  font-size: 11px;
  font-weight: 700;
  color: var(--c-text-muted);
  padding: 0 4px;
}

.section-title b {
  margin-left: 5px;
  font-size: 10px;
  font-weight: 600;
  color: var(--c-primary);
}

.add-agent-btn {
  color: var(--c-primary);
  font-size: 16px;
}

.agent-list {
  display: flex;
  flex-direction: column;
  gap: 3px;
  max-height: 144px;
  overflow-y: auto;
}

.agent-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 10px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.15s;
  border: 1px solid transparent;
}

.agent-card:hover { background: #edf2ef; }

.agent-card.active {
  background: var(--c-primary-light);
  border-color: #c7e2d9;
  box-shadow: inset 3px 0 0 var(--c-primary);
}

.agent-icon { font-size: 20px; flex-shrink: 0; }

.agent-info { flex: 1; min-width: 0; display: flex; flex-direction: column; }

.agent-name { font-size: 13px; font-weight: 600; color: var(--c-text); }

.agent-desc {
  font-size: 11px;
  color: var(--c-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.agent-del {
  font-size: 14px;
  color: var(--c-text-muted);
  opacity: 0;
  transition: opacity 0.15s;
}

.agent-card:hover .agent-del { opacity: 1; }

.agent-del:hover { color: #ef4444; }

.conversations { flex: 1; min-height: 140px; overflow: hidden; display: flex; flex-direction: column; margin-bottom: 0; }
.conversation-head { margin-bottom: 7px; }

.conv-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.conv-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 10px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.15s;
  position: relative;
  border: 1px solid transparent;
}

.conv-item:hover { background: #edf2ef; }

.conv-item.active {
  background: var(--c-primary-light);
  border-color: #c7e2d9;
}

.conv-icon { color: var(--c-text-muted); font-size: 15px; flex-shrink: 0; }

.conv-body { flex: 1; min-width: 0; }

.conv-title {
  display: block;
  font-size: 13px;
  color: var(--c-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conv-input {
  width: 100%;
  border: 1px solid var(--c-primary);
  border-radius: 4px;
  padding: 2px 6px;
  font-size: 13px;
  outline: none;
  background: var(--c-surface);
}

.conv-time { font-size: 11px; color: var(--c-text-muted); }

.conv-actions {
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.15s;
}

.conv-item:hover .conv-actions { opacity: 1; }

.action-icon {
  font-size: 14px;
  color: var(--c-text-muted);
  padding: 3px;
  border-radius: 4px;
}

.action-icon:hover {
  color: var(--c-primary);
  background: var(--c-primary-light);
}

.conv-empty {
  font-size: 13px;
  color: var(--c-text-muted);
  text-align: center;
  padding: 24px 0;
}

.close-btn { color: var(--c-text-muted); }

.icon-input { max-width: 120px; }

/* ========== 知识库 ========== */

.kb-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  max-height: 132px;
  overflow-y: auto;
}

.kb-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 10px;
  border-radius: var(--radius-sm);
  transition: all 0.15s;
  border: 1px solid transparent;
}

.kb-card:hover { background: #edf2ef; }

.kb-info {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

.kb-name {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: var(--c-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.kb-meta {
  font-size: 11px;
  color: var(--c-text-muted);
}

.kb-del {
  font-size: 14px;
  color: var(--c-text-muted);
  opacity: 0;
  transition: opacity 0.15s;
  cursor: pointer;
}

.kb-card:hover .kb-del { opacity: 1; }

.kb-del:hover { color: #ef4444; }

.kb-manage {
  font-size: 14px;
  color: var(--c-text-muted);
  opacity: 0;
  transition: opacity 0.15s;
  cursor: pointer;
}

.kb-card:hover .kb-manage { opacity: 1; }

.kb-manage:hover { color: var(--c-primary); }

/* ========== 知识库文档管理弹窗 ========== */

.kb-manager-dialog :deep(.el-dialog__header) { padding: 16px 20px; margin: 0; }
.kb-manager-dialog :deep(.el-dialog__body) { padding: 0; }

.kb-mgr-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: var(--c-text);
}

.kb-mgr-count {
  font-size: 13px;
  font-weight: 400;
  color: var(--c-text-muted);
}

.kb-mgr-body {
  display: flex;
  height: 500px;
  border-top: 1px solid var(--c-border);
}

.kb-mgr-list {
  width: 280px;
  min-width: 280px;
  border-right: 1px solid var(--c-border);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px;
}

.kb-mgr-list .doc-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.15s;
}

.kb-mgr-list .doc-item:hover { background: #f5f4f8; }

.kb-mgr-list .doc-item.active {
  background: var(--c-primary-light);
  border-color: var(--c-primary);
}

.kb-mgr-list .doc-icon {
  flex-shrink: 0;
  font-size: 20px;
  display: flex;
  align-items: center;
}

.kb-mgr-list .doc-body {
  flex: 1; min-width: 0;
  display: flex; flex-direction: column;
}

.doc-status {
  flex-shrink: 0;
  padding: 2px 6px;
  border-radius: 4px;
  background: #f1f3f2;
  color: var(--c-text-muted);
  font-size: 10px;
}
.doc-status.ready { background: var(--c-primary-light); color: var(--c-primary); }
.doc-status.processing { background: #fff7df; color: #9a6811; }
.doc-status.failed { background: #fff0eb; color: #b94d38; }
.doc-actions { display: flex; flex-shrink: 0; }

.kb-mgr-list .doc-name {
  font-size: 13px; font-weight: 500; color: var(--c-text);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

.kb-mgr-list .doc-meta {
  font-size: 11px; color: var(--c-text-muted); margin-top: 2px;
}

.kb-mgr-list .doc-del-btn {
  flex-shrink: 0; opacity: 0; transition: opacity 0.15s;
}

.kb-mgr-list .doc-item:hover .doc-del-btn { opacity: 1; }

.doc-empty {
  text-align: center; padding: 40px 0;
  color: var(--c-text-muted); font-size: 14px;
}

/* 右侧预览 */
.kb-mgr-preview {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.preview-placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--c-text-muted);
  font-size: 14px;
}

.preview-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--c-border);
}

.preview-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.preview-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.preview-text {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--c-text-secondary);
  white-space: pre-wrap;
  word-break: break-all;
  font-family: 'SF Mono', Consolas, 'Microsoft YaHei', monospace;
}

.preview-error {
  color: #ef4444;
  font-size: 13px;
  padding: 20px;
  text-align: center;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
  border-radius: 0;
}

.preview-placeholder-inner {
  text-align: center;
  padding: 40px;
  color: var(--c-text-muted);
  font-size: 14px;
}

.is-loading {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@media (max-width: 860px) {
  .ai-sidebar {
    position: absolute;
    inset: 0 auto 0 0;
    z-index: 40;
    width: min(88vw, 310px);
    min-width: min(88vw, 310px);
    box-shadow: var(--c-shadow-lg);
  }
  .ai-sidebar.collapsed {
    width: min(88vw, 310px);
    min-width: min(88vw, 310px);
    transform: translateX(-101%);
  }
  .sidebar-inner { width: min(88vw, 310px); }
  .kb-manager-dialog :deep(.el-dialog) { width: calc(100vw - 24px) !important; margin: 12px auto; }
  .kb-mgr-body { height: min(72vh, 560px); flex-direction: column; }
  .kb-mgr-list { width: 100%; min-width: 0; height: 220px; border-right: 0; border-bottom: 1px solid var(--c-border); }
}
</style>
