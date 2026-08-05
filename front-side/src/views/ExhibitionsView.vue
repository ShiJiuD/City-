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

const cityStore = useCityStore()

// 图片列表，按板块分配
const currentPosters = [all1, all2]
const futurePosters = [all3, all4]
const pastPosters = [all5, all6]

// ===== 筛选状态 =====
const searchQuery = ref('')
const selectedType = ref('综合展览')
const selectedCity = ref(cityStore.currentCity || '南京')

const exhibitionTypes = ['综合展览', '当代展览', '古典展览', '雕塑展览', '摄影展览']
const cities = ['苏州', '南京', '无锡', '深圳', '广州']

// ===== 分页状态 =====
const currentPage = ref(1)
const pageSize = 2

// ===== 展览数据接口 =====
interface Exhibition {
  id: number
  name: string
  galleryName: string
  dateRange: string
  price: number
  badge?: { text: string; type: 'black' | 'red' }
  category: 'current' | 'future' | 'past'
  bgColor: string
  type: string
  city: string
}

// ===== Mock 数据 =====
const allExhibitions: Exhibition[] = [
  // ===== 正在展出 =====
  {
    id: 1, name: '印象派大师展', galleryName: '德基美术馆', dateRange: '2026.07.15 - 2026.09.15', price: 128,
    badge: { text: '精选', type: 'black' }, category: 'current', bgColor: '#C4A882', type: '综合展览', city: '南京',
  },
  {
    id: 2, name: '当代雕塑艺术展', galleryName: '四方美术馆', dateRange: '2026.07.20 - 2026.08.30', price: 58,
    category: 'current', bgColor: '#9BAEBC', type: '雕塑展览', city: '南京',
  },
  {
    id: 3, name: '江南水墨画展', galleryName: '江苏省美术馆', dateRange: '2026.08.01 - 2026.10.07', price: 0,
    category: 'current', bgColor: '#8FA0A8', type: '综合展览', city: '南京',
  },
  {
    id: 4, name: '新媒体艺术季', galleryName: '金鹰美术馆', dateRange: '2026.08.05 - 2026.11.05', price: 88,
    category: 'current', bgColor: '#B5A898', type: '当代展览', city: '南京',
  },
  {
    id: 5, name: '花鸟画精品展', galleryName: '江苏省美术馆', dateRange: '2026.07.01 - 2026.09.30', price: 0,
    category: 'current', bgColor: '#A8B8A0', type: '综合展览', city: '南京',
  },
  {
    id: 6, name: '抽象表现主义展', galleryName: '德基美术馆', dateRange: '2026.08.01 - 2026.10.15', price: 108,
    category: 'current', bgColor: '#C0A898', type: '当代展览', city: '南京',
  },
  {
    id: 7, name: '宋元书画大展', galleryName: '四方美术馆', dateRange: '2026.08.10 - 2026.11.20', price: 138,
    category: 'current', bgColor: '#B8B0A8', type: '古典展览', city: '南京',
  },
  {
    id: 8, name: '城市建筑摄影展', galleryName: '金鹰美术馆', dateRange: '2026.09.01 - 2026.12.01', price: 38,
    category: 'current', bgColor: '#A0B0B8', type: '摄影展览', city: '南京',
  },
  {
    id: 9, name: '西方古典油画展', galleryName: '德基美术馆', dateRange: '2026.08.10 - 2026.10.20', price: 168,
    category: 'current', bgColor: '#A89080', type: '古典展览', city: '苏州',
  },
  {
    id: 10, name: '青年艺术家联展', galleryName: '江苏省美术馆', dateRange: '2026.08.12 - 2026.09.25', price: 0,
    category: 'current', bgColor: '#98A8B0', type: '综合展览', city: '无锡',
  },
  {
    id: 11, name: '现代摄影回顾展', galleryName: '四方美术馆', dateRange: '2026.08.15 - 2026.10.10', price: 48,
    category: 'current', bgColor: '#B0A090', type: '摄影展览', city: '深圳',
  },
  {
    id: 12, name: '当代陶艺双年展', galleryName: '金鹰美术馆', dateRange: '2026.08.18 - 2026.11.18', price: 68,
    category: 'current', bgColor: '#C0B0A0', type: '当代展览', city: '广州',
  },

  // ===== 即将展出 =====
  {
    id: 13, name: '未来主义设计展', galleryName: '金鹰美术馆', dateRange: '2026.10.01 - 2026.12.31', price: 98,
    badge: { text: '剩余57天', type: 'red' }, category: 'future', bgColor: '#C8B8A8', type: '当代展览', city: '南京',
  },
  {
    id: 14, name: '国际摄影双年展', galleryName: '四方美术馆', dateRange: '2026.10.15 - 2027.01.15', price: 68,
    badge: { text: '剩余71天', type: 'red' }, category: 'future', bgColor: '#A8B0B8', type: '摄影展览', city: '南京',
  },
  {
    id: 15, name: '古代青铜器展', galleryName: '南京博物院', dateRange: '2026.11.01 - 2027.02.28', price: 0,
    badge: { text: '剩余88天', type: 'red' }, category: 'future', bgColor: '#A09888', type: '古典展览', city: '苏州',
  },
  {
    id: 16, name: '当代陶艺邀请展', galleryName: '江苏省美术馆', dateRange: '2026.11.15 - 2027.03.15', price: 48,
    badge: { text: '剩余102天', type: 'red' }, category: 'future', bgColor: '#B8B0A0', type: '雕塑展览', city: '无锡',
  },

  // ===== 往期展出 =====
  {
    id: 17, name: '文艺复兴珍品展', galleryName: '德基美术馆', dateRange: '2026.03.01 - 2026.06.30', price: 198,
    category: 'past', bgColor: '#9A9080', type: '古典展览', city: '南京',
  },
  {
    id: 18, name: '数字交互艺术展', galleryName: '金鹰美术馆', dateRange: '2026.04.15 - 2026.07.15', price: 78,
    category: 'past', bgColor: '#90A0A8', type: '当代展览', city: '深圳',
  },
  {
    id: 19, name: '近现代书法展', galleryName: '江苏省美术馆', dateRange: '2026.02.20 - 2026.05.20', price: 0,
    category: 'past', bgColor: '#B0A898', type: '综合展览', city: '广州',
  },
  {
    id: 20, name: '当代装置艺术展', galleryName: '四方美术馆', dateRange: '2026.01.10 - 2026.04.10', price: 58,
    category: 'past', bgColor: '#A8B0B8', type: '雕塑展览', city: '南京',
  },
]

