<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useCityStore } from '../stores/city'
import logoImage from '../assets/home/LOGO.png'
import localIcon from '../assets/home/local.png'
import personIcon from '../assets/home/person.png'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const cityStore = useCityStore()

/** 导航 Tab 配置 */
const tabs = [
  { label: '首页', path: '/home' },
  { label: '全部展览', path: '/exhibitions' },
  { label: '美术馆', path: '/galleries' },
]

/** 判断当前激活 Tab */
function isActive(path: string): boolean {
  return route.path === path
}

/** 跳转页面 */
function navigateTo(path: string) {
  if (route.path !== path) {
    router.push(path)
  }
}

/** 城市选择 */
function handleCitySelect() {
  router.push('/select-city')
}

/** 个人中心 */
function handleProfile() {
  // TODO: 跳转个人中心页
}

/** 退出登录 */
function handleLogout() {
  auth.logout()
}
</script>

<template>
  <header class="navbar">
    <div class="navbar-inner">
      <!-- ===== 左侧：Logo + 品牌名 ===== -->
      <div class="navbar-left" @click="navigateTo('/home')">
        <img :src="logoImage" alt="logo" class="logo-img" />
      </div>

      <!-- ===== 中间：导航 Tabs ===== -->
      <nav class="navbar-center">
        <button
          v-for="tab in tabs"
          :key="tab.path"
          :class="['nav-tab', { active: isActive(tab.path) }]"
          @click="navigateTo(tab.path)"
        >
          {{ tab.label }}
        </button>
      </nav>

      <!-- ===== 右侧：功能区 ===== -->
      <div class="navbar-right">
        <!-- 城市选择 -->
        <button class="action-btn" @click="handleCitySelect">
          <img :src="localIcon" alt="城市" class="action-icon" />
          <span>{{ cityStore.currentCity || '城市选择' }}</span>
        </button>

        <!-- 个人中心 / 登录注册 -->
        <template v-if="auth.isLoggedIn">
          <button class="action-btn" @click="handleProfile">
            <img :src="personIcon" alt="个人" class="action-icon" />
            <span>个人中心</span>
          </button>
          <button class="logout-btn" @click="handleLogout">退出</button>
        </template>
        <template v-else>
          <button class="action-btn" @click="router.push('/login')">
            <img :src="personIcon" alt="登录" class="action-icon" />
            <span>登录/注册</span>
          </button>
        </template>
      </div>
    </div>
  </header>
</template>

<style scoped>
/* ===== 导航栏容器 ===== */
.navbar {
  width: 100%;
  height: 64px;
  background: #5A5E61;
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.navbar-inner {
  width: 100%;
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
}

/* ===== 左侧品牌区 ===== */
.navbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  flex-shrink: 0;
}

.logo-img {
  
  object-fit: contain;
}

.brand-name {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 1px;
  white-space: nowrap;
}

/* ===== 中间导航 Tabs ===== */
.navbar-center {
  display: flex;
  align-items: center;
  gap: 48px;
}

.nav-tab {
  background: none;
  border: none;
  font-size: 20px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.75);
  cursor: pointer;
  padding: 6px 0;
  position: relative;
  transition: color 0.25s ease;
  white-space: nowrap;
}

.nav-tab::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: 0;
  width: 0;
  height: 2px;
  background: #C59B27;
  transition: width 0.25s ease;
}

.nav-tab:hover {
  color: #fff;
}

.nav-tab:hover::after {
  width: 100%;
}

.nav-tab.active {
  color: #C59B27;
  font-weight: 600;
}

.nav-tab.active::after {
  width: 100%;
}

/* ===== 右侧功能区 ===== */
.navbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

/* 功能区按钮 */
.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.8);
  font-size: 13px;
  cursor: pointer;
  padding: 6px 8px;
  border-radius: 6px;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.action-btn:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.1);
}

.action-icon {
  width: 16px;
  height: 16px;
  object-fit: contain;
}

/* 退出按钮 */
.logout-btn {
  background: none;
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.logout-btn:hover {
  background: rgba(255, 77, 79, 0.7);
  border-color: transparent;
  color: #fff;
}
</style>
