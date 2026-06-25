<template>
  <div v-if="visible" class="ai-fab" @click="$router.push('/ai')" title="AI 助手">
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
      <path d="M12 2a4 4 0 0 1 4 4v.5a4 4 0 0 1-2.5 3.7c-1.2.5-2.5.5-3.7 0A4 4 0 0 1 8 6.5V6a4 4 0 0 1 4-4z"/>
      <path d="M8 14a4 4 0 0 0-4 4v2h16v-2a4 4 0 0 0-4-4H8z"/>
      <circle cx="12" cy="12" r="3"/>
    </svg>
    <div class="fab-pulse"></div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const visible = computed(() => route.path !== '/ai')
</script>

<style scoped>
.ai-fab {
  position: fixed;
  right: 28px;
  bottom: 36px;
  z-index: 1000;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: var(--c-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 20px rgba(22, 122, 105, 0.32);
  transition: all 0.3s ease;
}

.ai-fab:hover {
  transform: translateY(-3px) scale(1.05);
  box-shadow: 0 8px 28px rgba(22, 122, 105, 0.4);
}

.ai-fab svg {
  width: 22px;
  height: 22px;
  position: relative;
  z-index: 1;
}

.fab-pulse {
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  border: 2px solid var(--c-primary);
  opacity: 0;
  animation: fab-pulse 2s ease-out infinite;
}

@keyframes fab-pulse {
  0% { transform: scale(1); opacity: 0.5; }
  100% { transform: scale(1.4); opacity: 0; }
}
</style>
