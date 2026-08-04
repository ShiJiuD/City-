<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useCityStore } from '../stores/city'
import FooterBar from '../components/FooterBar.vue'
import Pagination from '../components/Pagination.vue'
import searchIcon from '../assets/home/search.png'
import all1 from '../assets/home/all1.jpg'
import all2 from '../assets/home/all2.jpg'
import all3 from '../assets/home/all3.png'
import all4 from '../assets/home/all4.png'
import all5 from '../assets/home/all5.jpg'
import all6 from '../assets/home/all6.jpg'
import localIcon from '../assets/home/local.png'

const cityStore = useCityStore()

const posterImages = [all1, all2, all3, all4, all5, all6]

// ===== 筛选状态 =====
const searchQuery = ref('')
const selectedType = ref('综合美术馆')
const selectedCity = ref(cityStore.currentCity || '南京')

const galleryTypes = ['综合美术馆', '当代美术馆', '古典美术馆', '雕塑美术馆', '摄影美术馆']
const cities = ['苏州', '南京', '无锡', '深圳', '广州']

// ===== 分页状态 =====
const currentPage = ref(1)
const pageSize = 6

// ===== 美术馆数据接口 =====
interface GalleryItem {
  id: number
  name: string
  address: string
  exhibitionCount: number
  type: string
  city: string
}

// ===== Mock 数据 =====
const allGalleries: GalleryItem[] = [
  { id: 1, name: '江苏省美术馆', address: '南京·秦淮区四条巷12号', exhibitionCount: 3, type: '综合美术馆', city: '南京' },
  { id: 2, name: '四方美术馆', address: '南京·浦口区珍七路9号', exhibitionCount: 2, type: '当代美术馆', city: '南京' },
  { id: 3, name: '德基美术馆', address: '南京·玄武区中山路18号', exhibitionCount: 5, type: '综合美术馆', city: '南京' },
  { id: 4, name: '金鹰美术馆', address: '南京·建邺区应天大街888号', exhibitionCount: 4, type: '当代美术馆', city: '南京' },
  { id: 5, name: '南京博物院', address: '南京·玄武区中山东路321号', exhibitionCount: 2, type: '古典美术馆', city: '南京' },
  { id: 6, name: '苏州博物馆', address: '苏州·姑苏区东北街204号', exhibitionCount: 3, type: '综合美术馆', city: '苏州' },
  { id: 7, name: '苏州美术馆', address: '苏州·姑苏区人民路2075号', exhibitionCount: 1, type: '雕塑美术馆', city: '苏州' },
  { id: 8, name: '无锡博物院', address: '无锡·梁溪区钟书路100号', exhibitionCount: 2, type: '古典美术馆', city: '无锡' },
  { id: 9, name: '深圳当代艺术馆', address: '深圳·福田区福中路184号', exhibitionCount: 4, type: '当代美术馆', city: '深圳' },
  { id: 10, name: '广东美术馆', address: '广州·越秀区二沙岛烟雨路38号', exhibitionCount: 3, type: '综合美术馆', city: '广州' },
  { id: 11, name: '六朝博物馆', address: '南京·玄武区长江路302号', exhibitionCount: 1, type: '古典美术馆', city: '南京' },
  { id: 12, name: '金陵美术馆', address: '南京·秦淮区剪子巷50号', exhibitionCount: 2, type: '摄影美术馆', city: '南京' },
]

// ===== 筛选后的数据 =====
const filteredGalleries = computed(() => {
  return allGalleries.filter((g) => {
    if (searchQuery.value && !g.name.includes(searchQuery.value) && !g.address.includes(searchQuery.value)) {
      return false
    }
    if (selectedType.value !== '综合美术馆' && g.type !== selectedType.value) {
      return false
    }
    if (selectedCity.value && g.city !== selectedCity.value) {
      return false
    }
    return true
  })
})

// ===== 分页 =====
const paginatedGalleries = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredGalleries.value.slice(start, start + pageSize)
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredGalleries.value.length / pageSize)))

const hasResults = computed(() => filteredGalleries.value.length > 0)

// ===== 方法 =====
function resetFilters() {
  searchQuery.value = ''
  selectedType.value = '综合美术馆'
  selectedCity.value = cityStore.currentCity || '南京'
  currentPage.value = 1
}

function goToPage(page: number) {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
  }
}

watch([selectedType, selectedCity, searchQuery], () => {
  currentPage.value = 1
})
</script>

