<template>
  <div class="editor-form">
    <div class="form-section">
      <label class="section-label">文章标题</label>
      <el-input
        v-model="article.title"
        placeholder="请输入文章标题"
        size="large"
        class="title-input"
      />
    </div>

    <div class="form-section">
      <label class="section-label">文章内容</label>
      <v-md-editor v-model="article.text" height="500px" />
    </div>

    <div class="form-row">
      <div class="form-section form-grow">
        <label class="section-label">文章标签</label>
        <el-input-tag v-model="article.tags" placeholder="输入后按回车添加" />
      </div>
      <div class="form-section">
        <label class="section-label">插入图片</label>
        <ImageUploader />
      </div>
    </div>

    <div class="form-actions">
      <el-button type="primary" size="large" :disabled="!article.title.trim()" @click="submit">
        发布文章
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadArticleRequest } from "@/api/article.js"
import ImageUploader from "@/components/user/mdeditor/ImageUploader.vue"

const article = reactive({
  title: '',
  text: '',
  tags: []
})

const submit = async () => {
  try {
    const data = {
      title: article.title,
      writerId: 1,
      content: article.text,
      tag: article.tags
    }
    await uploadArticleRequest(data)
    ElMessage.success('文章发布成功')
  } catch (error) {
    ElMessage.error('发布失败，请重试')
  }
}
</script>

<style scoped>
.editor-form {
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.form-section {
  margin-bottom: 24px;
}

.section-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #555;
  margin-bottom: 8px;
}

.title-input {
  max-width: 600px;
}

.form-row {
  display: flex;
  gap: 32px;
  align-items: flex-start;
}

.form-grow {
  flex: 1;
}

.form-actions {
  margin-top: 8px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}
</style>
