<template>
  <div class="image-uploader">
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
      <img :src="dialogImageUrl" alt="Preview" style="width: 100%" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const fileList = ref([])
const dialogVisible = ref(false)
const dialogImageUrl = ref('')

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

function getFiles() {
  return fileList.value.map((f) => f.raw)
}

function clearFiles() {
  fileList.value = []
}

defineExpose({ getFiles, clearFiles })
</script>

<style scoped>
.upload-trigger {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: #8c939d;
  font-size: 12px;
}
</style>
