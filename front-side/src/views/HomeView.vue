<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCityStore } from '../stores/city'
import { getHomeData } from '../api/home'
import type { HomeData } from '../types'
import FooterBar from '../components/FooterBar.vue'

const router = useRouter()
const cityStore = useCityStore()

// ===== 状态 =====
const loading = ref(true)
const error = ref('')
const homeData = ref<HomeData | null>(null)

// Banner 轮播
const bannerIndex = ref(0)
const bannerPaused = ref(false)
let bannerTimer: ReturnType<typeof setInterval> | null = null

// ===== 计算属性 =====
const banners = () => homeData.value?.banners ?? []
const hotExhibitions = () => homeData.value?.hotExhibitions ?? []
const galleries = () => homeData.value?.galleries ?? []

// ===== 获取首页数据 =====
async function fetchHomeData() {
  loading.value = true
  error.value = ''
  try {
    const res = await getHomeData(cityStore.currentCity || undefined)
    if (res.code === 1) {
      homeData.value = res.data
      // 只有多张轮播图时才启动自动播放
      if ((res.data.banners?.length ?? 0) > 1) {
        startBannerAutoPlay()
      }
    } else {
      error.value = res.msg || '数据加载失败'
    }
  } catch (e: any) {
    error.value = e.message || '网络请求失败，请检查网络连接'
  } finally {
    loading.value = false
  }
}

// ===== Banner 轮播逻辑 =====
function startBannerAutoPlay() {
  stopBannerAutoPlay()
  bannerTimer = setInterval(() => {
    if (!bannerPaused.value && banners().length > 1) {
      bannerIndex.value = (bannerIndex.value + 1) % banners().length
    }
  }, 3000)
}

function stopBannerAutoPlay() {
  if (bannerTimer) {
    clearInterval(bannerTimer)
    bannerTimer = null
  }
}

function goToBanner(index: number) {
  bannerIndex.value = index
}

function prevBanner() {
  const list = banners()
  bannerIndex.value = bannerIndex.value === 0 ? list.length - 1 : bannerIndex.value - 1
}

function nextBanner() {
  const list = banners()
  bannerIndex.value = bannerIndex.value === list.length - 1 ? 0 : bannerIndex.value + 1
}

// ===== 路由跳转 =====
function goToExhibition(id: number) {
  router.push(`/exhibition/${id}`)
}

function goToGallery(id: number) {
  router.push(`/gallery/${id}`)
}

function goToExhibitions() {
  router.push('/exhibitions')
}

function goToGalleries() {
  router.push('/galleries')
}

// ===== 重试 =====
function retry() {
  fetchHomeData()
}

// ===== 生命周期 =====
onMounted(() => {
  fetchHomeData()
})

onUnmounted(() => {
  stopBannerAutoPlay()
})
</script>

