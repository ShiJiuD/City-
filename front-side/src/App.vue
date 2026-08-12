<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import NavBar from './components/NavBar.vue'

const route = useRoute()

/** 访客页面（登录/注册/忘记密码）以及 hideNav 页面不显示导航栏 */
const showChrome = computed(() => !route.meta.guest && !route.meta.hideNav)
</script>

<template>
  <NavBar v-if="showChrome" />
  <router-view />
</template>
