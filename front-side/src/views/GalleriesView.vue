<script setup lang="ts">
import FooterBar from '../components/FooterBar.vue'
</script>

<template>
  <div class="galleries-page">
    <header class="page-header">
      <h1>美术馆</h1>
      <p class="page-desc">发现城市中的艺术空间</p>
    </header>
    <main class="page-main">
      <div class="placeholder-card">
        <span class="placeholder-icon">🏛️</span>
        <p>美术馆列表加载中，敬请期待...</p>
      </div>
    </main>
    <FooterBar />
  </div>
</template>

<style scoped>
.galleries-page {
  flex: 1;
  min-height: 0;
  background: #E8EEF0;
  overflow-y: auto;
}

.page-header {
  padding: 32px 40px 16px;
}

.page-header h1 {
  font-size: 28px;
  font-weight: 700;
  color: #333;
  margin: 0;
}

.page-desc {
  font-size: 14px;
  color: #888;
  margin: 8px 0 0;
}

.page-main {
  padding: 20px 40px;
}

.placeholder-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.placeholder-card p {
  font-size: 16px;
  color: #999;
}
</style>
