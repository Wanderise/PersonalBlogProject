# 后端 API 接口文档

> 基础路径: `http://localhost:8080`
> 响应格式: `{ "code": 200, "msg": null, "data": ... }`
> code=200 成功；业务异常返回对应 HTTP 状态码（401/403/404/500），响应体 `code` 字段随异常变化

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
| name | String | 是 | 用户名 |
| password | String | 是 | 密码 |

```json
{ "name": "zhangsan", "password": "123456" }
```

**成功响应:**

```json
{ "code": 200, "msg": null, "data": null }
```

**失败响应:**

```json
{ "code": 404, "msg": "not found", "data": null }
```

**后端处理:**
1. 密码使用 BCrypt 加密后存储（内置随机盐，不可逆）
2. `level` 默认 0，`gmtCreate` 和 `gmtModified` 设为当前时间
3. 未做用户名唯一性校验，数据库约束负责

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
    "user": { "id": 1, "name": "zhangsan", "image": null, "level": 0, "gmtCreate": "2026-05-15T12:00:00" }
  }
}
```

**失败响应:**

```json
{ "code": 404, "msg": "not found", "data": null }
```

**后端处理:**
1. 根据 `name` 查询用户
2. 使用 `BCryptPasswordEncoder.matches()` 比对新密码与数据库中 BCrypt 密文
3. 生成 JWT Token（payload 含 `UserName` 和 `userId`，有效期 10 天，HS256 签名）
4. 用户不存在返回 UserCountNotExist(404)，密码错误返回 WrongPassword(404)

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

**后端处理:** 从 JWT 解析 `userId`，查询用户，密码字段不返回。

---

### 1.4 更新用户信息（昵称）

```
PUT /user/info
认证: 是
```

**请求体:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 新用户名 |

```json
{ "name": "新用户名" }
```

**成功响应:**

```json
{
  "code": 200, "msg": null,
  "data": {
    "id": 1, "name": "新用户名",
    "image": null, "level": 0, "gmtCreate": "2026-05-15T12:00:00"
  }
}
```

**失败响应:**

```json
{ "code": 400, "msg": "name exist", "data": null }
```

**后端处理:**
1. 从 `UserContext`（ThreadLocal）获取当前用户名
2. 校验新 `name` 未被他人占用
3. 仅更新 `blog.user` 的 `name` 和 `gmtModified`，不更新其他字段

---

### 1.5 更新用户头像

```
PUT /user/avatar
认证: 是
```

**请求体:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| objectKey | String | 是 | R2 存储的 key |

```json
{ "objectKey": "avatars/1_1684512000000.jpg" }
```

**成功响应:**

```json
{ "code": 200, "msg": null, "data": null }
```

**后端处理:**
1. 先从 `UserContext` 获取当前用户
2. 先更新 DB 中 `image` 字段，再删除 R2 上的旧头像（防止 DB 写失败时旧文件已丢失）
3. `image` 存储 objectKey（非完整 URL），前端通过 `/file/download/url` 获取预签名 URL 显示

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
  "data": {
    "uploadUrl": "https://<r2-presigned-put-url>",
    "publicUrl": "https://<public-domain>/images/xxx.jpg"
  }
}
```

**后端处理:**
1. 生成 10 分钟内有效的 R2 预签名上传 URL
2. `publicUrl` 由 `r2.public-domain` 配置拼接，未配置或为 `http://example.com` 时返回 null
3. objectKey 未经校验，由调用方保证合法性

---

### 2.2 获取下载预签名 URL

```
GET /file/download/url?objectKey=images/xxx.jpg
认证: 是
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

**后端处理:** 生成 1 小时内有效的 R2 预签名下载 URL。

---

**文件上传流程（前端）:**

```
选择文件
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
| title | String | 是 | 文章标题 |
| content | String | 是 | Markdown 正文 |
| tag | List\<String\> | 否 | 标签列表 |
| image | List\<String\> | 否 | 配图 objectKey 数组 |
| version | Double | 否 | 版本号，不传则默认 1.0 |

