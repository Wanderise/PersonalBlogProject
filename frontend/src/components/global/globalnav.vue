<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth.js'

const router = useRouter()
const { state, loadAvatar, clearAuth } = useAuth()

onMounted(() => { loadAvatar() })

function handleToProfile() {
  router.push(`/User/${state.user.id || 1}/profile`)
}

function handleLogout() {
  clearAuth()
  router.push('/login')
}
</script>

<template>
  <header class="global-header">
    <div class="header-inner">
      <router-link to="/" class="site-name">
        <span class="site-dot"></span>
        个人博客
      </router-link>

      <nav class="header-nav" v-if="state.isLoggedIn">
        <router-link to="/User/1/home">首页</router-link>
        <router-link to="/User/1/list">发现</router-link>
        <router-link to="/User/1/articles">我的文章</router-link>
        <router-link to="/ai" class="ai-nav-link">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="ai-nav-icon">
            <path d="M12 2a4 4 0 0 1 3.5 2.1L12 12l3.5 7.9A4 4 0 0 1 12 22a4 4 0 0 1-3.5-2.1L12 12 8.5 4.1A4 4 0 0 1 12 2z"/>
          </svg>
          AI
        </router-link>
        <el-dropdown trigger="click" class="user-dropdown">
          <el-avatar
            v-if="state.avatarUrl"
            :size="34"
            :src="state.avatarUrl"
            class="user-avatar-img"
          />
          <span v-else class="user-avatar">{{ state.userName.charAt(0) }}</span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="handleToProfile">个人信息</el-dropdown-item>
              <el-dropdown-item @click="router.push('/User/1/articles')">我的文章</el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </nav>

      <nav class="header-nav" v-else>
        <router-link to="/login">登录</router-link>
        <router-link to="/register" class="btn-register">注册</router-link>
      </nav>
    </div>
  </header>
</template>

<style scoped>
.global-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(16px) saturate(180%);
  border-bottom: 1px solid var(--c-border);
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 28px;
  height: 58px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.site-name {
  font-size: 19px;
  font-weight: 700;
  color: var(--c-text);
  text-decoration: none;
  letter-spacing: -0.01em;
  display: flex;
  align-items: center;
  gap: 8px;
}

.site-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--c-primary);
}

.header-nav {
  display: flex;
  gap: 2px;
  align-items: center;
}

.header-nav a {
  padding: 7px 16px;
  border-radius: var(--radius-sm);
  color: var(--c-text-secondary);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: all var(--transition);
}

.header-nav a:hover {
  background: var(--c-primary-light);
  color: var(--c-primary);
}

.header-nav a.router-link-active {
  background: var(--c-primary-light);
  color: var(--c-primary);
  font-weight: 600;
}

.btn-register {
  background: var(--c-primary) !important;
  color: #fff !important;
  margin-left: 4px;
}

.btn-register:hover {
  background: var(--c-primary-dark) !important;
  color: #fff !important;
}

.ai-nav-link {
  display: flex !important;
  align-items: center;
  gap: 5px;
}

.ai-nav-icon {
  width: 16px;
  height: 16px;
}

.user-dropdown { margin-left: 6px; }

.user-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--c-primary), var(--c-primary-dark));
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: transform var(--transition), box-shadow var(--transition);
}

.user-avatar:hover {
  transform: scale(1.08);
  box-shadow: 0 2px 10px var(--c-primary-glow);
}

.user-avatar-img {
  cursor: pointer;
  transition: transform var(--transition), box-shadow var(--transition);
  border: 2px solid var(--c-border);
}

.user-avatar-img:hover {
  transform: scale(1.08);
  box-shadow: 0 2px 10px var(--c-primary-glow);
}
</style>
