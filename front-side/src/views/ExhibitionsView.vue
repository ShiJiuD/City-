<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCityStore } from '../stores/city'
import { getCurrentExhibitions, getFutureExhibitions, getPastExhibitions } from '../api/exhibition'
import type { ExhibitionListItem } from '../types'
import { ExhibitionType } from '../types'
import FooterBar from '../components/FooterBar.vue'
import Pagination from '../components/Pagination.vue'
import searchIcon from '../assets/home/search.png'
import all1 from '../assets/home/all1.jpg'
import all2 from '../assets/home/all2.jpg'
import all3 from '../assets/home/all3.png'
import all4 from '../assets/home/all4.png'
import all5 from '../assets/home/all5.jpg'
import all6 from '../assets/home/all6.jpg'

const router = useRouter()
const cityStore = useCityStore()

// 本地占位图
const currentPosters = [all1, all2]
const futurePosters = [all3, all4]
const pastPosters = [all5, all6]

// ===== 筛选状态 =====
const searchQuery = ref('')
const selectedType = ref<number | undefined>(undefined)
const selectedCity = ref(cityStore.currentCity || '南京')

const exhibitionTypes = [
  { label: '全部类型', value: undefined },
  { label: '当代展览', value: 1 },
  { label: '古典展览', value: 2 },
  { label: '雕塑展览', value: 3 },
  { label: '摄影展览', value: 4 },
]
const cities = ['苏州', '南京', '无锡', '深圳', '广州']

// ===== 数据状态 =====
const loading = ref(true)
const currentList = ref<ExhibitionListItem[]>([])
const futureList = ref<ExhibitionListItem[]>([])
const pastList = ref<ExhibitionListItem[]>([])

// ===== 分页 =====
const currentPage = ref(1)
const pageSize = 2
const paginatedCurrent = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return currentList.value.slice(start, start + pageSize)
})
const totalPages = computed(() => Math.max(1, Math.ceil(currentList.value.length / pageSize)))

const hasResults = computed(
  () => currentList.value.length > 0 || futureList.value.length > 0 || pastList.value.length > 0,
)

// ===== 请求后端 =====
async function fetchData() {
  loading.value = true
  const params = {
    keyword: searchQuery.value || undefined,
    type: selectedType.value,
    city: selectedCity.value || undefined,
  }
  try {
    const [curRes, futRes, pastRes] = await Promise.all([
      getCurrentExhibitions(params),
      getFutureExhibitions(params),
      getPastExhibitions(params),
    ])
    currentList.value = curRes.code === 1 ? curRes.data : []
    futureList.value = futRes.code === 1 ? futRes.data : []
    pastList.value = pastRes.code === 1 ? pastRes.data : []
  } catch {
    currentList.value = []
    futureList.value = []
    pastList.value = []
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  searchQuery.value = ''
  selectedType.value = undefined
  selectedCity.value = cityStore.currentCity || '南京'
  currentPage.value = 1
}

function goToPage(page: number) {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
  }
}

function goToList(category: string) {
  router.push(`/exhibitions/${category}`)
}

function formatPrice(price: number): string {
  if (price === 0) return '免费'
  return `¥${price}`
}

function formatDateRange(start: string, end: string): string {
  return `${start} - ${end}`
}

// 筛选变化重新请求
watch([selectedType, selectedCity, searchQuery], () => {
  currentPage.value = 1
  fetchData()
})

onMounted(() => {
  fetchData()
})

const FUTURE_PREVIEW_COUNT = 2
const PAST_PREVIEW_COUNT = 2
</script>

