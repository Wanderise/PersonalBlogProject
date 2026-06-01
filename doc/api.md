# 后端 API 接口文档

> 基础路径: `http://localhost:8080`
> 统一响应格式: `Result<E>` (code=200 成功, code=400 失败)

---

## 一、用户认证模块

### 1. 用户注册

**POST** `/user/register`

**请求体** (JSON):

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 用户名，2-20 字符 |
| password | String | 是 | 密码，6-30 字符 |

```json
{
  "name": "zhangsan",
  "password": "123456"
}
```

**成功响应** (code=200):

```json
{
  "code": 200,
  "msg": null,
  "data": null
}
```

**失败响应** (code=400):

```json
{
  "code": 400,
  "msg": "用户名已存在",
  "data": null
}
```

**后端需处理**:
1. 校验 `name` 是否已存在，存在则返回 "用户名已存在"
2. 对 `password` 进行 MD5 加密后存储
3. 设置 `gmtCreate` 为当前时间，`level` 默认为 0（普通用户）
4. 返回成功即可，无需返回 token

---

### 2. 用户登录

**POST** `/user/login`

**请求体** (JSON):

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 用户名 |
| password | String | 是 | 密码 |

```json
{
  "name": "zhangsan",
  "password": "123456"
}
```

**成功响应** (code=200):

```json
{
  "code": 200,
  "msg": null,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "user": {
      "id": 1,
      "name": "zhangsan",
      "image": null,
      "level": 0
    }
  }
}
```

**失败响应** (code=400):

```json
{
  "code": 400,
  "msg": "用户名或密码错误",
  "data": null
}
```

**后端需处理**:
1. 根据 `name` 查询用户
2. 使用 MD5 比对密码
3. 生成 JWT Token，payload 中包含 `userId` 和 `name`，有效期建议 7 天
4. 返回 token 和用户基本信息

---

### 3. 获取当前用户信息

**GET** `/user/info`

**请求头**:

| 字段 | 值 |
|------|------|
| Authorization | Bearer {token} |

无需请求体。

**成功响应** (code=200):

```json
{
  "code": 200,
  "msg": null,
  "data": {
    "id": 1,
    "name": "zhangsan",
    "image": null,
    "level": 0,
    "gmtCreate": "2026-05-15T12:00:00"
  }
}
```

**失败响应** (code=400):

```json
{
  "code": 400,
  "msg": "未登录或 token 已过期",
  "data": null
}
```

**后端需处理**:
1. 从 Authorization 头中提取 token 并校验
2. 根据 token 中的 userId 查询用户
3. 返回用户信息（不包含密码）

---

### 4. 更新用户信息

**PUT** `/user/info`

**请求头**:

| 字段 | 值 |
|------|------|
| Authorization | Bearer {token} |

**请求体** (JSON):

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 新用户名，2-20 字符 |

```json
{
  "name": "新用户名"
}
```

**成功响应** (code=200):

```json
{
  "code": 200,
  "msg": null,
  "data": {
    "id": 1,
    "name": "新用户名",
    "image": "avatars/1_1684512000000.jpg",
    "level": 0,
    "gmtCreate": "2026-05-15T12:00:00"
  }
}
```

**失败响应** (code=400):

```json
{
  "code": 400,
  "msg": "用户名已存在",
  "data": null
}
```

**后端需处理**:
1. 从 `UserContext.getUser()` 获取当前登录用户名
2. 校验新 `name` 是否已被其他用户占用
3. 执行 `UPDATE blog.user SET name = #{name}, gmt_modified = NOW() WHERE name = #{currentUserName}`
4. 返回更新后的 `UserVO`

---

### 5. 更新用户头像

**PUT** `/user/avatar`

**请求头**:

| 字段 | 值 |
|------|------|
| Authorization | Bearer {token} |

**请求体** (JSON):

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| objectKey | String | 是 | R2 存储的对象 key，如 `avatars/1_1684512000000.jpg` |

```json
{
  "objectKey": "avatars/1_1684512000000.jpg"
}
```

**成功响应** (code=200):

```json
{
  "code": 200,
  "msg": null,
  "data": null
}
```

**后端需处理**:
1. 从 `UserContext.getUser()` 获取当前登录用户名
2. 将 `objectKey` 存入 `blog.user` 表的 `image` 字段

> **说明**：`image` 字段存储 `objectKey`（非完整 URL）。前端通过 `GET /file/download/url?objectKey=xxx` 获取预签名下载 URL 再显示图片，不依赖自定义域名。

---

### 6. 更新用户密码

**PUT** `/user/password`

**请求头**:

