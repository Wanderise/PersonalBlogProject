# 后端 API 接口文档

> 基础路径: `http://localhost:8080`  
> 响应格式: `{ "code": 200, "msg": null, "data": ... }`  
> code=200 成功，code=400 失败

---

## 一、用户模块

### 1.1 注册

```
POST /user/register
认证: 否
```

**请求体:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 2-20 字符 |
| password | String | 是 | 6-30 字符 |

```json
{ "name": "zhangsan", "password": "123456" }
```

**成功响应:**

```json
{ "code": 200, "msg": null, "data": null }
```

**失败响应:**

```json
{ "code": 400, "msg": "用户名已存在", "data": null }
```

**后端需处理:**
1. 校验 `name` 唯一性
2. MD5 加密 `password` 后存储
3. `level` 默认 0，`gmt_create` 设为当前时间

---

### 1.2 登录

```
POST /user/login
认证: 否
```

**请求体:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 用户名 |
| password | String | 是 | 密码 |

```json
{ "name": "zhangsan", "password": "123456" }
```

**成功响应:**

```json
{
  "code": 200, "msg": null,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "user": { "id": 1, "name": "zhangsan", "image": null, "level": 0 }
  }
}
```

**失败响应:**

```json
{ "code": 400, "msg": "用户名或密码错误", "data": null }
```

**后端需处理:**
1. 根据 `name` 查询用户
2. MD5 比对密码
3. 生成 JWT Token（payload 包含 `userId` 和 `name`，有效期 7 天）

---

### 1.3 获取当前用户信息

```
GET /user/info
认证: 是
Authorization: Bearer {token}
```

**成功响应:**

```json
{
  "code": 200, "msg": null,
  "data": {
    "id": 1, "name": "zhangsan",
    "image": null, "level": 0, "gmtCreate": "2026-05-15T12:00:00"
  }
}
```

**失败响应:**

```json
{ "code": 400, "msg": "未登录或 token 已过期", "data": null }
```

**后端需处理:**
1. 从 Authorization 头提取 token 并校验
2. 根据 token 中 `userId` 查询用户，不返回密码

---

### 1.4 更新用户信息

```
PUT /user/info
认证: 是
```

**请求体:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 新用户名，2-20 字符 |

```json
{ "name": "新用户名" }
```

**成功响应:**

```json
{
  "code": 200, "msg": null,
  "data": {
    "id": 1, "name": "新用户名",
    "image": "avatars/1_1684512000000.jpg", "level": 0,
    "gmtCreate": "2026-05-15T12:00:00"
  }
}
```

**失败响应:**

```json
{ "code": 400, "msg": "用户名已存在", "data": null }
```

**后端需处理:**
1. 从 `UserContext` 获取当前用户
2. 校验新 `name` 未被他人占用
3. 更新 `blog.user` 的 `name` 和 `gmt_modified`

---

### 1.5 更新用户头像

```
PUT /user/avatar
认证: 是
```

**请求体:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| objectKey | String | 是 | R2 存储的 key，如 `avatars/1_1684512000000.jpg` |

```json
{ "objectKey": "avatars/1_1684512000000.jpg" }
```

**成功响应:**

```json
{ "code": 200, "msg": null, "data": null }
```

**后端需处理:**
1. 从 `UserContext` 获取当前用户
2. 将 `objectKey` 写入 `blog.user.image`
3. `image` 存 key（非完整 URL），前端通过 `/file/download/url` 获取预签名 URL 显示

---

### 1.6 更新用户密码

```
PUT /user/password
认证: 是
```

**请求体:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| oldPassword | String | 是 | 当前密码 |
| newPassword | String | 是 | 新密码，6-30 字符 |

```json
{ "oldPassword": "123456", "newPassword": "654321" }
```

**成功响应:**

```json
{ "code": 200, "msg": null, "data": null }
```

**失败响应:**

```json
{ "code": 400, "msg": "当前密码错误", "data": null }
```