```json
{
  "title": "Spring Boot 入门",
  "content": "# 第一章\n...",
  "tag": ["Java", "Spring"],
  "version": 1.0,
  "image": ["covers/1_xxx.jpg"]
}
```

**成功响应:**

```json
{ "code": 200, "msg": null, "data": { "id": 1 } }
```

**后端处理:**
1. `writerId` 从 JWT 获取，防止客户端伪造
2. `tag` 写入 `article_tag` 关联表和 `tag` 表
3. `version` 默认 1.0
4. `image` 字段以 JSON 数组字符串存储（如 `["covers/1_xxx.jpg"]`）
5. 创建时将当前内容也写入 `article_version` 归档

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
  "version": 1.1,
  "image": ["covers/1_xxx.jpg"]
}
```

**成功响应:**

```json
{ "code": 200, "msg": null, "data": null }
```

**失败响应:**

```json
{ "code": 403, "msg": "forbidden", "data": null }
```

**后端处理:**
1. 校验当前用户是否为作者
2. 先将当前文章内容和标签归档到 `article_version` 表
3. 若请求体未传 `version`，则在当前版本上 +0.1（使用 BigDecimal 避免浮点精度误差）
4. 更新 `article` 表，重建 `article_tag` 关联，清理孤立标签
5. 更新 `gmtModified`

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

**后端处理:**
1. 用 `baseMapper.selectById` 轻量查询（避免生成预签名 URL）
2. 校验作者身份
3. 删除 `article` 记录、`article_tag` 关联、孤立标签
4. 遍历 `image` JSON 数组，逐一删除 R2 文件
5. 删除所有版本历史记录

---

### 3.4 获取文章详情

```
GET /article/{id}
认证: 是
```

**成功响应:**

```json
{
  "code": 200, "msg": null,
  "data": {
    "id": 1,
    "title": "Spring Boot 入门",
    "content": "# 第一章\n...",
    "summary": "Spring Boot 入门的第一章内容...",
    "tag": ["Java", "Spring"],
    "image": ["covers/1_xxx.jpg"],
    "imageUrls": ["https://<r2-presigned-url>"],
    "version": 1.1,
    "writerId": 1,
    "writerName": "zhangsan",
    "gmtCreate": "2026-05-15T12:00:00",
    "gmtModified": "2026-05-16T08:30:00"
  }
}
```

**后端处理:**
1. JOIN `user` 表取 `writerName`
2. `tag` 从 `article_tag` 关联表查询
3. `image` JSON 反序列化后，为每个 key 生成预签名 URL 填入 `imageUrls`
4. `summary` 为 content 前 200 字符

---

### 3.5 文章列表（公开）

```
GET /article/list?page=1&size=12&keyword=Spring&tag=Java
认证: 是
```

**请求参数:**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 是 | 页码，无默认值（客户端必须传） |
| size | int | 是 | 每页条数 |
| keyword | String | 否 | 标题/内容模糊搜索 |
| tag | String | 否 | 按标签名筛选 |

**成功响应:**

```json
{
  "code": 200, "msg": null,
  "data": {
    "total": 25, "page": 1, "size": 12,
    "articles": [
      {
        "id": 1, "title": "Spring Boot 入门",
        "summary": "第一章...",
        "tag": ["Java", "Spring"],
        "image": ["covers/1_xxx.jpg"],
        "imageUrls": ["https://<r2-presigned-url>"],
        "version": 1.1,
        "writerId": 1, "writerName": "zhangsan",
        "gmtCreated": "2026-05-15T12:00:00",
        "gmtModified": "2026-05-16T08:30:00"
      }
    ]
  }
}
```

**后端处理:**
1. PageHelper 分页
2. `keyword` → LIKE `%keyword%`（参数化查询，安全），`tag` → EXISTS 子查询
3. 列表模式仅生成第一张图的预签名 URL 作为封面，不生成全部图片 URL
4. 按 `gmtModified` 降序

---

### 3.6 我的文章列表

```
GET /article/my?page=1&size=12
认证: 是
```

**请求参数:**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 是 | 页码 |
| size | int | 是 | 每页条数 |

**成功响应:** 结构同 3.5

**后端处理:** 从 token 获取 `userId`，只返回该用户的文章。

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
    {
      "id": 3, "articleId": 1, "version": 1.2,
      "title": "Spring Boot 入门（最新版）",
      "content": "# 第一章\n...",
      "tag": "[\"Java\",\"Spring\"]",
      "gmtCreate": "2026-06-05"
    },
    {
      "id": 2, "articleId": 1, "version": 1.1,
      "title": "Spring Boot 入门（修订版）",
      "content": "# 第一章\n...",
      "tag": "[\"Java\",\"Spring Boot\"]",
      "gmtCreate": "2026-06-04"
    }
  ]
}
```