| 字段 | 值 |
|------|------|
| Authorization | Bearer {token} |

**请求体** (JSON):

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| oldPassword | String | 是 | 当前密码 |
| newPassword | String | 是 | 新密码，6-30 字符 |

```json
{
  "oldPassword": "123456",
  "newPassword": "654321"
}
```

**成功响应** (code=200):

```json
{
  "code": 200,
  "msg": null,
  "data": null
}
```

**失败响应** (code=400):

```json
{
  "code": 400,
  "msg": "当前密码错误",
  "data": null
}
```

**后端需处理**:
1. 从 `UserContext.getUser()` 获取当前登录用户名
2. 查询用户记录，比对 `oldPassword` 与数据库中存储的密码（MD5 加密后比对）
3. 校验通过后，将 `newPassword` 经 MD5 加密后更新到 `blog.user` 表
4. 同步更新 `gmt_modified` 字段

---

### 7. 获取预签名下载 URL

**GET** `/file/download/url?objectKey=avatars/1_1684512000000.jpg`

**请求头**:

| 字段 | 值 |
|------|------|
| 无特殊要求 | - |

无需请求体。

**成功响应** (code=200):

```json
{
  "code": 200,
  "msg": null,
  "data": {
    "downloadUrl": "https://<account-id>.r2.cloudflarestorage.com/blog-test/avatars/1_1684512000000.jpg?X-Amz-..."
  }
}
```

**说明**：
- 返回 R2 预签名下载 URL（有效期 1 小时），直连 R2，不依赖自定义域名
- 前端使用此 URL 作为 `<img>` 的 `src` 显示头像

---

**头像上传完整流程**:

```
前端选择图片
    → POST /file/upload/url {objectKey, contentType}   ← 获取上传预签名 URL
    → PUT 预签名 URL (文件直传 R2，不经过后端)
    → PUT /user/avatar {objectKey}                     ← 后端将 objectKey 写入 DB
    → GET /file/download/url?objectKey=xxx              ← 获取下载预签名 URL
    → <img> 显示                                       ← 直连 R2，不依赖自定义域名
```

---

## 二、文章模块

### 1. 发布文章

**POST** `/article/add`

**请求头**:

| 字段 | 值 |
|------|------|
| Authorization | Bearer {token} |

**请求体** (JSON):

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | String | 是 | 文章标题，1-100 字符 |
| content | String | 是 | Markdown 正文 |
| tag | List\<String\> | 否 | 标签列表，如 `["Java","Spring"]` |
| image | List\<String\> | 否 | 文章配图 objectKey 数组，如 `["covers/1_xxx.jpg"]` |

```json
{
  "title": "Spring Boot 入门",
  "content": "# 第一章\n...",
  "tag": ["Java", "Spring"],
  "image": ["covers/1_1684512000000.jpg", "covers/1_1684512000001.jpg"]
}
```

**成功响应** (code=200):

```json
{
  "code": 200,
  "msg": null,
  "data": { "id": 1 }
}
```

**后端需处理**:
1. `writerId` 从 token 中获取，前端不传
2. `tag` 存入 `article` 表的 `tag` 字段（JSON 数组格式），同时写入 `article_tag` 关联表
3. `gmtCreate` 和 `gmtModified` 设为当前时间
4. 返回新文章 ID

---

### 2. 编辑文章

**PUT** `/article/{id}`

**请求头**:

| 字段 | 值 |
|------|------|
| Authorization | Bearer {token} |

**请求体** (JSON):

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | String | 是 | 文章标题 |
| content | String | 是 | Markdown 正文 |
| tag | List\<String\> | 否 | 标签列表 |
| image | List\<String\> | 否 | 文章配图 objectKey 数组 |

```json
{
  "title": "Spring Boot 入门（修订版）",
  "content": "# 第一章\n...",
  "tag": ["Java", "Spring", "Spring Boot"],
  "image": ["covers/1_1684512000000.jpg", "covers/1_1684512000002.jpg"]
}
```

**成功响应** (code=200):

```json
{
  "code": 200,
  "msg": null,
  "data": null
}
```

**失败响应** (code=400):

```json
{
  "code": 400,
  "msg": "只能修改自己的文章",
  "data": null
}
```

**后端需处理**:
1. 校验当前用户是否为文章作者，非作者返回 400
2. 更新 `article` 表及 `article_tag` 关联表
3. 更新 `gmtModified`

---

### 3. 删除文章

**DELETE** `/article/{id}`

**请求头**:

| 字段 | 值 |
|------|------|
| Authorization | Bearer {token} |