**后端需处理:**
1. 从 `UserContext` 获取当前用户
2. MD5 比对 `oldPassword`
3. 通过后 MD5 加密 `newPassword` 写入 DB，更新 `gmt_modified`

---

## 二、文件模块

### 2.1 获取上传预签名 URL

```
POST /file/upload/url
认证: 是
```

**请求体:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| objectKey | String | 是 | R2 目标路径 |
| contentType | String | 是 | 文件 MIME 类型 |

```json
{ "objectKey": "images/xxx.jpg", "contentType": "image/jpeg" }
```

**成功响应:**

```json
{
  "code": 200, "msg": null,
  "data": { "uploadUrl": "https://<r2-presigned-put-url>" }
}
```

---

### 2.2 获取下载预签名 URL

```
GET /file/download/url?objectKey=images/xxx.jpg
认证: 否
```

**请求参数:**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| objectKey | String | 是 | R2 文件 key |

**成功响应:**

```json
{
  "code": 200, "msg": null,
  "data": { "downloadUrl": "https://<r2-presigned-get-url>" }
}
```

---

**文件上传流程:**

```
前端选择图片
  → POST /file/upload/url { objectKey, contentType }  获取上传预签名 URL
  → PUT 预签名 URL（文件直传 R2，不经过后端）
  → PUT /user/avatar { objectKey }                    写入 DB
  → GET /file/download/url?objectKey=xxx               获取下载预签名 URL
  → <img src="...">                                   直连 R2 显示
```

---

## 三、文章模块

### 3.1 发布文章

```
POST /article/add
认证: 是
```

**请求体:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | String | 是 | 文章标题，1-100 字符 |
| content | String | 是 | Markdown 正文 |
| tag | List\<String\> | 否 | 标签列表 |
| image | List\<String\> | 否 | 配图 objectKey 数组 |

```json
{
  "title": "Spring Boot 入门",
  "content": "# 第一章\n...",
  "tag": ["Java", "Spring"],
  "image": ["covers/1_1684512000000.jpg"]
}
```

**成功响应:**

```json
{ "code": 200, "msg": null, "data": { "id": 1 } }
```

**后端需处理:**
1. `writerId` 从 token 获取
2. `tag` 写入 `article_tag` 和 `tag` 表
3. `gmtCreate` = `gmtModified` = 当前时间

---

### 3.2 编辑文章

```
PUT /article/{id}
认证: 是
```

**请求体:** 同 3.1

```json
{
  "title": "Spring Boot 入门（修订版）",
  "content": "# 第一章\n...",
  "tag": ["Java", "Spring Boot"],
  "image": ["covers/1_1684512000000.jpg"]
}
```

**成功响应:**

```json
{ "code": 200, "msg": null, "data": null }
```

**失败响应:**

```json
{ "code": 400, "msg": "只能修改自己的文章", "data": null }
```

## 后端需处理:**
1. 校验当前用户是否为作者
2. **先将当前文章内容写入 `article_version` 表（自动归档旧版本）**
3. 更新 `article` 表 + 重建 `article_tag` 关联
4. 更新 `gmtModified`

---

### 3.3 删除文章

```
DELETE /article/{id}
认证: 是
```

**成功响应:**

```json
{ "code": 200, "msg": null, "data": null }
```

**后端需处理:**
1. 校验作者身份
2. 删除 `article` 记录 + `article_tag` 关联
3. 遍历 `image` JSON 数组，逐一删除 R2 文件

---

### 3.4 获取文章详情

```
GET /article/{id}
认证: 否
```

**成功响应:**

```json
{
  "code": 200, "msg": null,
  "data": {
    "id": 1,
    "title": "Spring Boot 入门",
    "content": "# 第一章\n...",
    "tag": ["Java", "Spring"],
    "image": ["covers/1_xxx.jpg"],
    "imageUrls": ["https://<r2-presigned-url>"],
    "writerId": 1,
    "writerName": "zhangsan",
    "gmtCreate": "2026-05-15T12:00:00",
    "gmtModified": "2026-05-16T08:30:00"
  }
}
```

