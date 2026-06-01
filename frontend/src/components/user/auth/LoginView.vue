<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth.js'
import { useAuth } from '@/composables/useAuth.js'

const router = useRouter()
const { setAuth } = useAuth()

const form = reactive({ username: '', password: '' })
const formRef = ref(null)
const loading = ref(false)

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await login({ name: form.username, password: form.password })
    setAuth(res.data.token, res.data.user)
    ElMessage.success('登录成功')
    router.push('/User/1/home')
  } catch {
    ElMessage.error('用户名或密码错误')
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
            <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/>
            <polyline points="10 17 15 12 10 7"/><line x1="15" y1="12" x2="3" y2="12"/>
          </svg>
        </div>
        <h2>欢迎回来</h2>
        <p>登录你的博客账号</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="handleLogin">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" size="large" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" class="submit-btn" @click="handleLogin">
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="auth-footer">
        还没有账号？<router-link to="/register">立即注册</router-link>
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
