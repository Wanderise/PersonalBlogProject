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
        <p>开始一段新的对话吧</p>
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
      <div class="input-wrapper">
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
          :type="inputText.trim() ? 'primary' : 'default'"
          circle
          size="small"
          :disabled="!inputText.trim() || streaming"
          @click="send"
        >
          <el-icon><Promotion /></el-icon>
        </el-button>
      </div>
      <p class="input-hint">AI 回答可能存在错误，请注意甄别</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import { Expand, Promotion } from '@element-plus/icons-vue'
import { marked } from 'marked'

const props = defineProps({
  messages: { type: Array, default: () => [] },
  streaming: { type: Boolean, default: false },
  streamContent: { type: String, default: '' },
  title: { type: String, default: '' },
  agent: { type: String, default: 'general' }
})

const emit = defineEmits(['toggle-sidebar', 'send', 'update:streaming', 'update:streamContent'])

const inputText = ref('')
const inputRef = ref(null)
const msgContainer = ref(null)
const scrollAnchor = ref(null)

const agentGreeting = computed(() => {
  const map = { general: '你好！我是通用助手', coder: '你好！我是代码专家', writer: '你好！我是写作助手' }
  return map[props.agent] || '你好！有什么可以帮你的？'
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

function handleEnter() {
  const text = inputText.value.trim()
  if (!text || props.streaming) return
  emit('send', text)
  inputText.value = ''
  nextTick(() => {
    if (inputRef.value) { inputRef.value.style.height = 'auto' }
  })
}

function send() {
  handleEnter()
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

.input-hint {
  text-align: center;
  font-size: 11px;
  color: var(--c-text-muted);
  margin: 8px 0 0;
}
</style>
