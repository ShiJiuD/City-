<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import FooterBar from '../components/FooterBar.vue'
import { getGalleriesPage } from '../api/exhibition'

const router = useRouter()
import type { GalleryPageItem } from '../types'
import banner04_1 from '../assets/home/banner04-1.png'
import banner04_2 from '../assets/home/banner04-2.jpg'
import banner04_3 from '../assets/home/banner04-3.png'
import banner04_4 from '../assets/home/banner04-4.png'

const placeholderImages = [banner04_1, banner04_2, banner04_3, banner04_4]

const loading = ref(true)
const galleries = ref<GalleryPageItem[]>([])

async function fetchGalleries() {
  loading.value = true
  try {
    const res = await getGalleriesPage({ pageNum: 1, pageSize: 5 })
    if (res.code === 1) {
      galleries.value = res.data.records.slice(0, 5)
    }
  } catch {
    // 后端不可用时留空
    galleries.value = []
  } finally {
    loading.value = false
  }
}

function rankColor(rank: number): string {
  const t = (rank - 1) / 4
  const r = Math.round(255 + (148 - 255) * t)
  const g = Math.round(195 + (148 - 195) * t)
  const b = Math.round(0 + (148 - 0) * t)
  const a = (0.45 + (0.65 - 0.45) * t).toFixed(2)
  return `rgba(${r}, ${g}, ${b}, ${a})`
}

onMounted(() => fetchGalleries())
</script>

<template>
  <div class="hot-galleries-page">
    <div class="page-header">
      <h1 class="page-title">热门美术馆</h1>
      <p class="page-subtitle">依据浏览热度、展览更新活跃度综合排序</p>
      <div class="page-divider"></div>
    </div>

    <div v-if="loading" class="status-wrap">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else class="page-content">
      <div class="gallery-grid">
        <div v-for="(g, idx) in galleries" :key="g.id" class="gallery-card">
          <span class="card-rank" :style="{ background: rankColor(idx + 1) }">
            NO.{{ idx + 1 }}
          </span>
          <div class="card-image">
            <img :src="placeholderImages[idx % placeholderImages.length]" :alt="g.name" class="card-img" />
          </div>
          <div class="card-info">
            <p class="card-text">名称：{{ g.name }}</p>
            <p class="card-text">地址：{{ g.address }}</p>
            <p class="card-text">当前展览数：{{ g.exhibitionCount }}</p>
            <button class="card-detail" @click.stop="router.push('/gallery/' + g.id)">查看详情</button>
          </div>
        </div>
      </div>
    </div>

    <FooterBar />
  </div>
</template>

<style scoped>
.hot-galleries-page { flex: 1; min-height: 0; background: #E8EEF0; overflow-y: auto; }

.status-wrap { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 300px; gap: 16px; }
.spinner { width: 36px; height: 36px; border: 3px solid #e0e0e0; border-top-color: #5A5E61; border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.page-header { display: flex; flex-direction: column; align-items: center; max-width: 1440px; margin: 0 auto; padding: 48px 40px 0; }
.page-title { font-size: 36px; font-weight: 700; color: #000; margin: 0; line-height: 1.3; }
.page-subtitle { font-size: 14px; color: rgba(112,112,112,1); margin: 12px 0 24px; }
.page-divider { width: 100%; height: 1px; background: #000; }

.page-content { max-width: 1440px; margin: 0 auto; padding: 40px 40px 48px; }

.gallery-grid { display: grid; grid-template-columns: repeat(2, 580px); column-gap: 40px; row-gap: 44px; justify-content: center; }
.gallery-card { width: 580px; height: 680px; background: #fff; border-radius: 12px; overflow: hidden; position: relative; transition: transform 0.3s, box-shadow 0.3s; }
.gallery-card:hover { transform: translateY(-4px); box-shadow: 0 8px 28px rgba(0,0,0,0.12); }

.card-rank { position: absolute; top: 18px; left: 14px; z-index: 2; width: 86px; height: 83px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 20px; font-weight: 700; border-radius: 8px; letter-spacing: 1px; }

.card-image { width: 580px; height: 500px; overflow: hidden; }
.card-img { width: 100%; height: 100%; object-fit: cover; display: block; }

.card-info { height: 180px; padding: 14px 20px 16px; display: flex; flex-direction: column; position: relative; }
.card-text { font-size: 24px; color: #000; margin: 0 0 2px; }
.card-detail { position: absolute; right: 20px; bottom: 16px; background: none; border: none; font-size: 24px; color: #888; cursor: pointer; padding: 0; transition: opacity 0.2s; }
.card-detail:hover { opacity: 0.7; }
</style>
