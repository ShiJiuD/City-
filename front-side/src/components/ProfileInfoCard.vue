<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useCityStore } from '../stores/city'
import localIcon from '../assets/home/local.png'
import profileAvatar from '../assets/profile/Profile-Picture.png'
import changeIcon from '../assets/profile/change.png'

const router = useRouter()
const auth = useAuthStore()
const cityStore = useCityStore()

const nickname = computed(() => auth.user?.displayName || '用户')
const cityName = computed(() => cityStore.currentCity || '南京')
</script>

<template>
  <div class="info-wrapper">
    <h2 class="info-title">个人信息</h2>
    <div class="info-underline"></div>

    <div class="info-card">
      <div class="info-left">
        <img :src="profileAvatar" alt="头像" class="info-avatar" />
        <div class="info-detail">
          <span class="info-nickname">{{ nickname }}</span>
          <div class="info-city">
            <img :src="localIcon" alt="定位" class="info-city-icon" />
            <span>{{ cityName }}</span>
          </div>
          <button class="info-edit-btn" @click="router.push('/profile/edit')">
            编辑个人资料
          </button>
        </div>
      </div>
      <div class="info-right">
        <button class="info-switch-btn">
          <img :src="changeIcon" alt="切换" class="info-switch-icon" />
          <span>切换账号</span>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.info-wrapper {
  width: 1199px;
  margin-bottom: 40px;
}

.info-title {
  font-size: 36px;
  font-weight: 700;
  color: #111;
  margin: 0 0 8px;
}

.info-underline {
  width: 93px;
  height: 3px;
  background: #000;
  margin-bottom: 20px;
}

.info-card {
  width: 1199px;
  height: 200px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 40px;
  box-sizing: border-box;
}

.info-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.info-avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
}

.info-detail {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.info-nickname {
  font-size: 22px;
  font-weight: 700;
  color: #111;
}

.info-city {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: #888;
}

.info-city-icon {
  width: 16px;
  height: 16px;
}

.info-edit-btn {
  width: fit-content;
  padding: 6px 20px;
  background: #fff;
  border: 1px solid #d0d0d0;
  border-radius: 6px;
  font-size: 13px;
  color: #555;
  cursor: pointer;
  transition: background 0.2s;
}

.info-edit-btn:hover {
  background: #f5f5f5;
}

.info-right {
  display: flex;
  align-items: center;
}

.info-switch-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: none;
  border: none;
  font-size: 14px;
  color: #555;
  cursor: pointer;
  padding: 8px;
  transition: color 0.2s;
}

.info-switch-btn:hover {
  color: #111;
}

.info-switch-icon {
  width: 20px;
  height: 20px;
}
</style>
