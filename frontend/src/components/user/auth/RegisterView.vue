<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/auth.js'

const router = useRouter()

const form = reactive({ username: '', password: '', confirmPassword: '' })
const formRef = ref(null)
const loading = ref(false)

const validateConfirm = (rule, value, callback) => {
  callback(value !== form.password ? new Error('两次输入的密码不一致') : undefined)
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度为 2-20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度为 6-30 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await register({ name: form.username, password: form.password })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch {
    ElMessage.error('注册失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-header">
        <div class="auth-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/>
            <line x1="20" y1="8" x2="20" y2="14"/><line x1="23" y1="11" x2="17" y2="11"/>
          </svg>
        </div>
        <h2>创建账号</h2>
        <p>开始你的博客之旅</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="handleRegister">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" size="large" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" class="submit-btn" @click="handleRegister">
            注册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="auth-footer">
        已有账号？<router-link to="/login">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: calc(100vh - 56px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.auth-card {
  width: 400px;
  background: var(--c-surface);
  border-radius: var(--radius-lg);
  padding: 48px 40px 40px;
  box-shadow: var(--c-shadow-lg);
  border: 1px solid var(--c-border);
}

.auth-header {
  text-align: center;
  margin-bottom: 36px;
}

.auth-icon {
  width: 48px;
  height: 48px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--c-primary-light);
  border-radius: var(--radius-sm);
  color: var(--c-primary);
  margin-bottom: 16px;
}

.auth-icon svg { width: 24px; height: 24px; }

.auth-header h2 {
  font-size: 22px;
  font-weight: 700;
  color: var(--c-text);
  margin-bottom: 6px;
}

.auth-header p {
  font-size: 14px;
  color: var(--c-text-muted);
}

.submit-btn { width: 100%; }

.auth-footer {
  text-align: center;
  font-size: 14px;
  color: var(--c-text-muted);
}

.auth-footer a {
  color: var(--c-primary);
  text-decoration: none;
  font-weight: 500;
}
</style>
