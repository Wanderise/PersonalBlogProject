<template>
  <div class="ai-page">
    <AiSidebar
      :open="sidebarOpen"
      :active-id="activeConvId"
      :conversations="conversations"
      :agents="agents"
      :selected-agent="selectedAgent"
      :knowledge-bases="kbs"
      :selected-kb-ids="selectedKbIds"
      @toggle="sidebarOpen = !sidebarOpen"
      @new="handleNew"
      @select="handleSelect"
      @rename="handleRename"
      @delete="handleDelete"
      @agent-change="handleAgentChange"
      @refresh-agents="loadAgents"
      @kbs-change="selectedKbIds = $event"
      @refresh-kbs="loadKbs"
      ref="sidebarRef"
    />
    <button
      v-if="sidebarOpen"
      class="sidebar-scrim"
      aria-label="关闭侧栏"
      @click="sidebarOpen = false"
    ></button>

    <AiChatView
      v-if="activeConv"
      :messages="messages"
      :streaming="streaming"
      :stream-content="streamContent"
      :title="activeConv.title"
      :agent="selectedAgent"
      :selected-kb-ids="selectedKbIds"
      @toggle-sidebar="sidebarOpen = !sidebarOpen"
      @send="handleSend"
      @kb-refresh="loadKbs"
      ref="chatViewRef"
    />

    <div v-else class="ai-empty">
      <div class="empty-content">
        <button class="mobile-sidebar-trigger" aria-label="打开侧栏" @click="sidebarOpen = true">
          <el-icon><Menu /></el-icon>
        </button>
        <div class="empty-kicker"><el-icon><MagicStick /></el-icon> AI 写作工作台</div>
        <h1>从一个问题开始</h1>
        <p class="empty-lead">选择助手和知识库，让散落的文章、资料与想法在同一处发生联系。</p>
        <button class="start-button" @click="handleNew">
          <el-icon><ChatDotRound /></el-icon>
          创建新对话
        </button>
        <div class="workspace-status">
          <div><strong>{{ agents.length }}</strong><span>可用助手</span></div>
          <div><strong>{{ kbs.length }}</strong><span>知识库</span></div>
          <div><strong>{{ conversations.length }}</strong><span>历史对话</span></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatDotRound, MagicStick, Menu } from '@element-plus/icons-vue'
import AiSidebar from './AiSidebar.vue'
import AiChatView from './AiChatView.vue'
import {
  getConversations, createConversation, renameConversation, deleteConversation,
  getMessages, streamChat, getAgents, getKnowledgeBases
} from '@/api/ai.js'

const sidebarOpen = ref(window.innerWidth > 860)
const activeConvId = ref(null)
const agents = ref([])
const selectedAgent = ref(null)
const conversations = ref([])
const messages = ref([])
const streaming = ref(false)
const streamContent = ref('')
const chatViewRef = ref(null)
const sidebarRef = ref(null)
const kbs = ref([])
const selectedKbIds = ref([])
let abortCtrl = null

const activeConv = computed(() => conversations.value.find(c => c.id === activeConvId.value) || null)

onMounted(async () => {
  await Promise.all([loadConversations(), loadAgents(), loadKbs()])
})

async function loadConversations() {
  try {
    const res = await getConversations()
    conversations.value = res.data || []
  } catch { /* ignore */ }
}

async function loadAgents() {
  try {
    const res = await getAgents()
    agents.value = res.data || []
    if (agents.value.length && !selectedAgent.value) {
      selectedAgent.value = agents.value[0].id
    }
  } catch { /* ignore */ }
}

async function loadKbs() {
  try {
    const res = await getKnowledgeBases()
    kbs.value = res.data || []
    const validIds = new Set(kbs.value.map(kb => kb.id))
    selectedKbIds.value = selectedKbIds.value.filter(id => validIds.has(id))
    if (sidebarRef.value) sidebarRef.value.refreshKbs(kbs.value)
  } catch { /* ignore */ }
}

async function handleNew() {
  try {
    const res = await createConversation({ title: '新对话', agentId: selectedAgent.value })
    const conv = res.data
    conversations.value.unshift(conv)
    activeConvId.value = conv.id
    messages.value = []
    if (window.innerWidth <= 860) sidebarOpen.value = false
  } catch {
    ElMessage.error('创建对话失败')
  }
}

async function handleSelect(id) {
  activeConvId.value = id
  messages.value = []
  try {
    const res = await getMessages(id)
    messages.value = (res.data || []).map(m => ({
      role: m.role === 'user' ? 'user' : 'assistant',
      content: m.content
    }))
    if (window.innerWidth <= 860) sidebarOpen.value = false
  } catch { /* ignore */ }
}

