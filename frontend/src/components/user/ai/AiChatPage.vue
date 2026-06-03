<template>
  <div class="ai-page">
    <AiSidebar
      :open="sidebarOpen"
      :active-id="activeConvId"
      :conversations="conversations"
      :agents="agents"
      :selected-agent="selectedAgent"
      @toggle="sidebarOpen = !sidebarOpen"
      @new="handleNew"
      @select="handleSelect"
      @rename="handleRename"
      @delete="handleDelete"
      @agent-change="handleAgentChange"
      @refresh-agents="loadAgents"
    />

    <AiChatView
      v-if="activeConv"
      :messages="messages"
      :streaming="streaming"
      :stream-content="streamContent"
      :title="activeConv.title"
      :agent="selectedAgent"
      @toggle-sidebar="sidebarOpen = !sidebarOpen"
      @send="handleSend"
      ref="chatViewRef"
    />

    <div v-else class="ai-empty">
      <div class="empty-content">
        <div class="welcome-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 2a4 4 0 0 1 4 4v.5a4 4 0 0 1-2.5 3.7c-1.2.5-2.5.5-3.7 0A4 4 0 0 1 8 6.5V6a4 4 0 0 1 4-4z"/>
            <path d="M8 14a4 4 0 0 0-4 4v2h16v-2a4 4 0 0 0-4-4H8z"/>
            <circle cx="12" cy="12" r="3"/>
          </svg>
        </div>
        <h3>选择或创建一个对话开始</h3>
        <p>点击左侧「开始对话」或选择历史记录</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import AiSidebar from './AiSidebar.vue'
import AiChatView from './AiChatView.vue'
import {
  getConversations, createConversation, renameConversation, deleteConversation,
  getMessages, streamChat, getAgents
} from '@/api/ai.js'

const sidebarOpen = ref(true)
const activeConvId = ref(null)
const agents = ref([])
const selectedAgent = ref(null)
const conversations = ref([])
const messages = ref([])
const streaming = ref(false)
const streamContent = ref('')
const chatViewRef = ref(null)
let abortCtrl = null

const activeConv = computed(() => conversations.value.find(c => c.id === activeConvId.value) || null)

onMounted(async () => {
  await Promise.all([loadConversations(), loadAgents()])
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

async function handleNew() {
  try {
    const res = await createConversation({ title: '新对话', agentId: selectedAgent.value })
    const conv = res.data
    conversations.value.unshift(conv)
    activeConvId.value = conv.id
    messages.value = []
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
      role: m.role === 'USER' ? 'user' : 'assistant',
      content: m.content
    }))
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

async function handleSend(text) {
  if (!activeConvId.value) return
  const convId = activeConvId.value

  messages.value.push({ role: 'user', content: text })

  if (!activeConv.value || activeConv.value.title === '新对话') {
    const title = text.slice(0, 30) + (text.length > 30 ? '...' : '')
    const conv = conversations.value.find(c => c.id === convId)
    if (conv) conv.title = title
  }

  streaming.value = true
  streamContent.value = ''

  try {
    abortCtrl = new AbortController()
    const response = await streamChat(convId, text, selectedAgent.value, abortCtrl.signal)

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
  height: calc(100vh - 58px);
  overflow: hidden;
}

.ai-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--c-surface);
}

.empty-content { text-align: center; }

.welcome-icon {
  width: 72px;
  height: 72px;
  border-radius: 18px;
  background: var(--c-primary-light);
  color: var(--c-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.welcome-icon svg { width: 36px; height: 36px; }

.empty-content h3 {
  font-size: 22px;
  font-weight: 700;
  color: var(--c-text);
  margin-bottom: 8px;
}

.empty-content p {
  font-size: 15px;
  color: var(--c-text-muted);
}
</style>