// ===== 筛选后的数据 =====
const filteredExhibitions = computed(() => {
  return allExhibitions.filter((ex) => {
    // 搜索过滤
    if (searchQuery.value && !ex.name.includes(searchQuery.value) && !ex.galleryName.includes(searchQuery.value)) {
      return false
    }
    // 类型过滤（综合展览 = 全部，不过滤）
    if (selectedType.value !== '综合展览' && ex.type !== selectedType.value) {
      return false
    }
    // 城市过滤
    if (selectedCity.value && ex.city !== selectedCity.value) {
      return false
    }
    return true
  })
})

const currentExhibitions = computed(() => filteredExhibitions.value.filter((e) => e.category === 'current'))
const futureExhibitions = computed(() => filteredExhibitions.value.filter((e) => e.category === 'future'))
const pastExhibitions = computed(() => filteredExhibitions.value.filter((e) => e.category === 'past'))

// ===== 分页后的正在展出 =====
const paginatedCurrent = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return currentExhibitions.value.slice(start, start + pageSize)
})

const totalPages = computed(() => Math.max(1, Math.ceil(currentExhibitions.value.length / pageSize)))

// ===== 是否有任何结果 =====
const hasResults = computed(
  () => currentExhibitions.value.length > 0 || futureExhibitions.value.length > 0 || pastExhibitions.value.length > 0,
)

// ===== 方法 =====
function resetFilters() {
  searchQuery.value = ''
  selectedType.value = '综合展览'
  selectedCity.value = cityStore.currentCity || '南京'
  currentPage.value = 1
}

function goToPage(page: number) {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
  }
}

// 监听类型/城市变化时重置分页
watch([selectedType, selectedCity, searchQuery], () => {
  currentPage.value = 1
})

// ===== 格式化价格 =====
function formatPrice(price: number): string {
  return `¥${price}`
}

// ===== 即将展出 / 往期展出 的预览数量 =====
const FUTURE_PREVIEW_COUNT = 2
const PAST_PREVIEW_COUNT = 2
</script>