async function handleRename({ id, title }) {
  try {
    await renameConversation(id, title)
    const conv = conversations.value.find(c => c.id === id)
    if (conv) conv.title = title
  } catch {
    ElMessage.error('重命名失败')
  }
}

async function handleDelete(id) {
  try {
    await deleteConversation(id)
    conversations.value = conversations.value.filter(c => c.id !== id)
    if (activeConvId.value === id) {
      activeConvId.value = conversations.value[0]?.id || null
      messages.value = []
    }
  } catch {
    ElMessage.error('删除失败')
  }
}

function handleAgentChange(id) {
  selectedAgent.value = id
}

async function handleSend({ text, attachments }) {
  if (!activeConvId.value) return
  const convId = activeConvId.value

  const attList = attachments || []
  const hasAttach = attList.length > 0
  const attachNames = hasAttach ? attList.map(a => a.name).join('、') : ''
  const aiPrompt = text || (hasAttach ? `请参考知识库中刚上传的文件：${attachNames}` : '')

  messages.value.push({
    role: 'user',
    content: text || '',
    attachments: hasAttach ? attList.map(a => ({ type: a.type, name: a.name, r2Key: a.r2Key, id: a.id })) : undefined
  })

  if (!activeConv.value || activeConv.value.title === '新对话') {
    const title = text ? (text.slice(0, 30) + (text.length > 30 ? '...' : '')) : (hasAttach ? `附件：${attList[0].name}` : '新对话')
    const conv = conversations.value.find(c => c.id === convId)
    if (conv) {
      conv.title = title
      renameConversation(convId, title).catch(() => {})
    }
  }

  streaming.value = true
  streamContent.value = ''

  try {
    abortCtrl = new AbortController()
    const response = await streamChat(
      convId,
      aiPrompt,
      selectedAgent.value,
      selectedKbIds.value,
      abortCtrl.signal
    )

    if (!response.ok) throw new Error(`HTTP ${response.status}`)

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      streamContent.value = buffer
    }
  } catch (e) {
    if (e.name !== 'AbortError') {
      ElMessage.error('请求失败: ' + e.message)
      messages.value.push({ role: 'assistant', content: '[请求出错，请重试]' })
    }
  } finally {
    if (streamContent.value) {
      messages.value.push({ role: 'assistant', content: streamContent.value })
    }
    streaming.value = false
    streamContent.value = ''
    abortCtrl = null
  }
}
</script>

<style scoped>
.ai-page {
  display: flex;
  height: calc(100vh - 64px);
  overflow: hidden;
  position: relative;
  background: var(--c-bg);
}

.ai-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--c-surface-soft);
  position: relative;
}

.empty-content {
  width: min(620px, calc(100% - 40px));
  text-align: left;
}

.empty-kicker {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: var(--c-primary);
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 20px;
}

.empty-content h1 {
  font-family: Georgia, "Microsoft YaHei", serif;
  font-size: 42px;
  line-height: 1.2;
  font-weight: 600;
  color: var(--c-text);
  margin-bottom: 14px;
}

.empty-lead {
  max-width: 520px;
  font-size: 16px;
  color: var(--c-text-secondary);
  margin-bottom: 28px;
}

.start-button {
  height: 42px;
  padding: 0 18px;
  border: 0;
  border-radius: 6px;
  background: var(--c-primary);
  color: #fff;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
  transition: background var(--transition), transform var(--transition);
}

.start-button:hover { background: var(--c-primary-dark); transform: translateY(-1px); }

.workspace-status {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  max-width: 480px;
  margin-top: 52px;
  padding-top: 20px;
  border-top: 1px solid var(--c-border);
}

.workspace-status div { display: flex; flex-direction: column; gap: 2px; }
.workspace-status strong { font-size: 20px; color: var(--c-text); }
.workspace-status span { font-size: 12px; color: var(--c-text-muted); }

.sidebar-scrim,
.mobile-sidebar-trigger { display: none; }

@media (max-width: 860px) {
  .ai-page { height: calc(100vh - 58px); }
  .sidebar-scrim {
    display: block;
    position: absolute;
    inset: 0;
    z-index: 39;
    border: 0;
    background: rgba(23, 33, 31, 0.32);
  }
  .mobile-sidebar-trigger {
    display: grid;
    place-items: center;
    width: 36px;
    height: 36px;
    margin-bottom: 28px;
    border: 1px solid var(--c-border);
    border-radius: 6px;
    background: #fff;
    color: var(--c-text-secondary);
  }
  .empty-content h1 { font-size: 32px; }
  .workspace-status { margin-top: 40px; }
}
</style>