**后端需处理:**
1. JOIN `user` 表取 `writerName`
2. `tag` 从 `article_tag` 关联表查询
3. `image` JSON 反序列化后，为每个 key 生成预签名 URL 填入 `imageUrls`

---

### 3.5 文章列表（公开）

```
GET /article/list?page=1&size=10&keyword=Spring&tag=Java
认证: 否
```

**请求参数:**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 默认 1 |
| size | Integer | 否 | 默认 10 |
| keyword | String | 否 | 标题/内容模糊搜索 |
| tag | String | 否 | 按标签名筛选 |

**成功响应:**

```json
{
  "code": 200, "msg": null,
  "data": {
    "total": 25, "page": 1, "size": 10,
    "articles": [
      {
        "id": 1, "title": "Spring Boot 入门",
        "summary": "第一章...",
        "tag": ["Java", "Spring"],
        "image": ["covers/1_xxx.jpg"],
        "imageUrls": ["https://<r2-presigned-url>"],
        "writerId": 1, "writerName": "zhangsan",
        "gmtCreate": "2026-05-15T12:00:00"
      }
    ]
  }
}
```

**后端需处理:**
1. PageHelper 分页，返回 `total` / `page` / `size` / `articles`
2. `content` 截取前 200 字符为 `summary`
3. `keyword` → LIKE `%keyword%`，`tag` → EXISTS 子查询 `article_tag`
4. 按 `gmtModified` 降序

---

### 3.6 我的文章列表

```
GET /article/my?page=1&size=10
认证: 是
```

**请求参数:** 同 3.5（无需 keyword / tag）

**成功响应:**

```json
{
  "code": 200, "msg": null,
  "data": {
    "total": 5, "page": 1, "size": 10,
    "articles": [
      {
        "id": 1, "title": "Spring Boot 入门",
        "summary": "第一章...",
        "tag": ["Java", "Spring"],
        "image": ["covers/1_xxx.jpg"],
        "imageUrls": ["https://<r2-presigned-url>"],
        "writerId": 1, "writerName": "zhangsan",
        "gmtCreate": "2026-05-15T12:00:00",
        "gmtModified": "2026-05-16T08:30:00"
      }
    ]
  }
}
```

**后端需处理:**
1. 从 token 获取 `userId`，只返回该用户的文章
2. 分页结构同 3.5

---

### 3.7 文章版本历史

```
GET /article/{id}/versions
认证: 是
```

**成功响应:**

```json
{
  "code": 200, "msg": null,
  "data": [
    { "versionId": 3, "versionNumber": 3, "title": "Spring Boot 入门（最新版）", "summary": "...", "gmtCreate": "2026-06-05T16:00:00" },
    { "versionId": 2, "versionNumber": 2, "title": "Spring Boot 入门（修订版）", "summary": "...", "gmtCreate": "2026-06-04T10:00:00" },
    { "versionId": 1, "versionNumber": 1, "title": "Spring Boot 入门", "summary": "...", "gmtCreate": "2026-06-03T08:00:00" }
  ]
}
```

**后端需处理:** 校验当前用户是否为作者，按 `version_number` 降序返回。

---

### 3.8 回滚到指定版本

```
POST /article/{id}/versions/{versionId}/rollback
认证: 是
```

**成功响应:**

```json
{
  "code": 200, "msg": null,
  "data": {
    "id": 1, "title": "Spring Boot 入门",
    "content": "# 第一章\n...",
    "tag": ["Java", "Spring"],
    "writerId": 1, "writerName": "zhangsan",
    "gmtModified": "2026-06-05T17:00:00"
  }
}
```

**后端需处理:**
1. 校验当前用户是否为作者
2. 将当前文章内容存为新版本（`version_number = max + 1`）
3. 用目标版本的内容覆盖 `article` 表（`title`、`content`、`tag` 等）
4. 更新 `gmt_modified`

---

### 3.9 获取指定版本内容

