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
#app {
  min-height: 100vh;
}
</style>