<template>
  <div class="galleries-page">
    <!-- ===== Header：标题 + 搜索栏 ===== -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="header-title">美术馆</h1>
        <p class="header-subtitle">发现城市中的艺术空间</p>
      </div>
      <div class="header-search">
        <input
          v-model="searchQuery"
          type="text"
          class="search-input"
          placeholder="搜索美术馆"
        />
        <img :src="searchIcon" alt="搜索" class="search-icon" />
      </div>
    </div>

    <!-- ===== 综合筛选栏 ===== -->
    <div class="filter-bar">
      <div class="filter-item">
        <span class="filter-label">美术馆类型</span>
        <select v-model="selectedType" class="filter-select">
          <option v-for="t in galleryTypes" :key="t" :value="t">{{ t }}</option>
        </select>
      </div>

      <div class="filter-item">
        <span class="filter-label">城市</span>
        <select v-model="selectedCity" class="filter-select">
          <option v-for="c in cities" :key="c" :value="c">{{ c }}</option>
        </select>
      </div>

      <button class="filter-reset" @click="resetFilters">重置</button>
    </div>

    <!-- ===== 无结果 ===== -->
    <div v-if="!hasResults" class="empty-state">
      <span class="empty-icon">🔍</span>
      <p class="empty-text">暂无符合条件的美术馆</p>
      <button class="empty-reset" @click="resetFilters">重置筛选条件</button>
    </div>

    <!-- ===== 美术馆卡片网格 ===== -->
    <div v-else class="gallery-grid">
      <div
        v-for="(g, idx) in paginatedGalleries"
        :key="g.id"
        class="gallery-card"
      >
        <div class="card-poster">
          <img
            :src="posterImages[idx % posterImages.length]"
            alt="美术馆"
            class="poster-img"
          />
        </div>
        <div class="card-info">
          <p class="card-name">美术馆：{{ g.name }}</p>
          <p class="card-address">
            <img :src="localIcon" alt="地址" class="address-icon" />
            地址：{{ g.address }}
          </p>
          <p class="card-count">{{ g.exhibitionCount }} 场展览</p>
          <button class="card-detail">查看详情 &gt;</button>
        </div>
      </div>
    </div>

    <!-- ===== 分页器 ===== -->
    <Pagination
      :current-page="currentPage"
      :total-pages="totalPages"
      @page-change="goToPage"
    />

    <FooterBar />
  </div>
</template>

<style scoped>
/* ===== 页面容器 ===== */
.galleries-page {
  flex: 1;
  min-height: 0;
  background: #e8eef0;
  overflow-y: auto;
}

/* ===== Header ===== */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 1440px;
  margin: 0 auto;
  padding: 32px 40px 0;
}

.header-left {
  display: flex;
  flex-direction: column;
}

.header-title {
  font-size: 36px;
  font-weight: 700;
  color: #111;
  margin: 0;
  line-height: 1.2;
}

.header-subtitle {
  font-size: 16px;
  color: #888;
  margin: 8px 0 0;
}

/* 搜索框 */
.header-search {
  position: relative;
  display: flex;
  align-items: center;
  width: 280px;
}

.search-input {
  width: 100%;
  height: 40px;
  padding: 0 44px 0 20px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  color: #333;
  outline: none;
  transition: border-color 0.25s, background 0.25s;
}

.search-input::placeholder {
  color: #aaa;
}

.search-input:focus {
  border-color: rgba(0, 0, 0, 0.25);
  background: rgba(255, 255, 255, 0.95);
}

.search-icon {
  position: absolute;
  right: 14px;
  width: 18px;
  height: 18px;
  pointer-events: none;
  opacity: 0.5;
}

/* ===== 筛选栏 ===== */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  max-width: 1440px;
  margin: 20px auto 0;
  padding: 0 40px 16px;
  border-bottom: 1px solid #111;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 16px;
  color: #555;
  white-space: nowrap;
}

.filter-select {
  height: 36px;
  padding: 0 32px 0 12px;
  border: 1px solid #d0d0d0;
  border-radius: 8px;
  background: #fff;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  outline: none;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%23666' stroke-width='2'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 10px center;
  transition: border-color 0.2s;
}

.filter-select:hover,
.filter-select:focus {
  border-color: #aaa;
}

.filter-reset {
  height: 36px;
  padding: 0 24px;
  margin-left: auto;
  background: #fff;
  border: 1px solid #999;
  border-radius: 8px;
  font-size: 14px;
  color: #555;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.filter-reset:hover {
  background: #f2f2f2;
  color: #333;
}

/* ===== 空状态 ===== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  max-width: 1440px;
  margin: 0 auto;
  padding: 80px 40px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 16px;
  color: #999;
  margin: 0 0 20px;
}

.empty-reset {
  padding: 10px 32px;
  background: #5a5e61;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.empty-reset:hover {
  background: #4a4e51;
}

/* ===== 美术馆网格 ===== */
.gallery-grid {
  display: grid;
  grid-template-columns: repeat(2, 616px);
  column-gap: 11px;
  row-gap: 72px;
  justify-content: start;
  max-width: 1440px;
  margin: 66px auto 0;
  padding: 0 40px;
}

/* ===== 美术馆卡片 ===== */
.gallery-card {
  width: 616px;
  height: 528px;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transition: transform 0.3s, box-shadow 0.3s;
}

.gallery-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.12);
}

.card-poster {
  width: 100%;
  height: 388px;
  flex-shrink: 0;
  overflow: hidden;
}

.poster-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/* ===== 卡片信息区 ===== */
.card-info {
  flex: 1;
  position: relative;
  padding: 0 20px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.card-name {
  font-size: 18px;
  font-weight: 700;
  color: #111;
  margin: 0 0 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-address {
  font-size: 14px;
  color: #888;
  margin: 0 0 2px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.address-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.card-count {
  font-size: 14px;
  color: #888;
  margin: 0;
}

/* 查看详情 — 右下角，距底部 37px */
.card-detail {
  position: absolute;
  right: 20px;
  bottom: 37px;
  background: none;
  border: none;
  font-size: 16px;
  color: #888;
  cursor: pointer;
  padding: 0;
  transition: color 0.2s;
}

.card-detail:hover {
  color: #333;
}
</style>