```
GET /article/{id}/versions/{versionId}
认证: 是
```

**成功响应:**

```json
{
  "code": 200, "msg": null,
  "data": {
    "versionId": 2, "versionNumber": 2,
    "title": "Spring Boot 入门（修订版）",
    "content": "# 第一章\n...",
    "tag": ["Java", "Spring Boot"],
    "gmtCreate": "2026-06-04T10:00:00"
  }
}
```

---

### 3.10 版本管理设计

**编辑文章时自动创建版本:** 调用 `PUT /article/{id}` 编辑文章时，后端先将当前内容写入 `article_version` 表，再更新 `article` 表。**前端无需改动。**

**article_version 表:**

| 列 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| article_id | BIGINT FK | 关联 article.id |
| version_number | INT | 版本号，同篇文章下递增 |
| title | VARCHAR(100) | 该版本标题 |
| content | TEXT | 该版本 Markdown 正文 |
| tag | VARCHAR(500) | 该版本标签（JSON 数组序列化） |
| gmt_create | DATETIME | 创建时间（即该版本的归档时间） |

**回滚实现要点:**
- 回滚不是删除中间版本，而是以当前内容创建一个新版本号，再将旧版本内容恢复到主表
- 这样回滚操作本身也是可追溯的

---

## 四、AI 聊天模块

> 前端路由: `/ai`。存储由后端负责，前端仅做 UI 渲染。

---

### 4.1 流式对话

```
GET /ai/chat/stream?message=你好&conversationId=1&agentId=2&knowledgeBaseIds=1,2
认证: 是
Authorization: Bearer {token}
```

> 核心 AI 聊天接口。后端自动从指定的知识库中检索相关文本拼入上下文。前端使用 `ReadableStream` 逐块读取 AI 生成内容，实时渲染 Markdown，非标准 SSE 协议。

---

**请求参数（Query String）:**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| message | String | 是 | 用户消息文本 |
| conversationId | Integer | 否 | 会话 ID。首次对话不传，后端自动创建会话 |
| agentId | Integer | 否 | Agent ID，用于匹配自定义 system prompt。未匹配到时使用默认 prompt |
| knowledgeBaseIds | String | 否 | 逗号分隔的知识库 ID，如 `1,2`。后端据此检索向量数据库取相关文本拼入上下文 |

---

**响应格式:**

- `Content-Type: text/html;charset=UTF-8`（非标准 SSE，无 `data:` 前缀）
- 返回 `Flux<String>` 响应式流，AI 逐 token 输出原始文本
- 前端通过 `response.body.getReader()` 读取字节流，`TextDecoder` 解码后实时渲染 Markdown
- 流自然结束即响应完成，无结束标记

**流中示例（raw bytes）:**
```
你好！有什么可以帮助你的？根据你提供的文章...
```

---

**后端处理流程:**

```
1. 接收参数
     ├─ conversationId == null → 创建新会话（ai_conversation 表）
     ├─ agentId != null → 查 ai_agent 表取 systemPrompt
     └─ 未匹配 → 使用默认 prompt: "你是一个有帮助的AI助手"

2. 构建上下文
     ├─ 查 ai_message 表取该会话最近消息（历史对话）
     ├─ articleIds 非空 → 查 article 表取文章正文
     └─ fileKeys 非空 → 从 R2 下载文件，解析 PDF/Word/TXT 文本

3. 保存用户消息
     └─ INSERT INTO ai_message (conversation_id, role='user', content=message, gmt_create)

4. 调用 DeepSeek API
     └─ chatClient.prompt(systemPrompt).user(message).messages(history).stream().content()

5. 流式输出（Flux<String>）
     └─ 逐 token 写入 Response body

6. 流结束后保存 AI 回复
     └─ INSERT INTO ai_message (conversation_id, role='assistant', content=完整回复, gmt_create)
```

---

**前端调用示例:**

