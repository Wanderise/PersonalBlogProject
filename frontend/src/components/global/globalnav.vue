<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth.js'
import { House, Compass, Document, MagicStick } from '@element-plus/icons-vue'

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
        <span class="site-mark">博</span>
        <span class="site-copy">
          <strong>个人博客</strong>
          <small>WRITE & THINK</small>
        </span>
      </router-link>

      <nav class="header-nav" v-if="state.isLoggedIn">
        <router-link to="/User/1/home"><el-icon><House /></el-icon><span>首页</span></router-link>
        <router-link to="/User/1/list"><el-icon><Compass /></el-icon><span>发现</span></router-link>
        <router-link to="/User/1/articles"><el-icon><Document /></el-icon><span>我的文章</span></router-link>
        <router-link to="/ai" class="ai-nav-link">
          <el-icon><MagicStick /></el-icon><span>AI 工作台</span>
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
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(14px);
  border-bottom: 1px solid var(--c-border);
}

.header-inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 28px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.site-name {
  color: var(--c-text);
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 10px;
}

.site-mark {
  width: 34px;
  height: 34px;
  border-radius: 7px;
  background: var(--c-primary);
  color: #fff;
  display: grid;
  place-items: center;
  font-family: Georgia, "Microsoft YaHei", serif;
  font-size: 17px;
  font-weight: 700;
}

.site-copy { display: flex; flex-direction: column; line-height: 1.1; }
.site-copy strong { font-size: 15px; font-weight: 700; }
.site-copy small { margin-top: 4px; font-size: 9px; color: var(--c-text-muted); }

.header-nav {
  display: flex;
  gap: 2px;
  align-items: center;
}

.header-nav a {
  padding: 7px 16px;
  border-radius: 6px;
  color: var(--c-text-secondary);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: all var(--transition);
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.header-nav a:hover {
  background: var(--c-primary-light);
  color: var(--c-primary);
}

.header-nav a.router-link-active {
  background: var(--c-primary-light);
  color: var(--c-primary);
  font-weight: 600;
  box-shadow: inset 0 0 0 1px rgba(22, 122, 105, 0.08);
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

.ai-nav-link { margin-left: 4px; }

.user-dropdown { margin-left: 6px; }

.user-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--c-accent);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: transform var(--transition), box-shadow var(--transition);
}

@media (max-width: 760px) {
  .header-inner { padding: 0 14px; height: 58px; }
  .site-copy small { display: none; }
  .header-nav { gap: 0; }
  .header-nav a { padding: 8px 10px; }
  .header-nav a span { display: none; }
  .ai-nav-link { margin-left: 0; }
  .user-dropdown { margin-left: 2px; }
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
