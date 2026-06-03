<script setup>
import Globalnav from "@/components/global/globalnav.vue";
import AiFloatingButton from "@/components/user/ai/AiFloatingButton.vue";
</script>

<template>
  <Globalnav />
  <router-view></router-view>
  <AiFloatingButton />
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  background-color: #f5f7fa;
  color: #2c3e50;
  min-height: 100vh;
}

#app {
  min-height: 100vh;
}
</style>