<template>
  <div class="exhibitions-page">
    <!-- Header -->
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

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-item">
        <span class="filter-label">展览类型</span>
        <select v-model="selectedType" class="filter-select">
          <option v-for="t in exhibitionTypes" :key="t.label" :value="t.value">{{ t.label }}</option>
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

    <!-- 加载中 -->
    <div v-if="loading" class="status-wrap">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- 无结果 -->
    <div v-else-if="!hasResults" class="empty-state">
      <span class="empty-icon">🔍</span>
      <p class="empty-text">暂无符合条件的展览</p>
      <button class="empty-reset" @click="resetFilters">重置筛选条件</button>
    </div>

    <template v-else>
      <div class="exhibitions-content">
      <!-- 正在展出 -->
      <section v-if="currentList.length > 0" class="exhibition-section">
        <div class="section-header">
          <div class="section-header-left">
            <span class="section-bar"></span>
            <div class="section-title-group">
              <h2 class="section-title">正在展出</h2>
              <span class="section-sub">CURRENT</span>
            </div>
          </div>
          <button class="section-more" @click="goToList('current')">查看全部 &gt;</button>
        </div>
        <div class="exhibition-grid exhibition-grid--current">
          <div v-for="(ex, idx) in paginatedCurrent" :key="ex.id" class="exhibition-card" @click="router.push('/exhibition/' + ex.id)">
            <div class="card-poster card-poster--current">
              <img :src="currentPosters[idx % currentPosters.length]" alt="海报" class="poster-placeholder" />
            </div>
            <div class="card-info">
              <p class="card-exhibition-name">展览：{{ ex.title }}</p>
              <p class="card-gallery-name">美术馆：{{ ex.galleryName }}</p>
              <p class="card-time">时间：{{ formatDateRange(ex.startDate, ex.endDate) }}</p>
              <span class="card-price">{{ formatPrice(ex.price) }}</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 即将展出 -->
      <section v-if="futureList.length > 0" class="exhibition-section">
        <div class="section-header">
          <div class="section-header-left">
            <span class="section-bar section-bar--red"></span>
            <div class="section-title-group">
              <h2 class="section-title">即将展出</h2>
              <span class="section-sub">FUTURE</span>
            </div>
          </div>
          <button class="section-more" @click="goToList('future')">查看全部 &gt;</button>
        </div>
        <div class="exhibition-grid exhibition-grid--future">
          <div v-for="(ex, idx) in futureList.slice(0, FUTURE_PREVIEW_COUNT)" :key="ex.id" class="exhibition-card exhibition-card--future" @click="router.push('/exhibition/' + ex.id)">
            <div class="card-poster card-poster--future">
              <img :src="futurePosters[idx % futurePosters.length]" alt="海报" class="poster-placeholder" />
            </div>
            <div class="card-info">
              <p class="card-exhibition-name">展览：{{ ex.title }}</p>
              <p class="card-gallery-name">美术馆：{{ ex.galleryName }}</p>
              <p class="card-time">时间：{{ formatDateRange(ex.startDate, ex.endDate) }}</p>
              <span class="card-price">{{ formatPrice(ex.price) }}</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 往期展出 -->
      <section v-if="pastList.length > 0" class="exhibition-section">
        <div class="section-header">
          <div class="section-header-left">
            <span class="section-bar section-bar--gray"></span>
            <div class="section-title-group">
              <h2 class="section-title">往期展出</h2>
              <span class="section-sub">PAST</span>
            </div>
          </div>
          <button class="section-more" @click="goToList('past')">查看全部 &gt;</button>
        </div>
        <div class="exhibition-grid exhibition-grid--past">
          <div v-for="(ex, idx) in pastList.slice(0, PAST_PREVIEW_COUNT)" :key="ex.id" class="exhibition-card" @click="router.push('/exhibition/' + ex.id)">
            <div class="card-poster card-poster--current">
              <img :src="pastPosters[idx % pastPosters.length]" alt="海报" class="poster-placeholder" />
            </div>
            <div class="card-info">
              <p class="card-exhibition-name">展览：{{ ex.title }}</p>
              <p class="card-gallery-name">美术馆：{{ ex.galleryName }}</p>
              <p class="card-time">时间：{{ formatDateRange(ex.startDate, ex.endDate) }}</p>
              <span class="card-price">{{ formatPrice(ex.price) }}</span>
            </div>
          </div>
        </div>
      </section>

      <Pagination :current-page="currentPage" :total-pages="totalPages" @page-change="goToPage" />
      </div>
    </template>

    <FooterBar />
  </div>
</template>

