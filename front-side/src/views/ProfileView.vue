<script setup lang="ts">
import { useAuthStore } from '../stores/auth'
import FooterBar from '../components/FooterBar.vue'
import ProfileInfoCard from '../components/ProfileInfoCard.vue'
import ProfileStatsCard from '../components/ProfileStatsCard.vue'
import ProfileFeaturesCard from '../components/ProfileFeaturesCard.vue'

const auth = useAuthStore()

function handleLogout() {
  auth.logout()
}
</script>

<template>
  <div class="profile-page">
    <div class="profile-content">

      <!-- 1. 个人信息 -->
      <ProfileInfoCard />

      <!-- 2. 数据统计 -->
      <ProfileStatsCard />

      <!-- 3. 我的功能 -->
      <ProfileFeaturesCard />

      <!-- 4. 退出登录 -->
      <button class="logout-btn" @click="handleLogout">退出登录</button>

    </div>

    <FooterBar />
  </div>
</template>

<style scoped>
.profile-page {
  flex: 1;
  min-height: 0;
  background: #E8EEF0;
  overflow-y: auto;
}

.profile-content {
  max-width: 1440px;
  margin: 0 auto;
  padding: 48px 40px 60px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* 退出登录 */
.logout-btn {
  width: 1199px;
  height: 60px;
  background: #fff;
  border: none;
  border-radius: 30px;
  font-size: 18px;
  color: #333;
  cursor: pointer;
  transition: background 0.2s, transform 0.2s;
}

.logout-btn:hover {
  background: #e8e8e8;
  transform: scale(1.01);
}
</style>