**后端处理:** 校验当前用户是否为作者，按 `version` 降序返回。`tag` 字段为 JSON 数组字符串。

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
    "id": 3, "articleId": 1, "version": 1.0,
    "title": "Spring Boot 入门",
    "content": "# 第一章\n...",
    "tag": "[\"Java\",\"Spring\"]",
    "gmtCreate": "2026-06-03"
  }
}
```

**后端处理:**
1. 校验当前用户是否为作者
2. 用目标版本的内容直接覆盖 `article` 表（`title`、`content`、`version`）
3. 重建 tag 关联，清理孤立标签
4. 更新 `gmtModified`
5. 当前内容不回滚前不归档

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
    "id": 2, "articleId": 1, "version": 1.1,
    "title": "Spring Boot 入门（修订版）",
    "content": "# 第一章\n...",
    "tag": "[\"Java\",\"Spring Boot\"]",
    "gmtCreate": "2026-06-04"
  }
}
```

**后端处理:** 校验当前用户是否为作者，从 `article_version` 表查询对应版本返回。

---

## 四、AI 聊天模块

### 4.1 文本生成（非流式）

```
GET /ai/generate?message=你好
认证: 是
```

**请求参数:**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| message | String | 否 | 默认 "Tell me a joke" |

**成功响应:**

```json
{
  "code": 200, "msg": null,
  "data": { "generation": "你好！有什么可以帮助你的？" }
}
```

---

### 4.2 流式对话

```
GET /ai/chat/stream?message=你好&conversationId=1&agentId=2
认证: 是
```

**请求参数（Query String）:**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| message | String | 是 | 用户消息文本 |
| conversationId | Integer | 否 | 会话 ID |
| agentId | Integer | 否 | Agent ID 匹配自定义 system prompt |

**响应格式:**

- `Content-Type: text/plain`（原始文本流，非 SSE）
- 返回 `Flux<String>` 响应式流，AI 逐 token 输出
- 前端通过 `response.body.getReader()` 读取字节流，`TextDecoder` 解码后实时渲染

**流中示例（raw bytes）:**

```
你好！有什么可以帮助你的？根据你提供的文章...
```

**后端处理流程:**

```
1. 接收参数
     ├─ agentId != null → 查 ai_agent 表取 systemPrompt
     └─ 未匹配 → 使用默认 prompt: "你是一个有帮助的AI助手"

2. 查 ai_message 表取该会话历史消息

3. 保存用户消息到 DB（role=user）

4. 调用 DeepSeek API 流式生成

5. 流结束后保存 AI 回复到 DB（role=assistant）
```

**前端调用示例:**

```javascript
export function streamChat(conversationId, message, agentId, signal) {
  const params = new URLSearchParams({ message })
  if (conversationId) params.set('conversationId', conversationId)
  if (agentId) params.set('agentId', agentId)
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
  updateMarkdown(buffer)
}
```

---

### 4.3 会话列表

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
      "agentId": 2,
      "gmtCreate": "2026-06-03T16:00:00",
      "gmtModified": "2026-06-03T16:05:00"
    }
  ]
}
```

**后端处理:** 按当前 `userId` 查询，`gmtModified` 降序。

---

### 4.4 创建会话

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

### 4.5 重命名会话

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
{ "code": 403, "msg": "forbidden", "data": null }
```

**后端处理:** 校验所有权（conversation.userId == 当前用户）。

---

### 4.6 删除会话

```
DELETE /ai/conversations/{id}
认证: 是
```

**成功响应:**