```javascript
// api/ai.js
export function streamChat(conversationId, message, agentId, articleIds, fileKeys, signal) {
  const params = new URLSearchParams({ message })
  if (conversationId) params.set('conversationId', conversationId)
  if (agentId) params.set('agentId', agentId)
  if (articleIds?.length) params.set('articleIds', articleIds.join(','))
  if (fileKeys?.length) params.set('fileKeys', fileKeys.join(','))

  return fetch(`http://localhost:8080/ai/chat/stream?${params}`, {
    headers: { Authorization: `Bearer ${token}` },
    signal
  })
}

// 消费流
const reader = response.body.getReader()
const decoder = new TextDecoder()
let buffer = ''
while (true) {
  const { done, value } = await reader.read()
  if (done) break
  buffer += decoder.decode(value, { stream: true })
  // 实时更新 UI
  updateMarkdown(buffer)
}
```

---

**架构图:**

```
┌──────────────┐     GET /ai/chat/stream?message=...      ┌──────────────┐
│   Vue 前端    │ ──────────────────────────────────────────→│  Spring Boot │
│              │                                            │              │
│ ReadableStream│←── Flux<String> (raw text, chunk by chunk) │  ChatController│
│ TextDecoder  │                                            │       ↓      │
│ marked.render│                                            │  ChatService │
└──────────────┘                                            │       ↓      │
                                                            │  DeepSeek API│
                                                            └──────────────┘
