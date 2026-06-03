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

**后端需处理:**
1. 校验当前用户是否为作者
2. 更新 `article` 表 + 重建 `article_tag` 关联
3. 更新 `gmtModified`

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

## 四、AI 聊天模块

> 前端路由: `/ai`。存储由后端负责，前端仅做 UI 渲染。

---

### 4.1 流式对话

```
POST /ai/chat/stream?conversationId=1&agentId=2&message=你好
认证: 是
```

**请求参数:**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| message | String | 是 | 用户消息 |
| conversationId | Long | 否 | 会话 ID（新对话不传，后端自动创建） |
| agentId | Long | 否 | Agent ID |

**响应格式:**`text/event-stream`（SSE），`ReadableStream` 逐块读取渲染 Markdown。

**后端需处理:**
1. 无 `conversationId` → 创建新会话
2. 查询最近 10 轮历史 + Agent 的 `systemPrompt` → 拼接上下文
3. 调用 DeepSeek API 流式生成
4. 流结束后 user + assistant 消息写入 `ai_message` 表

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
| agentId | Long | 否 | 关联 Agent |

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

## 五、附录

### 5.1 认证总表

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
| 全部 /ai/* | 是 |

### 5.2 拦截器放行规则

`JwtInterceptor` 已拦截 `/**`，以下自动放行:

- `POST /user/register`、`POST /user/login`
- `GET /article/list`、`GET /article/{id}`
- `GET /file/download/url`
- Knife4j 文档路径
- `OPTIONS` 预检请求

### 5.3 数据库设计建议

**ai_conversation**

| 列 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| user_id | BIGINT | |
| title | VARCHAR(200) | |
| agent_id | BIGINT | |
| gmt_create | DATETIME | |
| gmt_modified | DATETIME | |

**ai_message**

| 列 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| conversation_id | BIGINT FK | |
| role | VARCHAR(20) | USER / ASSISTANT |
| content | TEXT | |
| gmt_create | DATETIME | |

**ai_agent**

| 列 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| user_id | BIGINT | |
| name | VARCHAR(50) | |
| system_prompt | TEXT | |
| icon | VARCHAR(10) | emoji |
| gmt_create | DATETIME | |

### 5.4 技术决策记录

- **密码**: 当前 MD5，后续迁移 BCrypt
- **JWT**: jjwt 0.12.6，payload 含 `userId` 和 `name`
- **分页**: PageHelper 6.1.0 + MyBatis-Plus
- **文件**: 直传 Cloudflare R2（预签名 URL）
- **AI**: Spring AI + DeepSeek，流式 SSE