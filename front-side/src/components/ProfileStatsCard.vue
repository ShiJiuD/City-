<script setup lang="ts">
</script>

<template>
  <div class="stats-wrapper">
    <div class="stats-card">
      <h2 class="stats-title">数据统计</h2>
      <div class="stats-underline"></div>

      <div class="stats-blocks">
        <div class="stats-block">
          <span class="stats-label">我的收藏</span>
          <span class="stats-number">4</span>
        </div>
        <div class="stats-block">
          <span class="stats-label">有效票夹</span>
          <span class="stats-number">1</span>
        </div>
        <div class="stats-block">
          <span class="stats-label">观展次数</span>
          <span class="stats-number">1</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.stats-wrapper {
  width: 1199px;
  margin-bottom: 40px;
}

/* 卡片 1199×423 */
.stats-card {
  width: 1199px;
  height: 423px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 24px 0 0;
  box-sizing: border-box;
  position: relative;
}

/* 标题在卡片内部左上角 */
.stats-title {
  font-size: 36px;
  font-weight: 700;
  color: #111;
  margin: 0 0 8px 40px;
}

.stats-underline {
  width: 93px;
  height: 3px;
  background: #000;
  margin-left: 40px;
  margin-bottom: 40px;
}

/* 三个小方块居中 */
.stats-blocks {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 48px;
}

.stats-block {
  width: 280px;
  height: 258px;
  background: #EBEBEB;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.stats-label {
  font-size: 18px;
  color: #555;
}

.stats-number {
  font-size: 72px;
  font-weight: 700;
  color: #111;
  line-height: 1;
}
</style>
