<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useCityStore } from '../stores/city'
import { getHomeData } from '../api/home'
import type { HomeData } from '../types'
import FooterBar from '../components/FooterBar.vue'

// ===== 本地 Banner 图片 =====
import banner01_1 from '../assets/home/banner01-1.png'
import banner01_2 from '../assets/home/banner01-2.jpg'
import banner01_3 from '../assets/home/banner01-3.png'
import banner02_1 from '../assets/home/banner02-1.png'
import banner02_2 from '../assets/home/banner02-2.png'
import banner02_3 from '../assets/home/banner02-3.png'
import banner03_1 from '../assets/home/banner03-1.png'
import banner03_2 from '../assets/home/banner03-2.png'
import banner03_3 from '../assets/home/banner03-3.png'
import banner03_4 from '../assets/home/banner03-4.png'
import banner04_1 from '../assets/home/banner04-1.png'
import banner04_2 from '../assets/home/banner04-2.jpg'
import banner04_3 from '../assets/home/banner04-3.png'
import banner04_4 from '../assets/home/banner04-4.png'

/* eslint-disable @typescript-eslint/no-explicit-any */
declare var Swiper: any

const router = useRouter()
const cityStore = useCityStore()

// ===== 状态 =====
const loading = ref(true)
const error = ref('')
const homeData = ref<HomeData | null>(null)
const homePage = ref<HTMLElement | null>(null)

// ===== 自定义慢速强制滚动捕捉 =====
let isScrolling = false
const SNAP_DURATION = 900 // 动画时长(ms)，越大越慢

function easeInOutCubic(t: number): number {
  return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2
}

function smoothScrollTo(target: number, duration: number) {
  const container = homePage.value
  if (!container) return
  const start = container.scrollTop
  const distance = target - start
  const startTime = performance.now()

  isScrolling = true

  function step(currentTime: number) {
    const elapsed = currentTime - startTime
    const progress = Math.min(elapsed / duration, 1)
    const eased = easeInOutCubic(progress)
    container!.scrollTop = start + distance * eased

    if (progress < 1) {
      requestAnimationFrame(step)
    } else {
      isScrolling = false
    }
  }

  requestAnimationFrame(step)
}

function handleWheel(e: WheelEvent) {
  const container = homePage.value
  if (!container) return
  const bannerHeight = container.clientHeight // Banner 占满视口
  const scrollTop = container.scrollTop

  // 向下滚动：在 Banner 区域 → 滑到内容区
  if (e.deltaY > 0 && scrollTop < bannerHeight - 10) {
    e.preventDefault()
    if (!isScrolling) {
      smoothScrollTo(bannerHeight, SNAP_DURATION)
    }
    return
  }

  // 向上滚动：在内容区顶部 → 滑回 Banner
  if (e.deltaY < 0 && scrollTop <= bannerHeight + 10 && scrollTop > 0) {
    e.preventDefault()
    if (!isScrolling) {
      smoothScrollTo(0, SNAP_DURATION)
    }
    return
  }
}

// Swiper 实例
let bannerSwiper: any = null
let coverflowSwiper: any = null
let recommendSwiper: any = null

// ===== 三个 Swiper 本地数据 =====
// Swiper 1：顶部 Banner
const bannerList = [
  { id: 1, imageUrl: banner01_1 },
  { id: 2, imageUrl: banner01_2 },
  { id: 3, imageUrl: banner01_3 },
]

// Swiper 2：热门展览 图片列表
const coverflowList = [
  { id: 1, posterImage: banner02_1 },
  { id: 2, posterImage: banner02_2 },
  { id: 3, posterImage: banner02_3 },
]

// Swiper 3：艺览智荐
const recommendList = [
  { id: 1, poster: banner03_1 },
  { id: 2, poster: banner03_2 },
  { id: 3, poster: banner03_3 },
  { id: 4, poster: banner03_4 },
]

// 热门美术馆本地数据
const galleryList = [
  { id: 1, cover: banner04_1, name: '江苏省美术馆', address: '南京·秦淮区四条巷12号', count: 3 },
  { id: 2, cover: banner04_2, name: '四方美术馆', address: '南京·浦口区珍七路9号', count: 2 },
  { id: 3, cover: banner04_3, name: '德基美术馆', address: '南京·玄武区中山路18号', count: 5 },
  { id: 4, cover: banner04_4, name: '金鹰美术馆', address: '南京·建邺区应天大街888号', count: 1 },
]