```json
{ "code": 200, "msg": null, "data": null }
```

**后端处理:** 校验所有权后，级联删除该会话在 `ai_message` 表中的所有消息。

---

### 4.7 获取会话消息

```
GET /ai/conversations/{id}/messages
认证: 是
```

**成功响应:**

```json
{
  "code": 200, "msg": null,
  "data": [
    { "id": 1, "role": "user", "content": "你好", "gmtCreate": "2026-06-03T16:00:00" },
    { "id": 2, "role": "assistant", "content": "你好！有什么可以帮助你的？", "gmtCreate": "2026-06-03T16:00:05" }
  ]
}
```

**后端处理:** `role` 为小写 `user` / `assistant`，按 `gmtCreate` 升序。

---

### 4.8 Agent 列表

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

### 4.9 创建 Agent

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
{ "name": "翻译官", "systemPrompt": "你是专业翻译，中英互译", "icon": "🌐" }
```

**成功响应:**

```json
{
  "code": 200, "msg": null,
  "data": { "id": 2, "name": "翻译官", "systemPrompt": "...", "icon": "🌐", "gmtCreate": "..." }
}
```

---

### 4.10 删除 Agent

```
DELETE /ai/agents/{id}
认证: 是
```

**成功响应:**

```json
{ "code": 200, "msg": null, "data": null }
```

**后端处理:** 先校验所有权（agent.userId == 当前用户），再删除。

---

## 五、RAG 知识库模块

### 5.1 上传文件到知识库

```
POST /ai/rag/upload
认证: 是
Content-Type: multipart/form-data
```

**请求体（multipart）:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| files | List\<MultipartFile\> | 是 | PDF(docx/txt) 文件 |
| knowledgeBaseId | Integer | 是 | 目标知识库 ID |

**后端处理:**
1. 校验 knowledgeBaseId 所有权
2. 计算文件 SHA-256，相同 hash 已存在则跳过（去重）
3. 文件名包含 `/` 或 `\` 时替换为 `_` 防止路径穿越
4. 文件上传到 R2
5. 解析文本（PDF→PagePdfDocumentReader，Word→POI XWPF，TXT→Apache Tika）
6. TokenTextSplitter 分块
7. 分块向量化写入 Qdrant（元数据: document_id, kb_id, version）
8. 写入 `rag_file` 表
9. 单文件失败不影响其他文件

**成功响应:**

```json
{
  "code": 200, "msg": null,
  "data": [
    {
      "id": 1, "title": "Spring Boot 实战.pdf",
      "fileType": "application/pdf",
      "r2Key": "knowledge/Spring Boot 实战.pdf",
      "status": "READY",
      "gmtCreate": "2026-06-05"
    }
  ]
}
```

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

**后端处理:**
1. 根据 `articleIds` 查 `article` 表获取标题和正文
2. 文章不存在时跳过并记录日志
3. SHA-256 内容去重，相同内容不重复上传
4. 将 Markdown 内容分块向量化写入 Qdrant
5. 同时将内容作为 `.md` 文件上传到 R2
6. 写入 `rag_file` 表

**成功响应:**

```json
{
  "code": 200, "msg": null,
  "data": [
    { "id": 2, "title": "Spring Boot 入门", "fileType": "md", "status": "READY", "gmtCreate": "2026-06-05" }
  ]
}
```

---

### 5.3 知识库 CRUD

#### 5.3.1 创建知识库

```
POST /ai/knowledge-bases
认证: 是
```

**请求体:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 知识库名称 |
| description | String | 否 | 描述 |

```json
{ "name": "我的知识库", "description": "Java 学习笔记" }
```

**成功响应:**

```json
{
  "code": 200, "msg": null,
  "data": { "id": 1, "name": "我的知识库", "description": "...", "gmtCreate": "..." }
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

**后端处理:** `docCount` 统计 `rag_file` 表中该知识库下的文档数量。

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

**后端处理:**
1. 校验所有权
2. 在事务中先删 `rag_file` 关联记录，再删 `knowledge_base` 记录
3. 不级联删除 R2 文件和 Qdrant 向量（TODO）

---

#### 5.3.4 知识库文档列表

```
GET /ai/knowledge-bases/{id}/documents
认证: 是
```

**成功响应:**

```json
{
  "code": 200, "msg": null,
  "data": [
    {
      "id": 1, "title": "Spring Boot 实战.pdf",
      "fileType": "pdf",
      "r2Key": "knowledge/xxx.pdf",
      "status": "READY",
      "gmtCreate": "2026-06-05"
    }
  ]
}
```

**后端处理:** 校验知识库所有权，查询 `rag_file` 表按 `gmtCreate` 降序返回。

---

#### 5.3.5 删除单个文档

```
DELETE /ai/knowledge-bases/{id}/documents/{docId}
认证: 是
```

**成功响应:**

```json
{ "code": 200, "msg": null, "data": null }
```

**后端处理:**
1. 校验知识库所有权
2. 校验文档属于该知识库
3. 从 `rag_file` 表删除记录
4. 不级联删除 R2 文件和 Qdrant 向量（TODO）

---

### 5.4 数据模型

**knowledge_base**

| 列 | 类型 | 说明 |
|------|------|------|
| id | INT PK AUTO_INCREMENT | |
| user_id | INT | 所属用户 |
| name | VARCHAR | 知识库名称 |
| description | VARCHAR | 描述 |
| gmt_create | DATETIME | |

**rag_file**

| 列 | 类型 | 说明 |
|------|------|------|
| id | INT PK AUTO_INCREMENT | |
| knowledge_base_id | INT | 关联 knowledge_base.id |
| title | VARCHAR | 文档标题/文件名 |
| file_type | VARCHAR | MIME 类型或 "md" |
| r2_key | VARCHAR | R2 存储 key |
| status | VARCHAR | "READY" |
| hash | VARCHAR(64) | SHA-256，用于去重 |
| version | DOUBLE | 版本号 |
| gmt_create | DATE | |

**ai_message**

| 列 | 类型 | 说明 |
|------|------|------|
| id | INT PK AUTO_INCREMENT | |
| conversation_id | INT | 关联 ai_conversation.id |
| role | VARCHAR | user / assistant |
| content | TEXT | 消息正文 |
| gmt_create | DATETIME | |

**ai_conversation**

| 列 | 类型 | 说明 |
|------|------|------|
| id | INT PK AUTO_INCREMENT | |
| user_id | INT | 所属用户 |
| title | VARCHAR | 会话标题 |
| agent_id | INT | 关联 ai_agent.id（可为空） |
| gmt_create | DATETIME | |
| gmt_modified | DATETIME | |

**ai_agent**

| 列 | 类型 | 说明 |
|------|------|------|
| id | INT PK AUTO_INCREMENT | |
| user_id | INT | 所属用户 |
| name | VARCHAR | 名称 |
| system_prompt | TEXT | 系统指令 |
| icon | VARCHAR | emoji 图标 |
| gmt_create | DATETIME | |

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
| POST /file/upload/url | 是 |
| GET /file/download/url | 是 |
| POST /article/add | 是 |
| PUT /article/{id} | 是 |
| DELETE /article/{id} | 是 |
| GET /article/list | 是 |
| GET /article/{id} | 是 |
| GET /article/my | 是 |
| GET /article/{id}/versions | 是 |
| GET /article/{id}/versions/{versionId} | 是 |
| POST /article/{id}/versions/{versionId}/rollback | 是 |
| 全部 /ai/* | 是 |

### 6.2 拦截器放行规则

`JwtInterceptor` 拦截 `/**`，以下路径放行（无需认证）:

- `POST /user/login`、`POST /user/register`
- `/doc.html`、`/v3/api-docs/**`、`/webjars/**`、`/swagger-ui/**`
- `OPTIONS` 预检请求（CORS）

### 6.3 响应格式

- **成功**: HTTP 200，`{"code": 200, "msg": null, "data": ...}`
- **业务异常**: HTTP 状态码由 BaseException 子类决定（401/403/404），`{"code": <对应HTTP状态码>, "msg": "<异常消息>", "data": null}`
- **未知异常**: HTTP 500，`{"code": 400, "msg": "服务器内部错误", "data": null}`

### 6.4 技术栈

- **密码**: BCrypt（通过 spring-security-crypto）
- **JWT**: jjwt 0.12.6，HS256，payload 含 `UserName` 和 `userId`，有效期 10 天
- **分页**: PageHelper 6.1.0 + MyBatis-Plus 3.5.15
- **文件存储**: Cloudflare R2（预签名 URL 直传，S3 兼容 SDK aws-sdk-java 2.20.0）
- **AI**: Spring AI + DeepSeek（deepseek-v4-flash），流式 Flux<String>
- **RAG Embedding**: 阿里 DashScope text-embedding-v4
- **向量数据库**: Qdrant

---

## 七、计划实现

> 以下接口/功能已纳入开发计划，尚未实现。

---

### 7.1 修改密码

```
[计划] PUT /user/password
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
2. `BCryptPasswordEncoder.matches()` 比对 `oldPassword`，不匹配返回 WrongPassword
3. BCrypt 加密 `newPassword` 写入 DB，更新 `gmtModified`

---

### 7.2 文章列表/详情公开访问

```
[计划] GET  /article/list     → 认证: 否
[计划] GET  /article/{id}     → 认证: 否
```

当前 `JwtInterceptor` 未放行这两个路径，均需认证。计划在 `InterceptorConfig.excludePathPatterns` 中追加 `/article/list`、`/article/{id:[0-9]+}`，改为公开。

---

### 7.3 流式对话 RAG 集成

```
[计划] GET /ai/chat/stream
认证: 是
```

**新增请求参数:**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| message | String | 是 | 用户消息文本 |
| conversationId | Integer | 否 | 会话 ID（不传则自动创建） |
| agentId | Integer | 否 | Agent ID |
| knowledgeBaseIds | String | 否 | 逗号分隔的知识库 ID，如 `1,2` |
| articleIds | String | 否 | 逗号分隔的文章 ID，作为上下文 |
| fileKeys | String | 否 | 逗号分隔的 R2 key，下载解析文本 |

**计划处理流程:**

```
1. 校验 knowledgeBaseIds 对应的 knowledge_base.user_id == 当前用户
2. 对 message 做 DashScope Embedding（text-embedding-v4，1536 维）
3. 在 Qdrant 中检索 Top-K 相似向量（Cosine 距离，filter: kb_id IN knowledgeBaseIds）
4. 根据命中向量元数据（document_id）回查 rag_file 获取来源信息
5. 将检索文本块拼入 system prompt:
   "参考以下资料回答用户问题：\n---\n{chunk1}\n---\n{chunk2}\n---\n\n用户问题：{message}"
6. articleIds → 查 article 表取正文，追加到上下文
7. fileKeys → 从 R2 下载文件，解析文本（PDF/Word/TXT），追加到上下文
8. 调用 DeepSeek 流式生成
```

---

### 7.4 删除知识库完善

```
[计划] DELETE /ai/knowledge-bases/{id}
```

当前仅删除 DB 中 `rag_file` 和 `knowledge_base` 记录。计划补充级联：

1. 从 Qdrant 批量删除该知识库所有向量（按 `metadata.kb_id` 过滤）
2. 遍历 `rag_file` 表中该库的所有 `r2_key`，逐一删除 R2 文件
3. 以上操作在事务中执行，失败时回滚

---

### 7.5 自动创建会话

当前流式对话要求客户端先 `POST /ai/conversations` 显式创建会话。计划改为：

- `conversationId` 不传或为 null 时，`ChatServiceImpl` 自动创建 `ai_conversation`
- `title` 取 message 前 30 字符（超过截断加 `...`）
- 会话创建后，将 `conversationId` 附加到首次流式响应事件中通知前端

---

### 7.8 注册用户名去重

```
[计划] POST /user/register
```

当前注册未校验用户名唯一性，依赖 DB 唯一约束抛异常。计划在 `UserService.register()` 中先查询 `blog.user`，若 `name` 已存在则返回明确错误：

```json
{ "code": 400, "msg": "用户名已存在", "data": null }
```
