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
          <span class="section-title">Agent</span>
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

      <div class="sidebar-section conversations">
        <div class="section-title">对话历史</div>
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
  </aside>
</template>

<script setup>
import { ref, reactive, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, ChatDotRound, ChatLineSquare, EditPen, Delete, Plus } from '@element-plus/icons-vue'
import { createAgent, deleteAgent } from '@/api/ai.js'

const props = defineProps({
  open: { type: Boolean, default: true },
  activeId: { type: [String, Number], default: null },
  conversations: { type: Array, default: () => [] },
  agents: { type: Array, default: () => [] },
  selectedAgent: { type: [String, Number], default: null }
})

const emit = defineEmits(['toggle', 'select', 'new', 'rename', 'delete', 'agentChange', 'refreshAgents'])

const editingId = ref(null)
const showAddAgent = ref(false)
const agentForm = reactive({ name: '', systemPrompt: '', icon: '🤖' })

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
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
</script>

<style scoped>
.ai-sidebar {
  width: 260px;
  min-width: 260px;
  background: #f8f7fb;
  border-right: 1px solid var(--c-border);
  display: flex;
  flex-direction: column;
  transition: all 0.25s ease;
  overflow: hidden;
}

.ai-sidebar.collapsed {
  width: 0;
  min-width: 0;
  border-right: none;
}

.sidebar-inner {
  width: 260px;
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
}

.sidebar-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.head-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 17px;
  font-weight: 700;
  color: var(--c-text);
}

.brand-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--c-primary);
}

.new-chat-btn {
  width: 100%;
  margin-bottom: 20px;
  border-radius: var(--radius-sm);
  height: 42px;
  font-weight: 600;
}

.sidebar-section { margin-bottom: 16px; }

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.section-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--c-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  padding: 0 4px;
}

.add-agent-btn {
  color: var(--c-primary);
  font-size: 16px;
}

.agent-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.agent-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.15s;
  border: 1px solid transparent;
}

.agent-card:hover { background: var(--c-primary-light); }

.agent-card.active {
  background: var(--c-primary-light);
  border-color: var(--c-primary);
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

.conversations { flex: 1; overflow: hidden; display: flex; flex-direction: column; }

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

.conv-item:hover { background: #ede9f7; }

.conv-item.active {
  background: var(--c-primary-light);
  border-color: var(--c-primary);
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
</style>
