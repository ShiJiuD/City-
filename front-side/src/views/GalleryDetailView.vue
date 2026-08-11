<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import FooterBar from '../components/FooterBar.vue'
import { getGalleryDetail } from '../api/exhibition'
import type { GalleryDetailVO, GalleryExhibitionItem } from '../api/exhibition'
import { checkFavorite, addFavorite, cancelFavorite } from '../api/favorite'
import localIcon from '../assets/home/local.png'
import loveIcon from '../assets/detail/love.png'
import fullLoveIcon from '../assets/detail/fulllove.png'
import all1 from '../assets/home/all1.jpg'
import all2 from '../assets/home/all2.jpg'
import all3 from '../assets/home/all3.png'
import all4 from '../assets/home/all4.png'

const router = useRouter()
const route = useRoute()
const galleryId = Number(route.params.id)

const posters = [all1, all2, all3, all4]

const loading = ref(true)
const error = ref('')
const detail = ref<GalleryDetailVO | null>(null)
const isFav = ref(false)

async function fetchDetail() {
  loading.value = true
  error.value = ''
  try {
    const res = await getGalleryDetail(galleryId)
    if (res.code === 1) detail.value = res.data
    else error.value = res.msg || '加载失败'
  } catch (e: any) {
    error.value = e.message || '网络请求失败'
  } finally { loading.value = false }
}

async function fetchFavStatus() {
  try {
    const res = await checkFavorite({ targetType: 2, targetId: galleryId })
    if (res.code === 1) isFav.value = res.data.isFavorited
  } catch { /* ignore */ }
}

async function toggleFav() {
  try {
    if (isFav.value) {
      const res = await cancelFavorite({ targetType: 2, targetId: galleryId })
      if (res.code === 1) isFav.value = false
    } else {
      const res = await addFavorite({ targetType: 2, targetId: galleryId })
      if (res.code === 1) isFav.value = true
    }
  } catch { /* ignore */ }
}

function formatPrice(p: number): string {
  if (!p || p === 0) return '免费'
  return `¥${p}`
}

function fmtDate(start: string, end: string): string {
  return `${start} — ${end}`
}

onMounted(() => {
  fetchDetail()
  fetchFavStatus()
})
</script>