<template>
  <div class="home-page">
    <!-- ===== 加载态 ===== -->
    <div v-if="loading" class="status-container">
      <div class="spinner"></div>
      <p class="status-text">加载中...</p>
    </div>

    <!-- ===== 错误态 ===== -->
    <div v-else-if="error" class="status-container">
      <p class="status-text error-text">{{ error }}</p>
      <button class="retry-btn" @click="retry">重试</button>
    </div>

    <!-- ===== 空数据态 ===== -->
    <div v-else-if="!homeData || (!banners().length && !hotExhibitions().length && !galleries().length)" class="status-container">
      <p class="status-text">暂无展览内容，敬请期待</p>
    </div>

    <!-- ===== 正常内容 ===== -->
    <template v-else>
      <main class="home-main">
        <!-- Banner 轮播区 -->
        <section v-if="banners().length" class="banner-section"
          @mouseenter="bannerPaused = true"
          @mouseleave="bannerPaused = false"
        >
          <div class="banner-viewport">
            <div
              class="banner-track"
              :style="{ transform: `translateX(-${bannerIndex * 100}%)` }"
            >
              <div
                v-for="banner in banners()"
                :key="banner.id"
                class="banner-slide"
              >
                <img
                  :src="banner.imageUrl"
                  :alt="banner.title"
                  class="banner-img"
                />
                <div class="banner-caption">
                  <h2>{{ banner.title }}</h2>
                </div>
              </div>
            </div>

            <!-- 左右箭头 -->
            <button v-if="banners().length > 1" class="banner-arrow left" @click="prevBanner">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="15 18 9 12 15 6"/>
              </svg>
            </button>
            <button v-if="banners().length > 1" class="banner-arrow right" @click="nextBanner">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="9 18 15 12 9 6"/>
              </svg>
            </button>

            <!-- 圆点指示器 -->
            <div v-if="banners().length > 1" class="banner-dots">
              <button
                v-for="(_, idx) in banners()"
                :key="idx"
                :class="['dot', { active: idx === bannerIndex }]"
                @click="goToBanner(idx)"
              />
            </div>
          </div>
        </section>

        <!-- 当前热门展览 -->
        <section v-if="hotExhibitions().length" class="exhibition-section">
          <div class="section-header">
            <h3 class="section-title">当前热门展览</h3>
            <button class="more-link" @click="goToExhibitions">查看更多 →</button>
          </div>
          <div class="exhibition-scroll">
            <div
              v-for="item in hotExhibitions()"
              :key="item.id"
              class="exhibition-card"
              @click="goToExhibition(item.id)"
            >
              <img
                :src="item.posterImage"
                :alt="item.title"
                class="exhibition-poster"

              />
              <div class="exhibition-info">
                <h4 class="exhibition-title">{{ item.title }}</h4>
                <p class="exhibition-subtitle">{{ item.subtitle }}</p>
                <span class="exhibition-gallery">{{ item.galleryName }}</span>
              </div>
            </div>
          </div>
        </section>

        <!-- 热门美术馆 -->
        <section v-if="galleries().length" class="gallery-section">
          <div class="section-header">
            <h3 class="section-title">热门美术馆</h3>
            <button class="more-link" @click="goToGalleries">查看更多 →</button>
          </div>
          <div class="gallery-grid">
            <div
              v-for="g in galleries()"
              :key="g.id"
              class="gallery-card"
              @click="goToGallery(g.id)"
            >
              <img
                :src="g.coverImage"
                :alt="g.name"
                class="gallery-cover"

              />
              <div class="gallery-info">
                <h4 class="gallery-name">{{ g.name }}</h4>
                <p class="gallery-address">{{ g.address }}</p>
                <span class="gallery-count">{{ g.exhibitionCount }} 场展览</span>
              </div>
            </div>
          </div>
        </section>

        <!-- 路由验证示例区 -->
        <section class="demo-section">
          <div class="section-header">
            <h3 class="section-title">页面导航验证</h3>
          </div>
          <div class="demo-grid">
            <div class="demo-card" @click="goToExhibitions">
              <span class="demo-icon">🎨</span>
              <h4>全部展览</h4>
              <p>浏览所有展览信息</p>
              <span class="demo-route">/exhibitions</span>
            </div>
            <div class="demo-card" @click="goToGalleries">
              <span class="demo-icon">🏛️</span>
              <h4>美术馆</h4>
              <p>发现城市艺术空间</p>
              <span class="demo-route">/galleries</span>
            </div>
            <div class="demo-card" @click="router.push('/home')">
              <span class="demo-icon">🏠</span>
              <h4>首页</h4>
              <p>返回首页</p>
              <span class="demo-route">/home</span>
            </div>
          </div>
        </section>
      </main>
    </template>

    <FooterBar />
  </div>
</template>

<style scoped>
/* ===== 页面容器 ===== */
.home-page {
  flex: 1;
  min-height: 0;
  background: #E8EEF0;
  overflow-y: auto;
}

/* ===== 状态容器（加载/错误/空） ===== */
.status-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 64px);
  gap: 16px;
}

.status-text {
  font-size: 16px;
  color: #666;
}

.error-text {
  color: #e74c3c;
}