```

---

### 4.2 会话列表

```
GET /ai/conversations
认证: 是
```

**成功响应:**

```json
{
  "code": 200, "msg": null,
  "data": [
    {
      "id": 1, "title": "Spring Boot 入门问题",
      "agentId": 2, "gmtCreate": "2026-06-03T16:00:00",
      "gmtModified": "2026-06-03T16:05:00"
    }
  ]
}
```

**后端需处理:** 按当前 `userId` 查询，`gmtModified` 降序。

---

### 4.3 创建会话

```
POST /ai/conversations
认证: 是
```

**请求体:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | String | 是 | 会话标题 |
| agentId | Integer | 否 | 关联 Agent |

```json
{ "title": "新对话", "agentId": 2 }
```

**成功响应:**

```json
{
  "code": 200, "msg": null,
  "data": { "id": 1, "title": "新对话", "agentId": 2, "gmtCreate": "...", "gmtModified": "..." }
}
```

---

### 4.4 重命名会话

```
PUT /ai/conversations/{id}
认证: 是
```

**请求体:**

```json
{ "title": "新标题" }
```

**成功响应:**

```json
{ "code": 200, "msg": null, "data": null }
```

**失败响应:**

```json
{ "code": 400, "msg": "无权操作", "data": null }
```

---

### 4.5 删除会话

```
DELETE /ai/conversations/{id}
认证: 是
```

**成功响应:**

```json
{ "code": 200, "msg": null, "data": null }
```

**后端需处理:** 校验所有权后，级联删除 `ai_message` 中该会话的所有消息。

---

### 4.6 获取会话消息

```
GET /ai/conversations/{id}/messages
认证: 是
```

**成功响应:**

```json
{
  "code": 200, "msg": null,
  "data": [
    { "id": 1, "role": "USER", "content": "你好", "gmtCreate": "2026-06-03T16:00:00" },
    { "id": 2, "role": "ASSISTANT", "content": "你好！有什么可以帮助你的？", "gmtCreate": "2026-06-03T16:00:05" }
  ]
}
```

**后端需处理:** `role` = `USER` / `ASSISTANT`，按 `gmtCreate` 升序。

---

### 4.7 Agent 列表

```
GET /ai/agents
认证: 是
```

**成功响应:**

```json
{
  "code": 200, "msg": null,
  "data": [
    {
      "id": 1, "name": "代码助手",
      "systemPrompt": "你是一名资深 Java 工程师...",
      "icon": "💻", "gmtCreate": "2026-05-20T10:00:00"
    }
  ]
}
```

---

### 4.8 创建 Agent

```
POST /ai/agents
认证: 是
```

**请求体:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 名称 |
| systemPrompt | String | 是 | 系统指令 |
| icon | String | 否 | emoji 图标 |

```json
{ "name": "翻译官", "systemPrompt": "你是专业翻译，中英互译，准确地道", "icon": "🌐" }
```

**成功响应:**

```json
{
  "code": 200, "msg": null,
  "data": { "id": 2, "name": "翻译官", "systemPrompt": "...", "icon": "🌐", "gmtCreate": "..." }
}
```

---

### 4.9 删除 Agent

```
DELETE /ai/agents/{id}
认证: 是
```

**成功响应:**

```json
{ "code": 200, "msg": null, "data": null }
```

---

## 五、RAG 知识库模块

> 基于阿里 DashScope Embedding 实现检索增强生成。
> 用户上传文件或提交文章 → 后端解析文本 → 分块向量化 → 存入向量库 + R2 + DB → 聊天时自动检索。

---

### 5.1 上传文件到知识库

```
POST /ai/rag/upload
认证: 是
Content-Type: multipart/form-data
```

**请求体（multipart）:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | File | 是 | PDF/Word/TXT 文件 |
| knowledgeBaseId | Integer | 是 | 目标知识库 ID |
| title | String | 否 | 文档标题，不传则用文件名 |

**成功响应:**

```json
{
  "code": 200, "msg": null,
  "data": {
    "id": 1,
    "title": "Spring Boot 实战.pdf",
    "fileType": "PDF",
    "r2Key": "knowledge/1_1684512000000_Spring Boot 实战.pdf",
    "status": "READY",
    "gmtCreate": "2026-06-05T16:00:00"
  }
}
```

**后端需处理:**
1. 文件上传到 R2（存储原始文件）
2. 解析文本（PDF→PDFBox, Word→POI, TXT→直接读）
3. 文本分块（每块 500-1000 tokens，重叠 100 tokens）
4. 调用 DashScope Embedding 将每块转为向量
5. 向量写入向量数据库
6. 写入 `knowledge_document` 表（`status=READY`）

---

### 5.2 提交文章到知识库

```
POST /ai/rag/articles
认证: 是
```

**请求体:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| articleIds | List\<Integer\> | 是 | 文章 ID 列表 |
| knowledgeBaseId | Integer | 是 | 目标知识库 ID |

```json
{ "articleIds": [1, 2, 3], "knowledgeBaseId": 1 }
```

**成功响应:**

```json
{
  "code": 200, "msg": null,
  "data": [
    { "id": 2, "title": "Spring Boot 入门", "status": "READY" }
  ]
}
```

**后端需处理:**
1. 根据 `articleIds` 查 `article` 表获取标题和正文
2. 将 Markdown 正文转为同 5.1 的分块→向量化流程
3. 同时将文章内容转存为 `.md` 文件上传到 R2
4. 写入 `knowledge_document` 表

---

### 5.3 知识库管理

#### 5.3.1 创建知识库

```
POST /ai/knowledge-bases
认证: 是
```

**请求体:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 知识库名称，1-50 字符 |
| description | String | 否 | 描述 |

```json
{ "name": "我的知识库", "description": "Java 学习笔记" }
```

**成功响应:**

```json
{
  "code": 200, "msg": null,
  "data": { "id": 1, "name": "我的知识库", "description": "...", "docCount": 0, "gmtCreate": "..." }
}
```

---

#### 5.3.2 获取知识库列表

```
GET /ai/knowledge-bases
认证: 是
```

**成功响应:**

```json
{
  "code": 200, "msg": null,
  "data": [
    { "id": 1, "name": "我的知识库", "description": "...", "docCount": 3, "gmtCreate": "..." }
  ]
}
```

**后端需处理:** 按当前 `userId` 查询，返回该用户所有知识库。

---

#### 5.3.3 删除知识库

```
DELETE /ai/knowledge-bases/{id}
认证: 是
```

**成功响应:**

```json
{ "code": 200, "msg": null, "data": null }
```

**后端需处理:**
1. 校验所有权
2. 级联删除向量库中该库所有向量
3. 级联删除 R2 中该库所有源文件
4. 级联删除 `knowledge_document` 表中该库所有记录

---

### 5.4 知识库文档列表

```
GET /ai/knowledge-bases/{id}/documents?page=1&size=20
认证: 是
```

**成功响应:**

```json
{
  "code": 200, "msg": null,
  "data": {
    "total": 5, "page": 1, "size": 20,
    "documents": [
      {
        "id": 1, "title": "Spring Boot 实战.pdf",
        "fileType": "PDF",
        "status": "READY",
        "gmtCreate": "2026-06-05T16:00:00"
      }
    ]
  }
}
```

### 5.5 删除单个文档

```
DELETE /ai/knowledge-bases/{id}/documents/{docId}
认证: 是
```

**后端需处理:**
1. 校验所有权
2. 从向量库删除该文档所有向量
3. 从 R2 删除源文件
4. 从 `knowledge_document` 表删除记录

---

### 5.6 数据模型

**knowledge_base**

| 列 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| user_id | BIGINT | 所属用户 |
| name | VARCHAR(50) | 知识库名称 |
| description | VARCHAR(200) | 描述 |
| gmt_create | DATETIME | |

**knowledge_document**

| 列 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| knowledge_base_id | BIGINT FK | |
| title | VARCHAR(100) | 文档标题 |
| file_type | VARCHAR(10) | PDF/WORD/TXT/MD |
| r2_key | VARCHAR(255) | R2 存储 key |
| status | VARCHAR(16) | `PROCESSING` 处理中 / `READY` 就绪 / `ERROR` 失败 |
| gmt_create | DATETIME | |

> **Embedding:** 阿里 DashScope `text-embedding-v2`（1536 维），Cosine 距离

---

### 5.7 聊天集成流程

```
用户发送消息
    → ChatController 接收 knowledgeBaseIds
    → 校验 knowledgeBaseIds 对应的 knowledge_base.user_id == 当前用户
    → 对 message 做 DashScope Embedding
    → 在向量库中检索与 embedding 最相似的 Top-K 文本块
    → 拼入 system prompt: "参考以下资料回答：\n{chunks}\n\n用户问题：{message}"
    → 调用 DeepSeek 流式生成
