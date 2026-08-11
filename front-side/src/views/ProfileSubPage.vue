<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import FooterBar from '../components/FooterBar.vue'

const router = useRouter()
const route = useRoute()

const page = computed(() => (route.params.page as string) || 'orders')

const config: Record<string, { title: string }> = {
  orders: { title: '我的订单' },
  tickets: { title: '票夹' },
  favorites: { title: '收藏列表' },
  history: { title: '看展记录' },
}

const current = computed(() => config[page.value] || config.orders)
</script>

<template>
  <div class="sub-page">
    <!-- 顶部导航 -->
    <header class="sub-nav">
      <button class="sub-back" @click="router.push('/profile')">&lt; 返回</button>
      <div class="sub-breadcrumb">
        <span class="sub-breadcrumb-dim">个人中心 &gt;</span>
        <span class="sub-breadcrumb-active">{{ current.title }}</span>
      </div>
    </header>

    <div class="sub-content">
      <h1 class="sub-title">{{ current.title }}</h1>
      <p class="sub-placeholder">此页面内容将在后续实现</p>
    </div>

    <FooterBar />
  </div>
</template>

<style scoped>
.sub-page { flex: 1; min-height: 0; background: #E8EEF0; overflow-y: auto; display: flex; flex-direction: column; }

.sub-nav { height: 64px; background: #5A5E61; display: flex; align-items: center; justify-content: space-between; padding: 0 211px 0 77px; flex-shrink: 0; }
.sub-back { background: none; border: none; color: #fff; font-size: 16px; cursor: pointer; padding: 0; transition: opacity 0.2s; }
.sub-back:hover { opacity: 0.7; }
.sub-breadcrumb { display: flex; align-items: center; gap: 6px; font-size: 14px; }
.sub-breadcrumb-dim { color: rgba(255,255,255,0.6); }
.sub-breadcrumb-active { color: #fff; font-weight: 700; }

.sub-content { max-width: 1440px; width: 100%; margin: 0 auto; padding: 48px 40px 80px; }
.sub-title { font-size: 36px; font-weight: 700; color: #000; margin: 0 0 24px; }
.sub-placeholder { font-size: 16px; color: #999; }
</style>
