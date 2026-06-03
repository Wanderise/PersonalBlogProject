import request from './request.js'

const BASE = 'http://localhost:8080'
const headers = () => ({ Authorization: `Bearer ${localStorage.getItem('token') || ''}` })

/* ========== 会话 ========== */

export function getConversations() {
  return request({ url: '/ai/conversations', method: 'GET' })
}

export function createConversation(data) {
  return request({ url: '/ai/conversations', method: 'POST', data })
}

export function renameConversation(id, title) {
  return request({ url: `/ai/conversations/${id}`, method: 'PUT', data: { title } })
}

export function deleteConversation(id) {
  return request({ url: `/ai/conversations/${id}`, method: 'DELETE' })
}

export function getMessages(conversationId) {
  return request({ url: `/ai/conversations/${conversationId}/messages`, method: 'GET' })
}

/* ========== 流式对话 ========== */

export function streamChat(conversationId, message, agentId, signal) {
  const params = new URLSearchParams({ message })
  if (conversationId) params.set('conversationId', conversationId)
  if (agentId) params.set('agentId', agentId)
  return fetch(`${BASE}/ai/chat/stream?${params}`, { headers: headers(), signal })
}

/* ========== Agent ========== */

export function getAgents() {
  return request({ url: '/ai/agents', method: 'GET' })
}

export function createAgent(data) {
  return request({ url: '/ai/agents', method: 'POST', data })
}

export function deleteAgent(id) {
  return request({ url: `/ai/agents/${id}`, method: 'DELETE' })
}