```

**注：** 聊天接口（4.1）无需前端传文章 ID 或文件 key，仅传 `knowledgeBaseIds` 即可。

---

## 六、附录

### 6.1 认证总表

| 接口 | 认证 |
|------|------|
| POST /user/register | 否 |
| POST /user/login | 否 |
| GET /user/info | 是 |
| PUT /user/info | 是 |
| PUT /user/avatar | 是 |
| PUT /user/password | 是 |
| POST /file/upload/url | 是 |
| GET /file/download/url | 否 |
| POST /article/add | 是 |
| PUT /article/{id} | 是 |
| DELETE /article/{id} | 是 |
| GET /article/list | 否 |
| GET /article/{id} | 否 |
| GET /article/my | 是 |
| GET /article/{id}/versions | 是 |
| GET /article/{id}/versions/{versionId} | 是 |
| POST /article/{id}/versions/{versionId}/rollback | 是 |
| 全部 /ai/* | 是 |

### 6.2 拦截器放行规则

`JwtInterceptor` 已拦截 `/**`，以下自动放行:

- `POST /user/register`、`POST /user/login`
- `GET /article/list`、`GET /article/{id}`
- `GET /file/download/url`
- Knife4j 文档路径
- `OPTIONS` 预检请求

### 6.3 技术决策记录

- **密码**: 当前 MD5，后续迁移 BCrypt
- **JWT**: jjwt 0.12.6，payload 含 `userId` 和 `name`
- **分页**: PageHelper 6.1.0 + MyBatis-Plus
- **文件**: 直传 Cloudflare R2（预签名 URL）
- **AI**: Spring AI + DeepSeek，流式 SSE
- **RAG**: 阿里 DashScope text-embedding-v2 Embedding