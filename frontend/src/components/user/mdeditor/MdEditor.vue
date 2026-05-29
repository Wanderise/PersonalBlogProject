<script setup>
import MainEditor from '@/components/user/mdeditor/MainEditor.vue';
</script>

<template>
  <div class="editor-page">
    <div class="page-header">
      <h2>撰写文章</h2>
      <p>使用 Markdown 格式编写内容</p>
    </div>
    <MainEditor />
  </div>
</template>

<style scoped>
.editor-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px 0;
}

.page-header {
  margin-bottom: 28px;
}

.page-header h2 {
  font-size: 24px;
  font-weight: 700;
  color: var(--c-text);
  margin-bottom: 6px;
}

.page-header p {
  font-size: 14px;
  color: var(--c-text-muted);
}
</style>
