<script setup>
</script>

<template>
  <div class="home-page">
    <div class="hero">
      <h1>欢迎来到我的博客</h1>
      <p>记录技术，分享生活</p>
    </div>
    <div class="quick-links">
      <router-link to="/User/1/editor" class="quick-card">
        <div class="card-icon">✍️</div>
        <h3>写文章</h3>
        <p>使用 Markdown 编辑器撰写新文章</p>
      </router-link>
      <router-link to="/User/1/list" class="quick-card">
        <div class="card-icon">📋</div>
        <h3>文章列表</h3>
        <p>浏览和管理已发布的文章</p>
      </router-link>
    </div>
  </div>
</template>

<style scoped>
.home-page {
  padding-top: 20px;
}

.hero {
  text-align: center;
  padding: 80px 20px 60px;
}

.hero h1 {
  font-size: 36px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 12px;
}

.hero p {
  font-size: 18px;
  color: #888;
}

.quick-links {
  display: flex;
  gap: 24px;
  justify-content: center;
  max-width: 600px;
  margin: 0 auto;
}

.quick-card {
  flex: 1;
  display: block;
  padding: 32px 24px;
  background: #fff;
  border-radius: 12px;
  text-align: center;
  text-decoration: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: transform 0.2s, box-shadow 0.2s;
}

.quick-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.card-icon {
  font-size: 36px;
  margin-bottom: 12px;
}

.quick-card h3 {
  font-size: 18px;
  color: #1a1a2e;
  margin-bottom: 8px;
}

.quick-card p {
  font-size: 13px;
  color: #999;
}
</style>
