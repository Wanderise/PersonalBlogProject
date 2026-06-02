<script setup>
</script>

<template>
  <div class="home-page">
    <section class="hero">
      <div class="hero-badge">书写 · 沉淀 · 分享</div>
      <h1>用文字记录每一次思考</h1>
      <p>一个安静的技术博客空间，在这里沉淀你的学习与成长</p>
      <div class="hero-actions">
        <router-link to="/editor" class="hero-btn hero-btn-primary">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="btn-icon">
            <path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/>
          </svg>
          开始写作
        </router-link>
        <router-link to="/User/1/list" class="hero-btn hero-btn-ghost">浏览文章</router-link>
      </div>
    </section>

    <section class="features">
      <router-link to="/editor" class="feature-card">
        <div class="feature-icon" style="background: #f5f3ff; color: #7c3aed;">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/>
          </svg>
        </div>
        <h3>写文章</h3>
        <p>Markdown 编辑器，支持图片上传与标签管理</p>
      </router-link>

      <router-link to="/User/1/articles" class="feature-card">
        <div class="feature-icon" style="background: #ecfdf5; color: #10b981;">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/>
            <line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/>
          </svg>
        </div>
        <h3>我的文章</h3>
        <p>随时管理和编辑已发布的文章</p>
      </router-link>

      <router-link to="/User/1/list" class="feature-card">
        <div class="feature-icon" style="background: #fef3c7; color: #f59e0b;">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
        </div>
        <h3>发现</h3>
        <p>浏览大家的精彩文章，发现新知识</p>
      </router-link>
    </section>
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
  padding: 80px 20px 56px;
}

.hero-badge {
  display: inline-block;
  padding: 5px 18px;
  background: var(--c-primary-light);
  color: var(--c-primary);
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 28px;
  letter-spacing: 0.04em;
}

.hero h1 {
  font-size: 42px;
  font-weight: 750;
  color: var(--c-text);
  letter-spacing: -0.025em;
  line-height: 1.2;
  margin-bottom: 16px;
}

.hero p {
  font-size: 18px;
  color: var(--c-text-secondary);
  line-height: 1.6;
  max-width: 460px;
  margin: 0 auto 40px;
}

.hero-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.hero-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 13px 28px;
  border-radius: var(--radius-sm);
  font-size: 15px;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.25s ease;
}

.btn-icon { width: 18px; height: 18px; }

.hero-btn-primary {
  background: var(--c-primary);
  color: #fff;
  box-shadow: 0 4px 16px var(--c-primary-glow);
}

.hero-btn-primary:hover {
  background: var(--c-primary-dark);
  transform: translateY(-2px);
  box-shadow: 0 6px 24px var(--c-primary-glow);
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
  box-shadow: var(--c-shadow-md);
}

/* === Features === */
.features {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  padding: 0 20px 48px;
}

.feature-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 40px 28px;
  background: var(--c-surface);
  border-radius: var(--radius);
  border: 1px solid var(--c-border);
  text-decoration: none;
  transition: all 0.3s ease;
}

.feature-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--c-shadow-lg);
  border-color: transparent;
}

.feature-icon {
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  margin-bottom: 22px;
}

.feature-icon svg { width: 26px; height: 26px; }

.feature-card h3 {
  font-size: 17px;
  font-weight: 650;
  color: var(--c-text);
  margin-bottom: 8px;
}

.feature-card p {
  font-size: 14px;
  color: var(--c-text-muted);
  line-height: 1.55;
  max-width: 200px;
}

@media (max-width: 640px) {
  .hero h1 { font-size: 28px; }
  .features { grid-template-columns: 1fr; }
}
</style>
