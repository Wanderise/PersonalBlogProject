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
        <el-input-tag
          v-model="article.tags"
          placeholder="输入后按回车添加"
          size="large"
        />
      </div>
      <div class="form-section">
        <label class="section-label">文章图片</label>
        <ImageUploader ref="uploaderRef" :existing-images="existingImagesWithUrls" />
      </div>
    </div>

    <div v-if="uploading" class="upload-status">
      <div class="status-text">
        <el-icon class="is-loading"><Loading /></el-icon>
        正在上传图片（{{ uploadedCount }}/{{ totalCount }}）...
      </div>
      <el-progress
        :percentage="uploadPercent"
        :stroke-width="6"
        :show-text="false"
        class="upload-bar"
      />
    </div>

    <div class="form-actions">
      <el-button
        type="primary"
        size="large"
        :disabled="!article.title.trim()"
        :loading="submitting"
        @click="submit"
      >
        <el-icon v-if="!submitting"><Upload /></el-icon>
        <span>{{ isEdit ? '保存修改' : '发布文章' }}</span>
      </el-button>
      <el-button v-if="isEdit" size="large" :disabled="submitting" @click="$router.back()">
        取消
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { v4 as uuidv4 } from 'uuid'
import { Upload, Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { uploadArticleRequest, getArticleById, updateArticle } from '@/api/article.js'
import { getUploadUrl } from '@/api/file.js'
import ImageUploader from '@/components/user/mdeditor/ImageUploader.vue'

const route = useRoute()
const router = useRouter()
const editId = route.query.edit ? Number(route.query.edit) : null
const isEdit = !!editId

const article = reactive({ title: '', text: '', tags: [] })
const existingImages = ref([])
const existingImagesWithUrls = computed(() => {
  return existingImages.value.map((key, i) => ({
    key,
    url: existingImageUrls.value[i] || key
  }))
})
const existingImageUrls = ref([])
const uploaderRef = ref(null)
const submitting = ref(false)
const uploading = ref(false)
const uploadedCount = ref(0)
const totalCount = ref(0)

const uploadPercent = computed(() => {
  if (totalCount.value === 0) return 0
  return Math.round((uploadedCount.value / totalCount.value) * 100)
})

onMounted(async () => {
  if (isEdit) {
    try {
      const res = await getArticleById(editId)
      const data = res.data
      article.title = data.title || ''
      article.text = data.content || ''
      article.tags = data.tag || []
      existingImages.value = data.image || []
      existingImageUrls.value = data.imageUrls || []
    } catch {
      ElMessage.error('加载文章失败')
      router.back()
    }
  }
})

async function submit() {
  submitting.value = true
  try {
    const files = uploaderRef.value?.getFiles() || []
    const validExistingKeys = uploaderRef.value?.getValidExistingKeys() || []
    const imageKeys = [...validExistingKeys]

    if (files.length > 0) {
      uploading.value = true
      totalCount.value = files.length
      uploadedCount.value = 0

      const tasks = files.map(async (file) => {
        const objectKey = `images/${uuidv4()}.${file.name.split('.').pop()}`
        const urlRes = await getUploadUrl({ objectKey, contentType: file.type })
        await axios.put(urlRes.data.uploadUrl, file, { headers: { 'Content-Type': file.type } })
        uploadedCount.value++
        return objectKey
      })

      imageKeys.push(...(await Promise.all(tasks)))
      uploading.value = false
    }

    const payload = {
      title: article.title,
      content: article.text,
      tag: article.tags,
      image: imageKeys
    }

    if (isEdit) {
      await updateArticle(editId, payload)
      ElMessage.success('文章已更新')
      router.push(`/article/${editId}`)
    } else {
      const res = await uploadArticleRequest(payload)
      ElMessage.success('文章发布成功')
      uploaderRef.value?.clearFiles()
      const newId = res.data && res.data.id
      if (newId) router.push(`/article/${newId}`)
    }
  } catch {
    ElMessage.error(isEdit ? '保存失败' : '发布失败，请重试')
  } finally {
    submitting.value = false
    uploading.value = false
    totalCount.value = 0
    uploadedCount.value = 0
  }
}
</script>

<style scoped>
.editor-form {
  background: var(--c-surface);
  border-radius: var(--radius);
  padding: 36px;
  box-shadow: var(--c-shadow-md);
  border: 1px solid var(--c-border);
}

.form-section {
  margin-bottom: 28px;
}

.section-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text);
  margin-bottom: 10px;
}

.title-input {
  max-width: 640px;
}

.form-row {
  display: flex;
  gap: 40px;
  align-items: flex-start;
}

.form-grow {
  flex: 1;
  min-width: 0;
}

.upload-status {
  margin: 16px 0;
  padding: 12px 16px;
  background: var(--c-primary-light);
  border-radius: var(--radius-sm);
  border: 1px solid rgba(91, 127, 255, 0.15);
}

.status-text {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--c-primary);
  margin-bottom: 8px;
}

.upload-bar { max-width: 400px; }

.form-actions {
  margin-top: 12px;
  padding-top: 28px;
  border-top: 1px solid var(--c-border);
  display: flex;
  gap: 12px;
}
</style>