<template>
  <div class="detail-page">
    <!-- 加载中 -->
    <div v-if="loading" class="status-wrap">
      <div class="spinner"></div>
    </div>

    <!-- 错误 -->
    <div v-else-if="error" class="status-wrap">
      <p class="error-text">{{ error }}</p>
      <button class="retry-btn" @click="fetchDetail">重试</button>
    </div>

    <template v-else-if="detail">
      <!-- ===== 模块一：头部沉浸式大图 1440×900 ===== -->
      <div class="hero">
        <img :src="posters[galleryId % posters.length]" alt="" class="hero-bg" />

        <!-- 顶部遮罩 1440×80 -->
        <div class="hero-top">
          <button class="hero-back" @click="router.back()">&lt; 返回</button>
        </div>

        <!-- 底部遮罩 1440×210 -->
        <div class="hero-bottom">
          <div class="hero-left">
            <h1 class="hero-name">{{ detail.name }}</h1>
            <p class="hero-line">
              <img :src="localIcon" alt="" class="hero-icon" />
              地址：{{ detail.address }}
            </p>
            <p class="hero-line">开放时间：周二到周日 8:30~18:30</p>
          </div>
          <button class="hero-fav" @click="toggleFav">
            <img :src="isFav ? fullLoveIcon : loveIcon" alt="" class="fav-icon" />
            <span>{{ isFav ? '已收藏' : '收藏' }}</span>
          </button>
        </div>
      </div>

      <!-- ===== 模块二：展馆简介 ===== -->
      <div class="intro-section">
        <h2 class="intro-title">展馆简介</h2>
        <div class="intro-line"></div>
        <p class="intro-sub">INTRODUCTION</p>

        <div class="intro-card">
          <p class="intro-text">
            江苏省美术馆坐落于南京长江路文化建筑群，其前身是 1936 年建成的国立美术陈列馆，为中国近现代首座国家级美术馆，是我国美术馆事业的起点。
          </p>
          <p class="intro-text">
            美术馆设有两处展馆，分别为长江路 266 号旧址与 333 号主体馆舍，两馆相距不足 500 米，总建筑面积 40000 平方米。266 号旧址占地 4700 平方米，四层主楼呈 "山" 字形，融合民族与西方近代建筑风格，是民国新民族形式建筑代表。333 号馆舍 2010 年开放，由德国 KSP 事务所设计，兼顾现代时尚与民国建筑风貌。
          </p>
          <p class="intro-text">
            馆内典藏近 14000 件美术作品，藏品以近现代作品为主，涵盖古今中外门类，中国书画藏品最为丰厚，形成新金陵画派、江苏水印版画、李剑晨水彩画等多个特色专题藏品系列。
          </p>
          <p class="intro-text">
            美术馆秉持 "经典" 与 "现代" 错位发展理念，旧址侧重经典艺术，兼顾 20 世纪建筑、设计类艺术；主体馆聚焦当代美术，策划多元学术与品牌展览。作为国内历史最悠久的美术馆，它见证中国现代艺术发展。未来，美术馆将秉持专业、多元、包容的服务理念，完善公共文化服务体系，打造艺术传播与中外文化交流平台，成为大众品鉴艺术、陶冶身心的文化空间。
          </p>
        </div>
      </div>

      <!-- ===== 模块三：正在展出 ===== -->
      <section v-if="detail.currentExhibitions?.length" class="exh-section">
        <div class="section-header">
          <div class="section-header-left">
            <span class="section-bar"></span>
            <div class="section-title-group">
              <h2 class="section-title">正在展出</h2>
              <span class="section-sub">CURRENT</span>
            </div>
          </div>
        </div>
        <div class="exh-grid">
          <div v-for="(ex, idx) in detail.currentExhibitions.slice(0, 2)" :key="ex.id" class="exh-card" @click="router.push('/exhibition/' + ex.id)">
            <div class="exh-poster">
              <img :src="posters[idx % posters.length]" alt="" class="exh-img" />
            </div>
            <div class="exh-info">
              <p class="exh-title">展览：{{ ex.title }}</p>
              <p class="exh-time">时间：{{ fmtDate(ex.startDate, ex.endDate) }}</p>
              <span class="exh-price">{{ formatPrice(ex.price) }}</span>
            </div>
          </div>
        </div>
      </section>

      <!-- ===== 模块四：往期展出 ===== -->
      <section v-if="detail.pastExhibitions?.length" class="exh-section">
        <div class="section-header">
          <div class="section-header-left">
            <span class="section-bar section-bar--gray"></span>
            <div class="section-title-group">
              <h2 class="section-title">往期展出</h2>
              <span class="section-sub">PAST</span>
            </div>
          </div>
        </div>
        <div class="exh-grid">
          <div v-for="(ex, idx) in detail.pastExhibitions.slice(0, 2)" :key="ex.id" class="exh-card" @click="router.push('/exhibition/' + ex.id)">
            <div class="exh-poster">
              <img :src="posters[(idx + 2) % posters.length]" alt="" class="exh-img" />
            </div>
            <div class="exh-info">
              <p class="exh-title">展览：{{ ex.title }}</p>
              <p class="exh-time">时间：{{ fmtDate(ex.startDate, ex.endDate) }}</p>
              <span class="exh-price">{{ formatPrice(ex.price) }}</span>
            </div>
          </div>
        </div>
      </section>
    </template>

    <FooterBar />
  </div>
</template>