<template>
  <div class="exhibitions-page">
    <!-- ===== Header：标题 + 搜索栏 ===== -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="header-title">全部展览</h1>
        <p class="header-subtitle">筛选浏览当前城市所有美术馆展览</p>
      </div>
      <div class="header-search">
        <input
          v-model="searchQuery"
          type="text"
          class="search-input"
          placeholder="搜索展览"
        />
        <img :src="searchIcon" alt="搜索" class="search-icon" />
      </div>
    </div>

    <!-- ===== 综合筛选栏 ===== -->
    <div class="filter-bar">
      <!-- 展览类型 -->
      <div class="filter-item">
        <span class="filter-label">展览类型</span>
        <select v-model="selectedType" class="filter-select">
          <option v-for="t in exhibitionTypes" :key="t" :value="t">{{ t }}</option>
        </select>
      </div>

      <!-- 城市 -->
      <div class="filter-item">
        <span class="filter-label">城市</span>
        <select v-model="selectedCity" class="filter-select">
          <option v-for="c in cities" :key="c" :value="c">{{ c }}</option>
        </select>
      </div>

      <!-- 重置按钮 -->
      <button class="filter-reset" @click="resetFilters">重置</button>
    </div>

    <!-- ===== 无结果提示 ===== -->
    <div v-if="!hasResults" class="empty-state">
      <span class="empty-icon">🔍</span>
      <p class="empty-text">暂无符合条件的展览</p>
      <button class="empty-reset" @click="resetFilters">重置筛选条件</button>
    </div>

    <template v-else>
      <!-- ===== 板块一：正在展出 CURRENT ===== -->
      <section v-if="currentExhibitions.length > 0" class="exhibition-section">
        <div class="section-header">
          <div class="section-header-left">
            <span class="section-bar"></span>
            <div class="section-title-group">
              <h2 class="section-title">正在展出</h2>
              <span class="section-sub">CURRENT</span>
            </div>
          </div>
          <button class="section-more">查看全部 &gt;</button>
        </div>

        <div class="exhibition-grid exhibition-grid--current">
          <div
            v-for="(ex, idx) in paginatedCurrent"
            :key="ex.id"
            class="exhibition-card"
          >
            <!-- 海报区域 -->
            <div class="card-poster card-poster--current">
              <img
                :src="currentPosters[idx % currentPosters.length]"
                alt="展览海报"
                class="poster-placeholder"
              />
              <!-- 角标 -->
              <span
                v-if="ex.badge"
                class="card-badge"
                :class="'card-badge--' + ex.badge.type"
              >
                {{ ex.badge.text }}
              </span>
            </div>
            <!-- 信息区域 -->
            <div class="card-info">
              <p class="card-exhibition-name">展览：{{ ex.name }}</p>
              <p class="card-gallery-name">美术馆：{{ ex.galleryName }}</p>
              <p class="card-time">时间：{{ ex.dateRange }}</p>
              <span class="card-price">{{ formatPrice(ex.price) }}</span>
            </div>
          </div>
        </div>
      </section>

      <!-- ===== 板块二：即将展出 FUTURE ===== -->
      <section v-if="futureExhibitions.length > 0" class="exhibition-section">
        <div class="section-header">
          <div class="section-header-left">
            <span class="section-bar section-bar--red"></span>
            <div class="section-title-group">
              <h2 class="section-title">即将展出</h2>
              <span class="section-sub">FUTURE</span>
            </div>
          </div>
          <button class="section-more">查看全部 &gt;</button>
        </div>

        <div class="exhibition-grid exhibition-grid--future">
          <div
            v-for="(ex, idx) in futureExhibitions.slice(0, FUTURE_PREVIEW_COUNT)"
            :key="ex.id"
            class="exhibition-card exhibition-card--future"
          >
            <!-- 海报区域 -->
            <div class="card-poster card-poster--future">
              <img
                :src="futurePosters[idx % futurePosters.length]"
                alt="展览海报"
                class="poster-placeholder"
              />
              <!-- 角标 -->
              <span
                v-if="ex.badge"
                class="card-badge"
                :class="'card-badge--' + ex.badge.type"
              >
                {{ ex.badge.text }}
              </span>
            </div>
            <!-- 信息区域 -->
            <div class="card-info">
              <p class="card-exhibition-name">展览：{{ ex.name }}</p>
              <p class="card-gallery-name">美术馆：{{ ex.galleryName }}</p>
              <p class="card-time">时间：{{ ex.dateRange }}</p>
              <span class="card-price">{{ formatPrice(ex.price) }}</span>
            </div>
          </div>
        </div>
      </section>

      <!-- ===== 板块三：往期展出 PAST ===== -->
      <section v-if="pastExhibitions.length > 0" class="exhibition-section">
        <div class="section-header">
          <div class="section-header-left">
            <span class="section-bar section-bar--gray"></span>
            <div class="section-title-group">
              <h2 class="section-title">往期展出</h2>
              <span class="section-sub">PAST</span>
            </div>
          </div>
          <button class="section-more">查看全部 &gt;</button>
        </div>

        <div class="exhibition-grid exhibition-grid--past">
          <div
            v-for="(ex, idx) in pastExhibitions.slice(0, PAST_PREVIEW_COUNT)"
            :key="ex.id"
            class="exhibition-card"
          >
            <!-- 海报区域 -->
            <div class="card-poster card-poster--current">
              <img
                :src="pastPosters[idx % pastPosters.length]"
                alt="展览海报"
                class="poster-placeholder"
              />
              <!-- 角标 -->
              <span
                v-if="ex.badge"
                class="card-badge"
                :class="'card-badge--' + ex.badge.type"
              >
                {{ ex.badge.text }}
              </span>
            </div>
            <!-- 信息区域 -->
            <div class="card-info">
              <p class="card-exhibition-name">展览：{{ ex.name }}</p>
              <p class="card-gallery-name">美术馆：{{ ex.galleryName }}</p>
              <p class="card-time">时间：{{ ex.dateRange }}</p>
              <span class="card-price">{{ formatPrice(ex.price) }}</span>
            </div>
          </div>
        </div>
      </section>

      <!-- ===== 分页器 ===== -->
      <Pagination
        :current-page="currentPage"
        :total-pages="totalPages"
        @page-change="goToPage"
      />
    </template>

    <FooterBar />
  </div>
