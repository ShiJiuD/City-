<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import NavBar from './components/NavBar.vue'

const route = useRoute()

/** 访客页面（登录/注册/忘记密码）不显示导航栏 */
const showChrome = computed(() => !route.meta.guest)
</script>

<template>
  <NavBar v-if="showChrome" />
  <router-view />
</template>