// ===== 计算属性：API 数据驱动结构 + 本地图片展示 =====

/** Banner：API 数据驱动，本地图片展示。只有 API 未加载时才回退 */
const displayBanners = computed(() => {
  if (homeData.value) {
    const list = homeData.value.banners || []
    return list.map((b, i) => ({
      id: b.id,
      imageUrl: bannerList[i % bannerList.length].imageUrl,
      title: b.title,
    }))
  }
  return bannerList.map((b) => ({ ...b, title: '' }))
})

/** 热门展览：API 数据驱动，本地图片展示。只有 API 未加载时才回退 */
const displayHotExhibitions = computed(() => {
  if (homeData.value) {
    const list = homeData.value.hotExhibitions || []
    return list.map((ex, i) => ({
      id: ex.id,
      posterImage: coverflowList[i % coverflowList.length].posterImage,
      title: ex.title,
      subtitle: ex.subtitle,
      galleryName: ex.galleryName,
      type: ex.type,
    }))
  }
  return coverflowList.map((ex) => ({
    ...ex,
    subtitle: '',
    galleryName: '',
    type: 0,
  }))
})

/** 热门美术馆：API 数据驱动，本地图片展示。只有 API 未加载时才回退 */
const displayGalleries = computed(() => {
  if (homeData.value) {
    const list = homeData.value.galleries || []
    return list.map((g, i) => ({
      id: g.id,
      cover: galleryList[i % galleryList.length].cover,
      name: g.name,
      address: g.address,
      count: g.exhibitionCount,
    }))
  }
  return galleryList
})

// ===== 获取首页数据 =====
async function fetchHomeData() {
  loading.value = true
  error.value = ''
  try {
    const res = await getHomeData(cityStore.currentCity || undefined)
    if (res.code === 1) {
      homeData.value = res.data
    } else {
      error.value = res.msg || '数据加载失败'
    }
  } catch (e: any) {
    error.value = e.message || '网络请求失败，请检查网络连接'
  } finally {
    loading.value = false
  }
}

// ===== 初始化 Swiper =====
function initSwipers() {
  // 区域一：Banner Swiper（本地图片）
  bannerSwiper = new Swiper('.banner-swiper', {
    slidesPerView: 1,
    loop: true,
    autoplay: { delay: 3000, disableOnInteraction: false },
    pagination: {
      el: '.banner-pagination',
      clickable: true,
    },
    observer: true,
    observeParents: true,
  })

  // 区域二：热门展览 Coverflow 层叠
  coverflowSwiper = new Swiper('.coverflow-swiper', {
    effect: 'coverflow',
    centeredSlides: true,
    slidesPerView: 'auto',
    coverflowEffect: {
      rotate: 0,
      stretch: 40,
      depth: 300,
      modifier: 1.2,
      slideShadows: false,
    },
    watchSlidesProgress: true,
  })

  // 区域三：艺览智荐 双列轮播
  recommendSwiper = new Swiper('.recommend-swiper', {
    slidesPerView: 2,
    spaceBetween: 16,
    loop: true,
    autoplay: { delay: 3500, disableOnInteraction: false },
  })
}

