<script setup>
</script>

<template>
  <div class="home-page">
    <div class="hero">
      <div class="hero-badge">个人博客</div>
      <h1>用文字记录思考和成长</h1>
      <p>一个安静的地方，写下技术笔记与生活随想</p>
      <div class="hero-actions">
        <router-link to="/editor" class="hero-btn hero-btn-primary">开始写作</router-link>
        <router-link to="/User/1/list" class="hero-btn hero-btn-ghost">浏览文章</router-link>
      </div>
    </div>

    <div class="features">
      <router-link to="/editor" class="feature-card">
        <div class="feature-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/>
          </svg>
        </div>
        <h3>写文章</h3>
        <p>Markdown 编辑器，支持图片上传</p>
      </router-link>

      <router-link to="/User/1/articles" class="feature-card">
        <div class="feature-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/>
            <line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/>
          </svg>
        </div>
        <h3>我的文章</h3>
        <p>管理和编辑已发布的文章</p>
      </router-link>

      <router-link to="/User/1/list" class="feature-card">
        <div class="feature-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
        </div>
        <h3>发现</h3>
        <p>浏览社区中大家的精彩文章</p>
      </router-link>
    </div>
  </div>
</template>

<style scoped>
.home-page {
  max-width: 960px;
  margin: 0 auto;
}

/* === Hero === */
.hero {
  text-align: center;
  padding: 80px 20px 64px;
}

.hero-badge {
  display: inline-block;
  padding: 4px 16px;
  background: var(--c-primary-light);
  color: var(--c-primary);
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 24px;
  letter-spacing: 0.02em;
}

.hero h1 {
  font-size: 40px;
  font-weight: 700;
  color: var(--c-text);
  letter-spacing: -0.02em;
  line-height: 1.25;
  margin-bottom: 16px;
}

.hero p {
  font-size: 18px;
  color: var(--c-text-secondary);
  line-height: 1.6;
  max-width: 480px;
  margin: 0 auto 36px;
}

.hero-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.hero-btn {
  display: inline-flex;
  align-items: center;
  padding: 12px 28px;
  border-radius: var(--radius-sm);
  font-size: 15px;
  font-weight: 600;
  text-decoration: none;
  transition: all var(--transition);
}

.hero-btn-primary {
  background: var(--c-primary);
  color: #fff;
  box-shadow: 0 2px 12px rgba(91, 127, 255, 0.3);
}

.hero-btn-primary:hover {
  background: var(--c-primary-dark);
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(91, 127, 255, 0.4);
}

.hero-btn-ghost {
  background: var(--c-surface);
  color: var(--c-text);
  border: 1px solid var(--c-border);
}

.hero-btn-ghost:hover {
  border-color: var(--c-primary);
  color: var(--c-primary);
  transform: translateY(-2px);
}

/* === Features === */
.features {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  padding: 0 20px 40px;
}

.feature-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 36px 24px;
  background: var(--c-surface);
  border-radius: var(--radius);
  border: 1px solid var(--c-border);
  text-decoration: none;
  transition: all 0.3s ease;
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--c-shadow-lg);
  border-color: transparent;
}

.feature-icon {
  width: 52px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--c-primary-light);
  border-radius: var(--radius-sm);
  margin-bottom: 20px;
  color: var(--c-primary);
}

.feature-icon svg {
  width: 24px;
  height: 24px;
}

.feature-card h3 {
  font-size: 17px;
  font-weight: 600;
  color: var(--c-text);
  margin-bottom: 8px;
}

.feature-card p {
  font-size: 14px;
  color: var(--c-text-muted);
  line-height: 1.5;
}

@media (max-width: 640px) {
  .hero h1 { font-size: 28px; }
  .features { grid-template-columns: 1fr; }
}
</style>
