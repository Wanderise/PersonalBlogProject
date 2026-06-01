<template>
  <div class="image-uploader">
    <div v-if="existingImages.length > 0" class="existing-section">
      <div class="section-label">已上传图片</div>
      <div class="existing-list">
        <div
          v-for="img in existingImages"
          :key="img.key"
          class="image-card"
          :class="{ removed: removedKeys.includes(img.key) }"
        >
          <img :src="img.url" alt="" @click="previewExisting(img)" />
          <div class="card-actions">
            <el-icon class="preview-icon" @click.stop="previewExisting(img)"><ZoomIn /></el-icon>
            <el-icon class="remove-icon" @click.stop="removeExisting(img.key)"><Delete /></el-icon>
          </div>
        </div>
      </div>
    </div>

    <div class="section-label">上传新图片</div>
    <el-upload
      ref="uploadRef"
      list-type="picture-card"
      action="#"
      :auto-upload="false"
      multiple
      v-model:file-list="fileList"
      :before-upload="beforeAdd"
      :on-change="handleChange"
      :on-preview="handlePreview"
      :on-remove="handleRemove"
    >
      <div class="upload-trigger">
        <el-icon :size="24"><Plus /></el-icon>
        <span>添加图片</span>
      </div>
    </el-upload>

    <el-dialog v-model="dialogVisible" class="preview-dialog">
      <img :src="dialogImageUrl" alt="" style="width: 100%" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Plus, ZoomIn, Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  existingImages: { type: Array, default: () => [] }
})

const fileList = ref([])
const dialogVisible = ref(false)
const dialogImageUrl = ref('')
const removedKeys = ref([])

function beforeAdd(file) {
  if (!file.type.startsWith('image/')) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (file.size / 1024 / 1024 > 5) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

function handleChange(uploadFile) {
  uploadFile.url = URL.createObjectURL(uploadFile.raw)
}

function handlePreview(uploadFile) {
  dialogImageUrl.value = uploadFile.url
  dialogVisible.value = true
}

function handleRemove(uploadFile) {
  const idx = fileList.value.indexOf(uploadFile)
  if (idx > -1) fileList.value.splice(idx, 1)
}

function previewExisting(img) {
  dialogImageUrl.value = img.url
  dialogVisible.value = true
}

function removeExisting(key) {
  removedKeys.value.push(key)
}

function getValidExistingKeys() {
  return props.existingImages
    .filter((img) => !removedKeys.value.includes(img.key))
    .map((img) => img.key)
}

function getFiles() {
  return fileList.value.map((f) => f.raw)
}

function clearFiles() {
  fileList.value = []
  removedKeys.value = []
}

defineExpose({ getFiles, clearFiles, getValidExistingKeys })
</script>

<style scoped>
.existing-section {
  margin-bottom: 16px;
}

.section-label {
  font-size: 13px;
  color: var(--c-text-light);
  margin-bottom: 8px;
}

.existing-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.image-card {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid var(--c-border);
  cursor: pointer;
  transition: opacity 0.2s;
}

.image-card.removed {
  opacity: 0.3;
  pointer-events: none;
}

.image-card img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-actions {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: rgba(0, 0, 0, 0.45);
  opacity: 0;
  transition: opacity 0.2s;
}

.image-card:hover .card-actions {
  opacity: 1;
}

.card-actions .el-icon {
  color: #fff;
  font-size: 18px;
  padding: 6px;
  border-radius: 4px;
  transition: background 0.15s;
}

.card-actions .preview-icon:hover {
  background: rgba(255, 255, 255, 0.25);
}

.card-actions .remove-icon:hover {
  background: rgba(245, 108, 108, 0.7);
}

.upload-trigger {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: #8c939d;
  font-size: 12px;
}
</style>