<style scoped>
.detail-page { flex: 1; min-height: 0; overflow-y: auto; background: #E8EEF0; }

.status-wrap { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 100vh; gap: 16px; }
.spinner { width: 36px; height: 36px; border: 3px solid #e0e0e0; border-top-color: #5A5E61; border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.error-text { font-size: 16px; color: #e74c3c; }
.retry-btn { padding: 10px 32px; background: #5A5E61; color: #fff; border: none; border-radius: 8px; font-size: 14px; cursor: pointer; }

/* ===== 头部全屏 ===== */
.hero { width: 100%; height: 900px; position: relative; overflow: hidden; }
.hero-bg { width: 100%; height: 100%; object-fit: cover; display: block; }

/* 顶部遮罩 1440×80 */
.hero-top { position: absolute; top: 0; left: 0; right: 0; height: 80px; background: rgba(0,0,0,0.4); display: flex; align-items: center; padding: 0 40px; z-index: 2; }
.hero-back { background: none; border: none; color: #fff; font-size: 16px; cursor: pointer; padding: 0; }
.hero-back:hover { opacity: 0.7; }

/* 底部遮罩 1440×210 */
.hero-bottom { position: absolute; bottom: 0; left: 0; right: 0; height: 210px; background: rgba(0,0,0,0.6); display: flex; align-items: flex-end; justify-content: space-between; padding: 0 60px 32px; z-index: 2; box-sizing: border-box; }
.hero-left { display: flex; flex-direction: column; gap: 6px; }
.hero-name { font-size: 28px; font-weight: 700; color: #fff; margin: 0; }
.hero-line { font-size: 14px; color: rgba(255,255,255,0.85); margin: 0; display: flex; align-items: center; gap: 4px; }
.hero-icon { width: 14px; height: 14px; }

/* 收藏按钮 200×74 */
.hero-fav { width: 200px; height: 74px; background: #fff; border: none; border-radius: 37px; display: flex; align-items: center; justify-content: center; gap: 8px; cursor: pointer; font-size: 18px; color: #000; flex-shrink: 0; transition: transform 0.2s; }
.hero-fav:hover { transform: scale(1.05); }
.fav-icon { width: 28px; height: 28px; }

/* ===== 展馆简介 ===== */
.intro-section { padding: 60px 0; display: flex; flex-direction: column; align-items: center; }
.intro-title { font-size: 24px; font-weight: 700; color: #000; margin: 0; }
.intro-line { width: 40px; height: 2px; background: #555; margin: 12px 0; }
.intro-sub { font-size: 14px; color: #999; letter-spacing: 4px; margin: 0 0 32px; }
.intro-card { max-width: 1200px; background: #fff; border-radius: 12px; padding: 48px 60px; }
.intro-text { font-size: 16px; color: #333; line-height: 2; text-indent: 2em; margin: 0 0 16px; }
.intro-text:last-child { margin-bottom: 0; }

/* ===== 展览板块 ===== */
.exh-section { max-width: 1440px; margin: 0 auto; padding: 50px 40px 60px; }
.section-header { display: flex; align-items: center; margin-bottom: 24px; }
.section-header-left { display: flex; align-items: stretch; gap: 10px; }
.section-bar { width: 3px; background: #111; border-radius: 2px; }
.section-bar--gray { background: #888; }
.section-title-group { display: flex; flex-direction: column; gap: 2px; }
.section-title { font-size: 20px; font-weight: 700; color: #111; margin: 0; }
.section-sub { font-size: 13px; color: #aaa; text-transform: uppercase; letter-spacing: 1px; }

.exh-grid { display: grid; grid-template-columns: repeat(2, 616px); gap: 20px; justify-content: center; }
.exh-card { width: 616px; height: 550px; background: #fff; border-radius: 12px; overflow: hidden; }
.exh-poster { width: 616px; height: 370px; overflow: hidden; }
.exh-img { width: 100%; height: 100%; object-fit: cover; display: block; }
.exh-info { height: 180px; padding: 14px 20px; display: flex; flex-direction: column; justify-content: center; position: relative; }
.exh-title { font-size: 16px; font-weight: 700; color: #111; margin: 0 0 6px; }
.exh-time { font-size: 13px; color: #888; margin: 0; }
.exh-price { position: absolute; right: 20px; bottom: 14px; font-size: 18px; font-weight: 700; color: #e74c3c; }
</style>
