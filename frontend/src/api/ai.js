import request, { API_BASE } from './request.js'

const BASE = API_BASE
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

export function streamChat(conversationId, message, agentId, knowledgeBaseIds, documentIds, signal) {
  const params = new URLSearchParams({ message })
  if (conversationId) params.set('conversationId', conversationId)
  if (agentId) params.set('agentId', agentId)
  if (knowledgeBaseIds?.length) params.set('knowledgeBaseIds', knowledgeBaseIds.join(','))
  if (documentIds?.length) params.set('documentIds', documentIds.join(','))
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

/* ========== RAG 知识库 ========== */

export function getKnowledgeBases() {
  return request({ url: '/ai/knowledge-bases', method: 'GET' })
}

export function createKnowledgeBase(data) {
  return request({ url: '/ai/knowledge-bases', method: 'POST', data })
}

export function deleteKnowledgeBase(id) {
  return request({ url: `/ai/knowledge-bases/${id}`, method: 'DELETE' })
}

export function getKnowledgeBaseDocuments(id, params) {
  return request({ url: `/ai/knowledge-bases/${id}/documents`, method: 'GET', params })
}

export function deleteKnowledgeBaseDocument(kbId, docId) {
  return request({ url: `/ai/knowledge-bases/${kbId}/documents/${docId}`, method: 'DELETE' })
}

export function retryKnowledgeBaseDocument(kbId, docId) {
  return request({ url: `/ai/knowledge-bases/${kbId}/documents/${docId}/retry`, method: 'POST' })
}

export function uploadRagFiles(files, knowledgeBaseId) {
  const form = new FormData()
  files.forEach(f => form.append('files', f))
  form.append('knowledgeBaseId', knowledgeBaseId)
  return fetch(`${BASE}/ai/rag/upload`, {
    method: 'POST',
    headers: { Authorization: headers().Authorization },
    body: form
  })
}

export function submitRagArticles(articleIds, knowledgeBaseId) {
  return request({
    url: '/ai/rag/articles',
    method: 'POST',
    data: { articleIds, knowledgeBaseId }
  })
}