/* Spinner */
.spinner {
  width: 36px;
  height: 36px;
  border: 3px solid #e0e0e0;
  border-top-color: #5A5E61;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 重试按钮 */
.retry-btn {
  padding: 10px 32px;
  background: #5A5E61;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.retry-btn:hover {
  background: #4a4e51;
}

/* ===== 主内容区 ===== */
.home-main {
  max-width: 1440px;
  margin: 0 auto;
  padding: 24px 32px 48px;
}

/* ===== Section 通用 ===== */
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.section-title {
  font-size: 22px;
  font-weight: 700;
  color: #333;
  margin: 0;
}

.more-link {
  background: none;
  border: none;
  font-size: 14px;
  color: #C59B27;
  cursor: pointer;
  transition: opacity 0.2s;
}

.more-link:hover {
  opacity: 0.7;
}

/* ===== Banner 轮播 ===== */
.banner-section {
  margin-bottom: 36px;
}

.banner-viewport {
  position: relative;
  width: 100%;
  overflow: hidden;
  border-radius: 12px;
  aspect-ratio: 1200 / 420;
}

.banner-track {
  display: flex;
  transition: transform 0.5s ease;
  height: 100%;
}

.banner-slide {
  min-width: 100%;
  position: relative;
}

.banner-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.banner-caption {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 24px 32px;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.55));
}

.banner-caption h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #fff;
}

/* 箭头 */
.banner-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.3);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.2s;
}

.banner-arrow:hover {
  background: rgba(0, 0, 0, 0.55);
}

.banner-arrow.left { left: 12px; }
.banner-arrow.right { right: 12px; }

/* 圆点 */
.banner-dots {
  position: absolute;
  bottom: 14px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 10px;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.7);
  background: transparent;
  cursor: pointer;
  padding: 0;
  transition: all 0.25s;
}

.dot.active {
  background: #fff;
  border-color: #fff;
}

/* ===== 热门展览卡片 ===== */
.exhibition-section {
  margin-bottom: 36px;
}

.exhibition-scroll {
  display: flex;
  gap: 20px;
  overflow-x: auto;
  padding-bottom: 8px;
  scroll-snap-type: x mandatory;
}

.exhibition-scroll::-webkit-scrollbar {
  height: 6px;
}

.exhibition-scroll::-webkit-scrollbar-thumb {
  background: #ccc;
  border-radius: 3px;
}

.exhibition-card {
  min-width: 280px;
  max-width: 280px;
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  scroll-snap-align: start;
  transition: transform 0.2s, box-shadow 0.2s;
}

.exhibition-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.exhibition-poster {
  width: 100%;
  height: 200px;
  object-fit: cover;
}

.exhibition-info {
  padding: 14px 16px;
}

.exhibition-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0 0 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.exhibition-subtitle {
  font-size: 12px;
  color: #999;
  margin: 0 0 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.exhibition-gallery {
  font-size: 12px;
  color: #C59B27;
  font-weight: 500;
}

/* ===== 美术馆网格 ===== */
.gallery-section {
  margin-bottom: 36px;
}

.gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 20px;
}

.gallery-card {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.gallery-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.gallery-cover {
  width: 100%;
  height: 180px;
  object-fit: cover;
}

.gallery-info {
  padding: 14px 16px;
}

.gallery-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0 0 4px;
}

.gallery-address {
  font-size: 13px;
  color: #888;
  margin: 0 0 8px;
}

.gallery-count {
  font-size: 12px;
  color: #C59B27;
  font-weight: 500;
}

/* ===== 路由验证 Demo 区 ===== */
.demo-section {
  margin-bottom: 24px;
}

.demo-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.demo-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32px 24px;
  background: #fff;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s ease;
  border: 2px solid transparent;
  text-align: center;
}

.demo-card:hover {
  border-color: #C59B27;
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.demo-icon {
  font-size: 40px;
  margin-bottom: 12px;
}

.demo-card h4 {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px;
}

.demo-card p {
  font-size: 13px;
  color: #888;
  margin: 0 0 12px;
}

.demo-route {
  font-size: 11px;
  color: #C59B27;
  font-family: monospace;
  background: #fdf6e9;
  padding: 3px 10px;
  border-radius: 4px;
}
</style>