function destroySwipers() {
  bannerSwiper?.destroy(true, true)
  coverflowSwiper?.destroy(true, true)
  recommendSwiper?.destroy(true, true)
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

// 城市切换时重新请求
watch(() => cityStore.currentCity, () => {
  if (cityStore.currentCity) {
    fetchHomeData()
  }
})

// 数据加载完成后初始化 Swiper + 注册滚动事件
watch(loading, async (val) => {
  if (!val && !error.value && homeData.value) {
    await nextTick()
    initSwipers()
    homePage.value?.addEventListener('wheel', handleWheel, { passive: false })
  }
})

onUnmounted(() => {
  destroySwipers()
  homePage.value?.removeEventListener('wheel', handleWheel)
})
</script>

<template>
  <div ref="homePage" class="home-page">
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

    <!-- ===== 正常内容 ===== -->
    <template v-else>
      <main class="home-main">
        <!-- ===== 区域一：顶部主 Banner Swiper（本地图片） ===== -->
        <section class="banner-section">
          <div class="swiper banner-swiper">
            <div class="swiper-wrapper">
              <div
                v-for="banner in displayBanners"
                :key="banner.id"
                class="swiper-slide"
              >
                <img
                  :src="banner.imageUrl"
                  alt="banner"
                  class="banner-img"
                />
              </div>
            </div>
            <div class="banner-pagination"></div>
          </div>
        </section>

        <!-- ===== 首屏以下内容容器 ===== -->
        <div class="home-content">

        <!-- ===== 区域二：当前热门展览 Coverflow ===== -->
        <section class="coverflow-section">
          <div class="coverflow-header">
            <h3 class="coverflow-heading">当前热门展览</h3>
            <div class="coverflow-sub-wrap">
              <div class="coverflow-line"></div>
              <span class="coverflow-sub">EXHIBITION</span>
            </div>
          </div>
          <div class="swiper coverflow-swiper">
            <div class="swiper-wrapper">
              <div
                v-for="item in displayHotExhibitions"
                :key="item.id"
                class="swiper-slide coverflow-slide"
              >
                <img
                  :src="item.posterImage"
                  alt="展览"
                  class="coverflow-img"
                />
              </div>
            </div>
          </div>
        </section>

        <!-- ===== 区域三：艺览智荐 多列轮播 ===== -->
        <section class="recommend-section">
          <div class="recommend-header">
            <div class="recommend-left">
              <span class="recommend-label">RECOMMEND</span>
              <h3 class="recommend-title">艺览智荐</h3>
              <p class="recommend-desc">基于AI个性化推荐展览</p>
            </div>
            <button class="recommend-more">查看更多 &gt;</button>
          </div>
          <div class="swiper recommend-swiper">
            <div class="swiper-wrapper">
              <div
                v-for="item in recommendList"
                :key="item.id"
                class="swiper-slide recommend-slide"
              >
                <img
                  :src="item.poster"
                  alt="推荐"
                  class="recommend-img"
                />
              </div>
            </div>
          </div>
        </section>

        <!-- 热门美术馆 -->
        <section class="gallery-section">
          <div class="gallery-header">
            <div class="gallery-title-area">
              <h3 class="gallery-title">热门美术馆</h3>
              <div class="gallery-line"></div>
              <span class="gallery-sub">GALLERY</span>
            </div>
            <button class="gallery-more" @click="goToGalleries">查看更多 &gt;</button>
          </div>
          <div class="gallery-grid">
            <div
              v-for="g in displayGalleries"
              :key="g.id"
              class="gallery-card"
            >
              <img
                :src="g.cover"
                :alt="g.name"
                class="gallery-cover"
              />
              <div class="gallery-info">
                <h4 class="gallery-name">{{ g.name }}</h4>
                <p class="gallery-address">{{ g.address }}</p>
                <p class="gallery-count">{{ g.count }} 场展览</p>
                <button class="gallery-detail" @click="goToGallery(g.id)">查看详情</button>
              </div>
            </div>
          </div>
        </section>

        </div>
        <!-- ===== .home-content 结束 ===== -->
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
  /* 去除容器约束，让 Banner 全屏满铺 */
}

/* ===== 首屏以下内容容器 ===== */
.home-content {
  max-width: 1440px;
  margin: 0 auto;
  padding: 36px 32px 48px;
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

/* ===== 区域一：Banner Swiper（全屏沉浸式） ===== */
.banner-section {
  width: 100%;
  height: calc(100vh - 64px); /* NavBar 64px + Banner = 100vh 精准满屏 */
  overflow: hidden;
}

.banner-swiper {
  width: 100%;
  height: 100%;
  overflow: hidden;
  position: relative; /* 为分页器绝对定位提供锚点 */
}

/* Swiper Slide 和内部图片：强制填满 + 居中裁切 */
.banner-swiper .swiper-slide {
  width: 100%;
  height: 100%;
}

.banner-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  display: block;
}

/* Swiper 分页器：悬浮在图片上方 */
:deep(.banner-pagination) {
  position: absolute;
  bottom: 24px !important;
  left: 50% !important;
  transform: translateX(-50%);
  width: auto !important;
  z-index: 10;
}

:deep(.banner-pagination .swiper-pagination-bullet) {
  width: 10px;
  height: 10px;
  background: rgba(255, 255, 255, 0.5);
  opacity: 1;
  margin: 0 6px;
  transition: background 0.3s;
}

