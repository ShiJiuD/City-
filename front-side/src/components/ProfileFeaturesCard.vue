<script setup lang="ts">
import { useRouter } from 'vue-router'
import orderIcon from '../assets/profile/order.png'
import ticketIcon from '../assets/profile/ticket.png'
import loveIcon from '../assets/profile/love.png'
import timeIcon from '../assets/profile/time.png'

const router = useRouter()

const features = [
  { icon: orderIcon, title: '我的订单', sub: '查看和管理您的所有订单', page: 'orders' },
  { icon: ticketIcon, title: '票夹', sub: '存放您的有效电子票', page: 'tickets' },
  { icon: loveIcon, title: '收藏列表', sub: '您收藏的展览与艺术馆', page: 'favorites' },
  { icon: timeIcon, title: '看展记录', sub: '查看您的观展记录', page: 'history' },
]

function goTo(page: string) {
  router.push(`/profile/${page}`)
}
</script>

<template>
  <div class="features-wrapper">
    <div class="features-card">
      <h2 class="features-title">我的功能</h2>
      <div class="features-underline"></div>

      <div class="features-bars">
        <div
          v-for="(f, idx) in features"
          :key="idx"
          class="feature-bar"
          @click="goTo(f.page)"
        >
          <img :src="f.icon" alt="" class="feature-icon" />
          <div class="feature-text">
            <span class="feature-name">{{ f.title }}</span>
            <span class="feature-sub">{{ f.sub }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.features-wrapper {
  width: 1199px;
  margin-bottom: 40px;
}

/* 卡片 1199×803 */
.features-card {
  width: 1199px;
  height: 803px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 24px 0 0;
  box-sizing: border-box;
  position: relative;
}

/* 标题在卡片内部左上角 */
.features-title {
  font-size: 36px;
  font-weight: 700;
  color: #111;
  margin: 0 0 8px 40px;
}

.features-underline {
  width: 93px;
  height: 3px;
  background: #000;
  margin-left: 40px;
  margin-bottom: 28px;
}

/* 长条列表 */
.features-bars {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
}

.feature-bar {
  width: 1128px;
  height: 152px;
  background: #F7F7F7;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 0 36px;
  box-sizing: border-box;
  cursor: pointer;
  transition: background 0.2s;
}

.feature-bar:hover {
  background: #eee;
}

.feature-icon {
  width: 64px;
  height: 64px;
  flex-shrink: 0;
}

.feature-text {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.feature-name {
  font-size: 20px;
  font-weight: 700;
  color: #111;
}

.feature-sub {
  font-size: 14px;
  color: #888;
}
</style>
