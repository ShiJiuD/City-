<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import FooterBar from '../components/FooterBar.vue'
import Pagination from '../components/Pagination.vue'
import { getFavoriteList, getFavoriteCount, cancelFavorite } from '../api/favorite'
import type { FavoriteListVO, FavoriteCountVO } from '../api/favorite'
import localIcon from '../assets/home/local.png'
import all1 from '../assets/home/all1.jpg'
import all2 from '../assets/home/all2.jpg'
import all3 from '../assets/home/all3.png'
import all4 from '../assets/home/all4.png'
import all5 from '../assets/home/all5.jpg'
import all6 from '../assets/home/all6.jpg'

const router = useRouter()
const posters = [all1, all2, all3, all4, all5, all6]

// Tab: 1=展览 2=美术馆
const activeTab = ref(1)
const counts = ref<FavoriteCountVO>({ exhibitionCount: 0, galleryCount: 0, totalCount: 0 })

const loading = ref(true)
const list = ref<FavoriteListVO[]>([])
const currentPage = ref(1)
const pageSize = 4
const total = ref(0)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

async function fetchCounts() {
  try {
    const res = await getFavoriteCount()
    if (res.code === 1) counts.value = res.data
  } catch { /* ignore */ }
}

async function fetchList() {
  loading.value = true
  try {
    const res = await getFavoriteList({
      targetType: activeTab.value,
      page: currentPage.value,
      pageSize,
    })
    if (res.code === 1) {
      list.value = res.data.records
      total.value = res.data.total
    }
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function switchTab(tab: number) {
  if (activeTab.value === tab) return
  activeTab.value = tab
  currentPage.value = 1
}

async function handleCancel(item: FavoriteListVO) {
  try {
    const res = await cancelFavorite({ targetType: activeTab.value, targetId: item.targetId })
    if (res.code === 1) {
      fetchList()
      fetchCounts()
    }
  } catch { /* ignore */ }
}

function goToPage(page: number) {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
  }
}

function formatPrice(price?: number): string {
  if (!price || price === 0) return '免费'
  return `¥ ${price}`
}

// 展览状态：1=开放中, 2=即将结束, 3=已结束
function getStatusLabel(item: FavoriteListVO): { text: string; cls: string } | null {
  if (activeTab.value !== 1 || !item.startDate || !item.endDate) return null
  const now = new Date()
  const end = new Date(item.endDate)
  const start = new Date(item.startDate)
  if (now > end) return { text: '已结束', cls: 'badge-ended' }
  const daysLeft = Math.ceil((end.getTime() - now.getTime()) / (1000 * 60 * 60 * 24))
  if (daysLeft <= 7) return { text: '即将结束', cls: 'badge-closing' }
  if (now >= start) return { text: '开放中', cls: 'badge-open' }
  return null
}

// 展览卡片底栏文字
function getExhibitionExtra(item: FavoriteListVO): string {
  if (item.startDate && item.endDate) return `时间：${item.startDate} — ${item.endDate}`
  return ''
}

// 美术馆卡片底栏文字
function getGalleryExtra(item: FavoriteListVO): string {
  return `正在展出：${item.exhibitionCount ?? 0}场`
}

watch(activeTab, () => fetchList())
watch(currentPage, () => fetchList())

onMounted(() => {
  fetchCounts()
  fetchList()
})
</script>

<template>
  <div class="fav-page">
    <!-- 顶部导航 -->
    <header class="sub-nav">
      <button class="sub-back" @click="router.push('/profile')">&lt; 返回</button>
      <div class="sub-breadcrumb">
        <span class="sub-breadcrumb-dim">个人中心 &gt;</span>
        <span class="sub-breadcrumb-active">收藏列表</span>
      </div>
    </header>

    <!-- 标题 -->
    <div class="fav-header">
      <h1 class="fav-title">收藏列表</h1>
      <p class="fav-subtitle">收藏您感兴趣的展览和美术馆</p>
    </div>

    <!-- Tab 切换 -->
    <div class="fav-tabs-wrapper">
      <div class="fav-tabs">
        <button :class="['fav-tab', { active: activeTab === 1 }]" @click="switchTab(1)">
          <span>展览</span>
          <span :class="['fav-badge', { active: activeTab === 1 }]">{{ counts.exhibitionCount }}</span>
        </button>
        <button :class="['fav-tab', { active: activeTab === 2 }]" @click="switchTab(2)">
          <span>美术馆</span>
          <span :class="['fav-badge', { active: activeTab === 2 }]">{{ counts.galleryCount }}</span>
        </button>
      </div>
      <div class="fav-tabs-line"></div>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="status-wrap">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- 空状态 -->
    <div v-else-if="list.length === 0" class="status-wrap">
      <p class="empty-text">暂无收藏</p>
    </div>

    <!-- 卡片列表 -->
    <div v-else class="fav-content">
      <div class="fav-grid">
        <div v-for="(item, idx) in list" :key="item.id" class="fav-card">
          <!-- 图片区 616×389 -->
          <div class="card-image">
            <img :src="posters[idx % posters.length]" alt="" class="card-img" />
            <span v-if="getStatusLabel(item)" :class="['card-badge', getStatusLabel(item)!.cls]">
              {{ getStatusLabel(item)!.text }}
            </span>
          </div>

          <!-- 文字区 195px -->
          <div class="card-body">
            <div class="card-top">
              <!-- 展览: title + galleryName -->
              <template v-if="activeTab === 1">
                <p class="card-title">展览：{{ item.title }}</p>
                <p v-if="item.galleryName" class="card-sub">美术馆：{{ item.galleryName }}</p>
              </template>
              <!-- 美术馆: name + address -->
              <template v-else>
                <p class="card-title">{{ item.name }}</p>
                <p v-if="item.address" class="card-sub">
                  <img :src="localIcon" alt="" class="card-loc" />
                  {{ item.address }}
                </p>
              </template>
            </div>

            <div class="card-bottom">
              <span class="card-extra">
                {{ activeTab === 1 ? getExhibitionExtra(item) : getGalleryExtra(item) }}
              </span>
              <span v-if="activeTab === 1" class="card-price">{{ formatPrice(item.price) }}</span>
              <button v-else class="card-link">查看详情 &gt;</button>
            </div>
          </div>

          <!-- 取消收藏 -->
          <button class="card-remove" @click="handleCancel(item)">✕</button>
        </div>
      </div>

      <!-- 翻页 -->
      <Pagination :current-page="currentPage" :total-pages="totalPages" @page-change="goToPage" />
    </div>

    <FooterBar />
  </div>
</template>

<style scoped>
.fav-page { flex: 1; min-height: 0; background: #E8EEF0; overflow-y: auto; display: flex; flex-direction: column; }

/* ===== 顶栏 ===== */
.sub-nav { height: 64px; background: #5A5E61; display: flex; align-items: center; justify-content: space-between; padding: 0 211px 0 77px; flex-shrink: 0; }
.sub-back { background: none; border: none; color: #fff; font-size: 16px; cursor: pointer; padding: 0; }
.sub-back:hover { opacity: 0.7; }
.sub-breadcrumb { display: flex; align-items: center; gap: 6px; font-size: 14px; }
.sub-breadcrumb-dim { color: rgba(255,255,255,0.6); }
.sub-breadcrumb-active { color: #fff; font-weight: 700; }

/* ===== 标题 ===== */
.fav-header { max-width: 1440px; width: 100%; margin: 0 auto; padding: 48px 40px 0; }
.fav-title { font-size: 36px; font-weight: 700; color: #000; margin: 0; }
.fav-subtitle { font-size: 14px; color: #888; margin: 8px 0 0; }

/* ===== Tab ===== */
.fav-tabs-wrapper { max-width: 1440px; width: 100%; margin: 24px auto 0; padding: 0 40px; }
.fav-tabs { display: flex; gap: 32px; }
.fav-tab { display: flex; align-items: center; gap: 8px; background: none; border: none; font-size: 18px; color: #999; cursor: pointer; padding: 0 0 8px; position: relative; }
.fav-tab.active { color: #000; font-weight: 700; }
.fav-tab.active::after { content: ''; position: absolute; bottom: -1px; left: 0; right: 0; height: 2px; background: #000; z-index: 2; }
.fav-badge { display: inline-flex; align-items: center; justify-content: center; min-width: 22px; height: 22px; border-radius: 11px; font-size: 12px; font-weight: 700; color: #fff; background: #bbb; padding: 0 6px; }
.fav-badge.active { background: #000; }
.fav-tabs-line { width: 100%; height: 1px; background: #D1D5DB; margin-top: -1px; }

/* ===== 状态 ===== */
.status-wrap { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 300px; gap: 16px; }
.spinner { width: 36px; height: 36px; border: 3px solid #e0e0e0; border-top-color: #5A5E61; border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.empty-text { font-size: 16px; color: #999; }

/* ===== 内容 ===== */
.fav-content { max-width: 1440px; width: 100%; margin: 0 auto; padding: 32px 40px 80px; }
.fav-grid { display: grid; grid-template-columns: repeat(2, 616px); gap: 24px; justify-content: center; }

/* ===== 卡片 616×584 ===== */
.fav-card { width: 616px; height: 584px; background: #fff; border-radius: 8px; overflow: hidden; position: relative; transition: transform 0.3s, box-shadow 0.3s; }
.fav-card:hover { transform: translateY(-4px); box-shadow: 0 8px 28px rgba(0,0,0,0.12); }

.card-image { width: 616px; height: 389px; position: relative; overflow: hidden; }
.card-img { width: 100%; height: 100%; object-fit: cover; display: block; }

.card-badge { position: absolute; top: 12px; left: 12px; z-index: 2; padding: 4px 10px; border-radius: 4px; font-size: 12px; color: #fff; }
.badge-open { background: #1a3a5c; }
.badge-closing { background: #e74c3c; }
.badge-ended { background: #888; }

.card-body { height: 195px; padding: 16px 20px; display: flex; flex-direction: column; justify-content: space-between; box-sizing: border-box; }
.card-top { display: flex; flex-direction: column; gap: 4px; }
.card-title { font-size: 18px; font-weight: 700; color: #000; margin: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.card-sub { font-size: 13px; color: #888; margin: 0; display: flex; align-items: center; gap: 4px; }
.card-loc { width: 14px; height: 14px; }
.card-bottom { display: flex; align-items: center; justify-content: space-between; }
.card-extra { font-size: 13px; color: #888; }
.card-price { font-size: 18px; font-weight: 700; color: #000; }
.card-link { font-size: 14px; color: #888; background: none; border: none; cursor: pointer; padding: 0; }

.card-remove { position: absolute; top: 8px; right: 8px; z-index: 3; width: 28px; height: 28px; border-radius: 50%; background: rgba(0,0,0,0.5); color: #fff; border: none; font-size: 14px; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.card-remove:hover { background: rgba(0,0,0,0.8); }
</style>