</template>

<style scoped>
/* ===== 页面容器 ===== */
.exhibitions-page {
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

/* 重置按钮 — 靠右 */
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

/* ===== 展览板块 ===== */
.exhibition-section {
  max-width: 1440px;
  margin: 66px auto 0;
  padding: 0 40px;
}

/* 板块标题 — 宽度与下方网格一致 */
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  width: calc(616px * 2 + 11px);
}

.section-header-left {
  display: flex;
  align-items: stretch;
  gap: 10px;
}

/* 左侧竖线装饰 — 高度跟随标题组 */
.section-bar {
  display: inline-block;
  width: 3px;
  background: #111;
  border-radius: 2px;
  flex-shrink: 0;
}

.section-bar--red {
  background: #e74c3c;
}

.section-bar--gray {
  background: #888;
}

.section-title-group {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.section-title {
  font-size: 20px;
  font-weight: 700;
  color: #111;
  margin: 0;
  line-height: 1.3;
}

.section-sub {
  font-size: 13px;
  font-weight: 400;
  color: #aaa;
  text-transform: uppercase;
  letter-spacing: 1px;
  line-height: 1.3;
}

/* 查看全部 */
.section-more {
  background: none;
  border: none;
  font-size: 16px;
  color: #888;
  cursor: pointer;
  padding: 0;
  transition: color 0.2s;
}

.section-more:hover {
  color: #333;
}

/* ===== 展览网格 ===== */
.exhibition-grid {
  display: grid;
  grid-template-columns: repeat(2, 616px);
  gap: 11px;
  justify-content: start;
}

/* ===== 展览卡片 ===== */
.exhibition-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  transition: transform 0.3s, box-shadow 0.3s;
}

.exhibition-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.12);
}

/* 正在展出卡片大小：616×582 */
.exhibition-grid--current .exhibition-card {
  width: 616px;
  height: 582px;
}

/* 即将展出卡片大小：616×600 */
.exhibition-grid--future .exhibition-card {
  width: 616px;
  height: 600px;
}

/* 往期展出卡片大小：同正在展出 616×582 */
.exhibition-grid--past .exhibition-card {
  width: 616px;
  height: 582px;
}

/* ===== 海报区域 ===== */
.card-poster {
  position: relative;
  overflow: hidden;
}

/* 正在展出海报：616×388 */
.card-poster--current {
  width: 616px;
  height: 388px;
}

/* 即将展出海报：616×400 */
.card-poster--future {
  width: 616px;
  height: 400px;
}

.poster-placeholder {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/* ===== 角标 ===== */
.card-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 2;
  border-radius: 4px;
  padding: 4px 8px;
  font-size: 12px;
  color: #fff;
  line-height: 1.4;
  white-space: nowrap;
}

/* black 样式：深色底 */
.card-badge--black {
  background: rgba(51, 51, 51, 0.9);
}

/* red 样式：警示红底 */
.card-badge--red {
  background: #e74c3c;
}

/* ===== 卡片信息区 ===== */
.card-info {
  position: relative;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
}

.card-exhibition-name {
  font-size: 16px;
  font-weight: 700;
  color: #111;
  margin: 0 0 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-gallery-name {
  font-size: 13px;
  color: #888;
  margin: 0 0 4px;
}

.card-time {
  font-size: 13px;
  color: #888;
  margin: 0;
}

/* 价格：右下角 */
.card-price {
  position: absolute;
  right: 20px;
  bottom: 16px;
  font-size: 18px;
  font-weight: 700;
  color: #e74c3c;
}

</style>
