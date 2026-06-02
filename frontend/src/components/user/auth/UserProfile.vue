<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getUserInfo, updateUserInfo, updateAvatar, updatePassword } from '@/api/auth.js'
import { getUploadUrl, getDownloadUrl } from '@/api/file.js'
import { useAuth } from '@/composables/useAuth.js'

const { loadAvatar, updateLocalUser } = useAuth()

const user = reactive({ id: 0, name: '', image: '', level: 0, gmtCreate: '' })
const editing = ref(false)
const loading = ref(false)
const saving = ref(false)
const avatarUploading = ref(false)
const changingPassword = ref(false)

const editForm = reactive({ name: '' })
const passwordFormRef = ref(null)
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度为 6-30 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        callback(value !== passwordForm.newPassword ? new Error('两次输入的密码不一致') : undefined)
      },
      trigger: 'blur'
    }
  ]
}

async function fetchUserInfo() {
  loading.value = true
  try {
    const res = await getUserInfo()
    const data = res.data
    Object.assign(user, data)
    if (data.image) {
      const urlRes = await getDownloadUrl(data.image)
      user.image = urlRes.data.downloadUrl
    }
  } catch {
    ElMessage.error('获取用户信息失败')
  } finally {
    loading.value = false
  }
}

function startEdit() {
  editForm.name = user.name
  editing.value = true
}

function cancelEdit() {
  editing.value = false
  changingPassword.value = false
  Object.assign(passwordForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
}

async function saveInfo() {
  if (!editForm.name.trim()) {
    ElMessage.warning('用户名不能为空')
    return
  }
  saving.value = true
  try {
    if (changingPassword.value) {
      const valid = await passwordFormRef.value.validate().catch(() => false)
      if (!valid) { saving.value = false; return }
    }

    const res = await updateUserInfo({ name: editForm.name.trim() })
    user.name = res.data.name || editForm.name.trim()
    updateLocalUser({ name: user.name })

    if (changingPassword.value) {
      await updatePassword({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword })
    }

    editing.value = false
    cancelEdit()
    ElMessage.success('修改成功')
  } catch {
    ElMessage.error('修改失败')
  } finally {
    saving.value = false
  }
}

async function handleAvatarChange(file) {
  const validTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
  if (!validTypes.includes(file.type)) return ElMessage.error('仅支持 JPG / PNG / GIF / WebP 格式')
  if (file.size > 2 * 1024 * 1024) return ElMessage.error('图片大小不能超过 2MB')

  avatarUploading.value = true
  try {
    const objectKey = `avatars/${user.id}_${Date.now()}.${file.name.split('.').pop()}`
    const { data } = await getUploadUrl({ objectKey, contentType: file.type })
    await fetch(data.uploadUrl, { method: 'PUT', headers: { 'Content-Type': file.type }, body: file })
    await updateAvatar(objectKey)
    updateLocalUser({ image: objectKey })
    const { data: dlData } = await getDownloadUrl(objectKey)
    user.image = dlData.downloadUrl
    await loadAvatar()
    ElMessage.success('头像更新成功')
  } catch {
    ElMessage.error('头像上传失败')
  } finally {
    avatarUploading.value = false
  }
}

function beforeAvatarUpload(file) {
  handleAvatarChange(file)
  return false
}

function levelLabel(lv) {
  const map = { 0: '普通用户', 1: '管理员', 2: '超级管理员' }
  return map[lv] || '普通用户'
}

onMounted(() => { fetchUserInfo() })
</script>

<template>
  <div class="profile-page" v-loading="loading">
    <div class="profile-header">
      <el-upload
        class="avatar-uploader"
        :show-file-list="false"
        :before-upload="beforeAvatarUpload"
        accept="image/*"
      >
        <el-avatar :size="100" :src="user.image">
          {{ user.name?.charAt(0) || '用' }}
        </el-avatar>
        <div class="avatar-overlay">
          <span v-if="avatarUploading">上传中...</span>
          <span v-else>更换头像</span>
        </div>
      </el-upload>
      <h2 class="profile-name">{{ user.name }}</h2>
      <span class="profile-badge">{{ levelLabel(user.level) }}</span>
      <p class="profile-join">注册于 {{ user.gmtCreate || '-' }}</p>
    </div>

    <div class="profile-card">
      <div class="card-title">
        <h3>个人信息</h3>
        <el-button v-if="!editing" type="primary" size="small" @click="startEdit">编辑</el-button>
      </div>

      <el-descriptions v-if="!editing" :column="1" border>
        <el-descriptions-item label="用户ID">{{ user.id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ user.name }}</el-descriptions-item>
        <el-descriptions-item label="用户等级">{{ levelLabel(user.level) }}</el-descriptions-item>
      </el-descriptions>

      <el-form v-else label-width="80px" class="edit-form">
        <el-form-item label="用户名">
          <el-input v-model="editForm.name" maxlength="20" show-word-limit />
        </el-form-item>

        <el-divider />
        <el-form-item label="修改密码">
          <el-switch v-model="changingPassword" active-text="是" inactive-text="否" />
        </el-form-item>

        <template v-if="changingPassword">
          <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="100px">
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入当前密码" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" show-password />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
            </el-form-item>
          </el-form>
        </template>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="saveInfo">保存</el-button>
          <el-button @click="cancelEdit">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.profile-page {
  max-width: 600px;
  margin: 0 auto;
  padding: 24px 0 60px;
}

.profile-header {
  text-align: center;
  padding: 48px 0 36px;
}

.avatar-uploader {
  display: inline-block;
  position: relative;
  cursor: pointer;
  border-radius: 50%;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.25s;
}

.avatar-uploader:hover .avatar-overlay { opacity: 1; }

.profile-name {
  margin: 22px 0 10px;
  font-size: 28px;
  font-weight: 700;
  color: var(--c-text);
}

.profile-badge {
  display: inline-block;
  padding: 4px 16px;
  border-radius: 14px;
  font-size: 13px;
  font-weight: 500;
  color: var(--c-primary);
  background: var(--c-primary-light);
}

.profile-join {
  margin-top: 12px;
  font-size: 13px;
  color: var(--c-text-muted);
}

.profile-card {
  background: var(--c-surface);
  border-radius: var(--radius);
  padding: 32px;
  box-shadow: var(--c-shadow);
  border: 1px solid var(--c-border);
}

.card-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.card-title h3 {
  margin: 0;
  font-size: 17px;
  font-weight: 650;
  color: var(--c-text);
}

.edit-form { margin-top: 8px; }
</style>