:deep(.banner-pagination .swiper-pagination-bullet-active) {
  background: #fff;
}

/* ===== 区域二：热门展览 Coverflow 层叠 ===== */
.coverflow-section {
  margin-bottom: 36px;
}

/* 居中标题 + 横线 + 英文 */
.coverflow-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 24px;
}

.coverflow-heading {
  font-size: 32px;
  font-weight: 400;
  color: #333;
  margin: 0 0 10px;
}

.coverflow-sub-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.coverflow-line {
  width: 100%;
  height: 2px;
  background: #000;
  margin-bottom: 10px;
}

.coverflow-sub {
  font-size: 20px;
  font-weight: 400;
  color: #000;
  letter-spacing: 6px;
}

.coverflow-swiper {
  padding: 30px 0 40px;
}

.coverflow-slide {
  width: 520px;
}

.coverflow-img {
  width: 100%;
  height: 480px;
  object-fit: cover;
  display: block;
  border-radius: 8px;
}

.coverflow-slide:not(.swiper-slide-active) {
  opacity: 0.55;
}

/* ===== 区域三：艺览智荐 双列轮播 ===== */
.recommend-section {
  margin-bottom: 36px;
}

/* 左右两端对齐标题 */
.recommend-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 24px;
}

.recommend-left {
  display: flex;
  flex-direction: column;
}

.recommend-label {
  font-size: 12px;
  color: #000;
  letter-spacing: 4px;
  margin-bottom: 4px;
}

.recommend-title {
  font-size: 28px;
  font-weight: 700;
  color: #000;
  margin: 0 0 6px;
  text-decoration: underline;
  text-underline-offset: 6px;
  text-decoration-color: #000;
  text-decoration-thickness: 2px;
}

.recommend-desc {
  font-size: 13px;
  color: #000;
  margin: 0;
}

.recommend-more {
  background: none;
  border: none;
  font-size: 20px;
  color: #888;
  cursor: pointer;
  padding: 6px 0;
  transition: color 0.25s, transform 0.25s;
  flex-shrink: 0;
}

.recommend-more:hover {
  color: #333;
  transform: translateX(3px);
}

.recommend-slide {
  width: 540px;
}

.recommend-img {
  width: 540px;
  height: 811px;
  object-fit: cover;
  display: block;
  border-radius: 8px;
}

/* ===== 热门美术馆 ===== */
.gallery-section {
  margin-bottom: 36px;
}

/* Header */
.gallery-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 24px;
}

.gallery-title-area {
  display: flex;
  flex-direction: column;
}

.gallery-title {
  font-size: 28px;
  font-weight: 700;
  color: #000;
  margin: 0 0 6px;
}

.gallery-line {
  width: 100%;
  height: 2px;
  background: #000;
  margin-bottom: 6px;
}

.gallery-sub {
  font-size: 13px;
  color: #000;
  letter-spacing: 3px;
}

.gallery-more {
  background: none;
  border: none;
  font-size: 20px;
  color: #888;
  cursor: pointer;
  transition: color 0.25s, transform 0.25s;
  flex-shrink: 0;
}

.gallery-more:hover {
  color: #333;
  transform: translateX(3px);
}

/* 2×2 网格 */
.gallery-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  column-gap: 44px;
  row-gap: 54px;
}

/* 卡片 */
.gallery-card {
  width: 580px;
  height: 680px;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: transform 0.25s, box-shadow 0.25s;
}

.gallery-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.12);
}

.gallery-cover {
  width: 580px;
  height: 500px;
  object-fit: cover;
  display: block;
}

.gallery-info {
  padding: 14px 20px 16px;
  display: flex;
  flex-direction: column;
  height: 180px;
}

.gallery-name {
  font-size: 18px;
  font-weight: 700;
  color: #000;
  margin: 0 0 4px;
}

.gallery-address {
  font-size: 13px;
  color: #000;
  margin: 0 0 2px;
}

.gallery-count {
  font-size: 13px;
  color: #000;
  margin: 0;
}

.gallery-detail {
  background: none;
  border: none;
  font-size: 13px;
  color: #888;
  cursor: pointer;
  align-self: flex-end;
  margin-top: auto;
  padding: 0;
  transition: color 0.2s;
}

.gallery-detail:hover {
  color: #000;
}
</style>
