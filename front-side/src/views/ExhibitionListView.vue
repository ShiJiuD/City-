<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCityStore } from '../stores/city'
import { getCurrentExhibitions, getFutureExhibitions, getPastExhibitions } from '../api/exhibition'
import type { ExhibitionListItem } from '../types'
import FooterBar from '../components/FooterBar.vue'
import all1 from '../assets/home/all1.jpg'
import all2 from '../assets/home/all2.jpg'
import all3 from '../assets/home/all3.png'
import all4 from '../assets/home/all4.png'
import all5 from '../assets/home/all5.jpg'
import all6 from '../assets/home/all6.jpg'

const route = useRoute()
const router = useRouter()
const cityStore = useCityStore()

const category = computed(() => (route.params.category as string) || 'current')

const titleMap: Record<string, string> = {
  current: '正在展出',
  future: '即将展出',
  past: '往期展出',
}
const subMap: Record<string, string> = {
  current: 'CURRENT',
  future: 'FUTURE',
  past: 'PAST',
}

const pageTitle = computed(() => titleMap[category.value] || '正在展出')
const pageSub = computed(() => subMap[category.value] || 'CURRENT')
const cityName = computed(() => cityStore.currentCity || '南京')

const posterMap: Record<string, string[]> = {
  current: [all1, all2],
  future: [all3, all4],
  past: [all5, all6],
}

const posters = computed(() => posterMap[category.value] || [all1, all2])

const loading = ref(true)
const exhibitions = ref<ExhibitionListItem[]>([])

async function fetchData() {
  loading.value = true
  const params = { city: cityStore.currentCity || undefined }
  try {
    let res
    if (category.value === 'future') {
      res = await getFutureExhibitions(params)
    } else if (category.value === 'past') {
      res = await getPastExhibitions(params)
    } else {
      res = await getCurrentExhibitions(params)
    }
    exhibitions.value = res.code === 1 ? res.data : []
  } catch {
    exhibitions.value = []
  } finally {
    loading.value = false
  }
}

function formatPrice(price: number): string {
  if (price === 0) return '免费'
  return `¥${price}`
}

function formatDateRange(start: string, end: string): string {
  return `${start} - ${end}`
}

onMounted(() => fetchData())
</script>

<template>
  <div class="list-page">
    <div class="page-header">
      <h1 class="page-title">{{ pageTitle }}</h1>
      <p class="page-subtitle">当前所在城市{{ cityName }}{{ pageSub }}</p>
      <div class="page-divider"></div>
    </div>

    <div v-if="loading" class="status-wrap">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else class="page-content">
      <div class="exhibition-grid">
        <div v-for="(ex, idx) in exhibitions" :key="ex.id" class="exhibition-card" @click="router.push('/exhibition/' + ex.id)">
          <div class="card-poster">
            <img :src="posters[idx % posters.length]" alt="海报" class="poster-img" />
          </div>
          <div class="card-info">
            <p class="card-text">展览：{{ ex.title }}</p>
            <p class="card-text sub">美术馆：{{ ex.galleryName }}</p>
            <p class="card-text sub">时间：{{ formatDateRange(ex.startDate, ex.endDate) }}</p>
            <span class="card-price">{{ formatPrice(ex.price) }}</span>
          </div>
        </div>
      </div>
    </div>

    <FooterBar />
  </div>
</template>

<style scoped>
.list-page { flex: 1; min-height: 0; background: #E8EEF0; overflow-y: auto; }

.status-wrap { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 300px; gap: 16px; }
.spinner { width: 36px; height: 36px; border: 3px solid #e0e0e0; border-top-color: #5A5E61; border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.page-header { display: flex; flex-direction: column; align-items: center; max-width: 1440px; margin: 0 auto; padding: 48px 40px 0; }
.page-title { font-size: 36px; font-weight: 700; color: #000; margin: 0; line-height: 1.3; }
.page-subtitle { font-size: 14px; color: rgba(112,112,112,1); margin: 12px 0 24px; }
.page-divider { width: 100%; height: 1px; background: #000; }

.page-content { max-width: 1440px; margin: 0 auto; padding: 40px 40px 80px; }
.exhibition-grid { display: grid; grid-template-columns: repeat(2, 616px); gap: 11px; justify-content: center; }

.exhibition-card { width: 616px; height: 582px; background: #fff; border-radius: 12px; overflow: hidden; transition: transform 0.3s, box-shadow 0.3s; }
.exhibition-card:hover { transform: translateY(-4px); box-shadow: 0 8px 28px rgba(0,0,0,0.12); }

.card-poster { width: 616px; height: 388px; position: relative; overflow: hidden; }
.poster-img { width: 100%; height: 100%; object-fit: cover; display: block; }

.card-info { height: 194px; padding: 16px 20px; display: flex; flex-direction: column; justify-content: center; position: relative; }
.card-text { font-size: 16px; color: #111; margin: 0 0 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-weight: 700; }
.card-text.sub { font-size: 13px; color: #888; font-weight: 400; }
.card-price { position: absolute; right: 20px; bottom: 8px; font-size: 18px; font-weight: 700; color: #e74c3c; }
</style>