<style scoped>
.exhibitions-page { flex: 1; min-height: 0; background: #e8eef0; overflow-y: auto; }
.exhibitions-content { padding-bottom: 80px; }

.status-wrap { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 300px; gap: 16px; }
.spinner { width: 36px; height: 36px; border: 3px solid #e0e0e0; border-top-color: #5A5E61; border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.page-header { display: flex; justify-content: space-between; align-items: center; max-width: 1440px; margin: 0 auto; padding: 32px 40px 0; }
.header-left { display: flex; flex-direction: column; }
.header-title { font-size: 36px; font-weight: 700; color: #111; margin: 0; line-height: 1.2; }
.header-subtitle { font-size: 16px; color: #888; margin: 8px 0 0; }
.header-search { position: relative; display: flex; align-items: center; width: 280px; }
.search-input { width: 100%; height: 40px; padding: 0 44px 0 20px; border: 1px solid rgba(0,0,0,0.1); border-radius: 20px; background: rgba(255,255,255,0.7); font-size: 14px; color: #333; outline: none; transition: border-color 0.25s, background 0.25s; }
.search-input::placeholder { color: #aaa; }
.search-input:focus { border-color: rgba(0,0,0,0.25); background: rgba(255,255,255,0.95); }
.search-icon { position: absolute; right: 14px; width: 18px; height: 18px; pointer-events: none; opacity: 0.5; }

.filter-bar { display: flex; align-items: center; gap: 16px; max-width: 1440px; margin: 20px auto 0; padding: 0 40px 16px; border-bottom: 1px solid #111; }
.filter-item { display: flex; align-items: center; gap: 8px; }
.filter-label { font-size: 16px; color: #555; white-space: nowrap; }
.filter-select { height: 36px; padding: 0 32px 0 12px; border: 1px solid #d0d0d0; border-radius: 8px; background: #fff; font-size: 14px; color: #333; cursor: pointer; outline: none; appearance: none; background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%23666' stroke-width='2'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E"); background-repeat: no-repeat; background-position: right 10px center; transition: border-color 0.2s; }
.filter-select:hover, .filter-select:focus { border-color: #aaa; }
.filter-reset { height: 36px; padding: 0 24px; margin-left: auto; background: #fff; border: 1px solid #999; border-radius: 8px; font-size: 14px; color: #555; cursor: pointer; transition: background 0.2s, color 0.2s; }
.filter-reset:hover { background: #f2f2f2; color: #333; }

.empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; max-width: 1440px; margin: 0 auto; padding: 80px 40px; }
.empty-icon { font-size: 48px; margin-bottom: 16px; }
.empty-text { font-size: 16px; color: #999; margin: 0 0 20px; }
.empty-reset { padding: 10px 32px; background: #5a5e61; color: #fff; border: none; border-radius: 8px; font-size: 14px; cursor: pointer; transition: background 0.2s; }
.empty-reset:hover { background: #4a4e51; }

.exhibition-section { max-width: 1440px; margin: 66px auto 0; padding: 0 40px; }
.section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; width: calc(616px * 2 + 11px); }
.section-header-left { display: flex; align-items: stretch; gap: 10px; }
.section-bar { display: inline-block; width: 3px; background: #111; border-radius: 2px; flex-shrink: 0; }
.section-bar--red { background: #e74c3c; }
.section-bar--gray { background: #888; }
.section-title-group { display: flex; flex-direction: column; gap: 2px; }
.section-title { font-size: 20px; font-weight: 700; color: #111; margin: 0; line-height: 1.3; }
.section-sub { font-size: 13px; font-weight: 400; color: #aaa; text-transform: uppercase; letter-spacing: 1px; line-height: 1.3; }
.section-more { background: none; border: none; font-size: 16px; color: #888; cursor: pointer; padding: 0; transition: color 0.2s; }
.section-more:hover { color: #333; }

.exhibition-grid { display: grid; grid-template-columns: repeat(2, 616px); gap: 11px; justify-content: start; }
.exhibition-card { background: #fff; border-radius: 12px; overflow: hidden; transition: transform 0.3s, box-shadow 0.3s; }
.exhibition-card:hover { transform: translateY(-4px); box-shadow: 0 8px 28px rgba(0,0,0,0.12); }
.exhibition-grid--current .exhibition-card { width: 616px; height: 582px; }
.exhibition-grid--future .exhibition-card { width: 616px; height: 600px; }
.exhibition-grid--past .exhibition-card { width: 616px; height: 582px; }

.card-poster { position: relative; overflow: hidden; }
.card-poster--current { width: 616px; height: 388px; }
.card-poster--future { width: 616px; height: 400px; }
.poster-placeholder { width: 100%; height: 100%; object-fit: cover; display: block; }

.card-info { position: relative; padding: 16px 20px; display: flex; flex-direction: column; justify-content: center; height: 194px; }
.card-exhibition-name { font-size: 16px; font-weight: 700; color: #111; margin: 0 0 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.card-gallery-name { font-size: 13px; color: #888; margin: 0 0 4px; }
.card-time { font-size: 13px; color: #888; margin: 0; }
.card-price { position: absolute; right: 20px; bottom: 8px; font-size: 18px; font-weight: 700; color: #e74c3c; }
.exhibition-grid--future .card-info { height: 200px; }

</style>