**成功响应** (code=200):

```json
{
  "code": 200,
  "msg": null,
  "data": null
}
```

**后端需处理**:
1. 校验当前用户是否为文章作者
2. 删除 `article` 记录及关联的 `article_tag` 记录
3. 将 `image` JSON 数组中的每个 objectKey 对应的 R2 文件逐一删除

---

### 4. 获取文章详情

**GET** `/article/{id}`

无需认证。

**成功响应** (code=200):

```json
{
  "code": 200,
  "msg": null,
  "data": {
    "id": 1,
    "title": "Spring Boot 入门",
    "content": "# 第一章\n...",
    "tag": ["Java", "Spring"],
    "image": ["covers/1_1684512000000.jpg", "covers/1_1684512000001.jpg"],
    "imageUrls": [
      "https://<r2-presigned-url-1>",
      "https://<r2-presigned-url-2>"
    ],
    "writerId": 1,
    "writerName": "zhangsan",
    "gmtCreate": "2026-05-15T12:00:00",
    "gmtModified": "2026-05-16T08:30:00"
  }
}
```

**后端需处理**:
1. 根据文章 ID 查询，同时 JOIN 用户表获取 `writerName`
2. `image` 字段从 JSON 字符串反序列化为 `List<String>`（objectKey 数组）
3. 对 `image` 中每个 objectKey 生成预签名下载 URL，填入 `imageUrls`
4. `tag` 从 JSON 字符串反序列化为 `List<String>`

---

### 5. 文章列表（公开）

**GET** `/article/list?page=1&size=10&keyword=Spring&tag=Java`

无需认证。

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码，默认 1 |
| size | Integer | 否 | 每页条数，默认 10 |
| keyword | String | 否 | 标题/内容关键词搜索 |
| tag | String | 否 | 按标签筛选 |

**成功响应** (code=200):

```json
{
  "code": 200,
  "msg": null,
  "data": {
    "total": 25,
    "page": 1,
    "size": 10,
    "list": [
      {
        "id": 1,
        "title": "Spring Boot 入门",
        "summary": "第一章...",
        "tag": ["Java", "Spring"],
        "image": ["covers/1_1684512000000.jpg"],
        "imageUrls": ["https://<r2-presigned-url>"],
        "writerId": 1,
        "writerName": "zhangsan",
        "gmtCreate": "2026-05-15T12:00:00"
      }
    ]
  }
}
```

**后端需处理**:
1. 分页查询，返回 `total`、`page`、`size`、`list`
2. `content` 截取前 200 字符作为 `summary`，列表不返回全文
3. 支持 `keyword` 模糊搜索（LIKE `%keyword%`）和 `tag` 筛选
4. 按 `gmtModified` 降序排列

---

### 6. 我的文章列表

**GET** `/article/my?page=1&size=10`

**请求头**:

| 字段 | 值 |
|------|------|
| Authorization | Bearer {token} |

**成功响应** (code=200):

```json
{
  "code": 200,
  "msg": null,
  "data": {
    "total": 5,
    "page": 1,
    "size": 10,
    "list": [
      {
        "id": 1,
        "title": "Spring Boot 入门",
        "summary": "第一章...",
        "tag": ["Java", "Spring"],
        "gmtCreate": "2026-05-15T12:00:00",
        "gmtModified": "2026-05-16T08:30:00"
      }
    ]
  }
}
```

**后端需处理**:
1. 从 token 获取当前用户 ID，只返回该用户的文章
2. 分页结构同 5 节
3. 列表不返回全文，`content` 截取为 `summary`

---

## 三、附加说明

### JWT 依赖

已配置在根 `pom.xml`，无需额外添加。

### 密码加密

当前使用 MD5（`DigestUtils.md5DigestAsHex`），后续建议迁移至 BCrypt。

### 拦截器

`JwtInterceptor` 已实现，对 `/**` 路径（除登录/注册/Knife4j/OPTIONS 外）统一校验。需放行公开接口：`GET /article/list`、`GET /article/{id}`。

### 接口认证范围

| 接口 | 是否需要认证 |
|------|-------------|
| POST /user/register | 否 |
| POST /user/login | 否 |
| GET /user/info | **是** |
| PUT /user/info | **是** |
| PUT /user/avatar | **是** |
| PUT /user/password | **是** |
| POST /article/add | **是** |
| PUT /article/{id} | **是** |
| DELETE /article/{id} | **是** |
| GET /article/list | 否 |
| GET /article/{id} | 否 |
| GET /article/my | **是** |
| POST /file/upload/url | **是** |
| GET /file/download/url | 否 |
