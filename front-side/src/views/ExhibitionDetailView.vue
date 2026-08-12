<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import FooterBar from '../components/FooterBar.vue'
import { getExhibitionDetail } from '../api/exhibition'
import type { ExhibitionDetailVO } from '../api/exhibition'
import { checkFavorite, addFavorite, cancelFavorite } from '../api/favorite'
import localIcon from '../assets/home/local.png'
import loveIcon from '../assets/detail/love.png'
import fullLoveIcon from '../assets/detail/fulllove.png'
import ticketIcon from '../assets/detail/ticket.png'
import all1 from '../assets/home/all1.jpg'
import all2 from '../assets/home/all2.jpg'
import all3 from '../assets/home/all3.png'
import all4 from '../assets/home/all4.png'
import all5 from '../assets/home/all5.jpg'
import all6 from '../assets/home/all6.jpg'

const posters = [all1, all2, all3, all4, all5, all6]

const router = useRouter()
const route = useRoute()
const exhibitionId = Number(route.params.id)

const loading = ref(true)
const error = ref('')
const detail = ref<ExhibitionDetailVO | null>(null)
const isFav = ref(false)

async function fetchDetail() {
  loading.value = true
  error.value = ''
  try {
    const res = await getExhibitionDetail(exhibitionId)
    if (res.code === 1) detail.value = res.data
    else error.value = res.msg || '加载失败'
  } catch (e: any) {
    error.value = e.message || '网络请求失败'
  } finally { loading.value = false }
}

async function fetchFavStatus() {
  try {
    const res = await checkFavorite({ targetType: 1, targetId: exhibitionId })
    if (res.code === 1) isFav.value = res.data.isFavorited
  } catch { /* ignore */ }
}

async function toggleFav() {
  try {
    if (isFav.value) {
      const res = await cancelFavorite({ targetType: 1, targetId: exhibitionId })
      if (res.code === 1) isFav.value = false
    } else {
      const res = await addFavorite({ targetType: 1, targetId: exhibitionId })
      if (res.code === 1) isFav.value = true
    }
  } catch { /* ignore */ }
}

function formatPrice(p: number): string {
  if (!p || p === 0) return '免费'
  return `¥${p}`
}

onMounted(() => {
  fetchDetail()
  fetchFavStatus()
})
</script>

<template>
  <div class="detail-page">
    <div v-if="loading" class="status-wrap">
      <div class="spinner"></div>
    </div>

    <div v-else-if="error" class="status-wrap">
      <p class="error-text">{{ error }}</p>
      <button class="retry-btn" @click="fetchDetail">重试</button>
    </div>

    <template v-else-if="detail">
      <!-- ===== 头部大图 ===== -->
      <div class="hero">
        <img :src="posters[exhibitionId % posters.length]" alt="" class="hero-bg" />
        <div class="hero-top">
          <button class="hero-back" @click="router.back()">&lt; 返回</button>
        </div>
        <div class="hero-bottom">
          <div class="hero-left">
            <h1 class="hero-name">{{ detail.title }}</h1>
            <p v-if="detail.subtitle" class="hero-subtitle">{{ detail.subtitle }}</p>
            <p class="hero-line">
              <img :src="localIcon" alt="" class="hero-icon" />
              地址：{{ detail.galleryName }} · {{ detail.galleryAddress }}
            </p>
            <p class="hero-line">展期：{{ detail.startDate }} — {{ detail.endDate }}</p>
          </div>
          <div class="hero-actions">
            <button class="hero-ticket" @click="router.push('/order/create/' + exhibitionId)">
              <img :src="ticketIcon" alt="" class="action-icon" />
              <span>购票</span>
            </button>
            <button class="hero-fav" @click="toggleFav">
              <img :src="isFav ? fullLoveIcon : loveIcon" alt="" class="action-icon" />
              <span>{{ isFav ? '已收藏' : '收藏' }}</span>
            </button>
          </div>
        </div>
      </div>

      <!-- ===== 展览简介 ===== -->
      <div class="intro-section">
        <h2 class="intro-title">展览简介</h2>
        <div class="intro-line"></div>
        <p class="intro-sub">INTRODUCTION</p>

        <div class="intro-card">
          <p class="intro-text">
            陆俨少是近现代山水画大家，在山水笔墨、构图造境上独树一帜。本次「咫尺千里」特展，汇集陆俨少手卷、册页、扇面三类小品佳作。作品画幅虽小巧，却可容纳千山万壑，于咫尺之间铺展千里山河，正是本展 "咫尺千里" 的内核意蕴。手卷徐徐展开，步步移步换景；册页方寸之内，一景一境各有意趣；扇面形制雅致，笔墨灵动洒脱。本次展出作品，既有江南烟雨的温润清逸，也有崇山峻岭的雄奇壮阔，可观陆俨少独有的勾云勾水、墨色变幻的艺术特色。
          </p>
          <p class="intro-text">
            本次展览为江苏省美术馆 90 周年系列展，借这批珍贵小品，带领观者感受传统中国画以小见大、以简驭繁的东方审美，体悟传统山水独有的意境与文人情怀。
          </p>
        </div>
      </div>
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

/* ===== 头部 ===== */
.hero { width: 100%; height: 900px; position: relative; overflow: hidden; }
.hero-bg { width: 100%; height: 100%; object-fit: cover; display: block; }

.hero-top { position: absolute; top: 0; left: 0; right: 0; height: 80px; background: rgba(0,0,0,0.4); display: flex; align-items: center; padding: 0 40px; z-index: 2; }
.hero-back { background: none; border: none; color: #fff; font-size: 16px; cursor: pointer; }
.hero-back:hover { opacity: 0.7; }

.hero-bottom { position: absolute; bottom: 0; left: 0; right: 0; height: 210px; background: rgba(0,0,0,0.6); display: flex; align-items: flex-end; justify-content: space-between; padding: 0 60px 32px; z-index: 2; box-sizing: border-box; }
.hero-left { display: flex; flex-direction: column; gap: 4px; }
.hero-name { font-size: 28px; font-weight: 700; color: #fff; margin: 0; }
.hero-subtitle { font-size: 14px; color: rgba(255,255,255,0.7); margin: 0; }
.hero-line { font-size: 14px; color: rgba(255,255,255,0.85); margin: 0; display: flex; align-items: center; gap: 4px; }
.hero-icon { width: 14px; height: 14px; }

.hero-actions { display: flex; gap: 16px; flex-shrink: 0; }
.hero-ticket, .hero-fav { width: 200px; height: 74px; border: none; border-radius: 37px; display: flex; align-items: center; justify-content: center; gap: 8px; cursor: pointer; font-size: 18px; color: #000; transition: transform 0.2s; }
.hero-ticket { background: #fff; }
.hero-ticket:hover, .hero-fav:hover { transform: scale(1.05); }
.hero-fav { background: #fff; }
.action-icon { width: 28px; height: 28px; }

/* ===== 简介 ===== */
.intro-section { padding: 60px 0; display: flex; flex-direction: column; align-items: center; }
.intro-title { font-size: 24px; font-weight: 700; color: #000; margin: 0; }
.intro-line { width: 40px; height: 2px; background: #555; margin: 12px 0; }
.intro-sub { font-size: 14px; color: #999; letter-spacing: 4px; margin: 0 0 32px; }
.intro-card { max-width: 1200px; background: #fff; border-radius: 12px; padding: 48px 60px; }
.intro-text { font-size: 16px; color: #333; line-height: 2; text-indent: 2em; margin: 0 0 16px; }
.intro-text:last-child { margin-bottom: 0; }
</style>
