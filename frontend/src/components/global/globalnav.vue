<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
const isLoggedIn = ref(false)
const userName = ref('')

const updateAuthState = () => {
  const token = localStorage.getItem('token')
  isLoggedIn.value = !!token
  if (token) {
    try {
      const user = JSON.parse(localStorage.getItem('user') || '{}')
      userName.value = user.name || '用户'
    } catch {
      userName.value = '用户'
    }
  }
}

watch(
  () => route.fullPath,
  () => {
    updateAuthState()
  },
  { immediate: true }
)

const handleToProfile = () => {
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  router.push(`/User/${user.id || 1}/profile`)
}

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  isLoggedIn.value = false
  userName.value = ''
  router.push('/login')
}
</script>

<template>
  <header class="global-header">
    <div class="header-inner">
      <router-link to="/" class="site-name">个人博客</router-link>

      <nav class="header-nav" v-if="isLoggedIn">
        <router-link to="/User/1/home">首页</router-link>
        <router-link to="/User/1/editor">写文章</router-link>
        <router-link to="/User/1/list">文章列表</router-link>
        <el-dropdown trigger="click" class="user-dropdown">
          <span class="user-avatar">{{ userName.charAt(0) }}</span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="handleToProfile">查看个人信息</el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </nav>

      <nav class="header-nav" v-else>
        <router-link to="/login">登录</router-link>
        <router-link to="/register" class="register-link">注册</router-link>
      </nav>
    </div>
  </header>
</template>

<style scoped>
.global-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: #fff;
  border-bottom: 1px solid #e8eaed;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.site-name {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a2e;
  text-decoration: none;
  letter-spacing: 1px;
}

.header-nav {
  display: flex;
  gap: 8px;
  align-items: center;
}

.header-nav a {
  padding: 6px 16px;
  border-radius: 6px;
  color: #555;
  text-decoration: none;
  font-size: 14px;
  transition: all 0.2s;
}

.header-nav a:hover {
  background: #f0f2f5;
  color: #409eff;
}

.header-nav a.router-link-active {
  background: #e8f0fe;
  color: #409eff;
  font-weight: 500;
}

.register-link {
  background: #409eff !important;
  color: #fff !important;
}

.register-link:hover {
  background: #337ecc !important;
  color: #fff !important;
}

.register-link.router-link-active {
  background: #409eff !important;
  color: #fff !important;
}

.user-dropdown {
  margin-left: 12px;
}

.user-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.user-avatar:hover {
  opacity: 0.85;
}
</style>
