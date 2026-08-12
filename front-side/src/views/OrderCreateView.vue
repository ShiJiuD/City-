<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getExhibitionDetail } from '../api/exhibition'
import type { ExhibitionDetailVO } from '../api/exhibition'
import { createOrder } from '../api/order'
import all1 from '../assets/home/all1.jpg'
import all2 from '../assets/home/all2.jpg'
import all3 from '../assets/home/all3.png'
import all4 from '../assets/home/all4.png'
import all5 from '../assets/home/all5.jpg'
import all6 from '../assets/home/all6.jpg'

const router = useRouter()
const route = useRoute()
const exhibitionId = Number(route.params.exhibitionId)
const posters = [all1, all2, all3, all4, all5, all6]

const loading = ref(true)
const detail = ref<ExhibitionDetailVO | null>(null)
const error = ref('')

const ticketType = ref('成人票')
const visitDate = ref('')
const quantity = ref(1)
const submitting = ref(false)
const toast = ref('')

const ticketTypes = ['成人票']

const canSubmit = computed(() => ticketType.value && visitDate.value && quantity.value >= 1)
const totalPrice = computed(() => (detail.value?.price || 0) * quantity.value)

const dateOptions = computed(() => {
  if (!detail.value) return []
  const dates: string[] = []
  const start = new Date(detail.value.startDate)
  const end = new Date(detail.value.endDate)
  const cur = new Date(Math.max(start.getTime(), Date.now()))
  while (cur <= end) { dates.push(cur.toISOString().slice(0, 10)); cur.setDate(cur.getDate() + 1) }
  return dates
})

async function fetchDetail() {
  loading.value = true
  try {
    const res = await getExhibitionDetail(exhibitionId)
    if (res.code === 1) detail.value = res.data
    else error.value = res.msg || '加载失败'
  } catch (e: any) { error.value = e.message || '网络请求失败' }
  finally { loading.value = false }
}

async function handleSubmit() {
  if (!canSubmit.value || submitting.value) return
  submitting.value = true
  try {
    const res = await createOrder({
      items: [{ exhibitionId, ticketType: ticketType.value, quantity: quantity.value, visitDate: visitDate.value + ' 09:00:00' }],
    })
    if (res.code === 1) {
      toast.value = '下单成功！'
      setTimeout(() => { toast.value = ''; router.push('/profile/orders') }, 1500)
    } else {
      toast.value = res.msg || '下单失败'
      setTimeout(() => { toast.value = '' }, 2000)
    }
  } catch (e: any) {
    toast.value = e.message || '下单失败'
    setTimeout(() => { toast.value = '' }, 2000)
  } finally { submitting.value = false }
}

onMounted(() => fetchDetail())
</script>

<template>
  <div class="order-page">
    <!-- 顶部黑条 -->
    <div class="top-bar">
      <button class="top-back" @click="router.back()">‹ 返回</button>
    </div>

    <div v-if="loading" class="status-wrap"><div class="spinner"></div></div>
    <div v-else-if="error" class="status-wrap"><p class="err">{{ error }}</p></div>

    <template v-else-if="detail">
      <div class="content">

        <!-- 标题 -->
        <div class="header">
          <h1 class="title">我的订单</h1>
          <p class="subtitle">管理您的展览预约订单</p>
        </div>

        <!-- Tab 栏 -->
        <div class="tab-bar">
          <span v-for="tab in ['全部','待支付','已支付','已取消','已退款']" :key="tab" :class="['tab', { active: tab === '全部' }]">{{ tab }}</span>
        </div>

        <!-- 展览信息卡片 -->
        <div class="exh-card">
          <img :src="posters[exhibitionId % posters.length]" class="exh-img" />
          <div class="exh-info">
            <h2 class="exh-title">{{ detail.title }}</h2>
            <p class="exh-line">场馆：{{ detail.galleryName }}</p>
            <p class="exh-line exh-addr">{{ detail.galleryAddress }}</p>
            <p class="exh-line exh-time">展览时间：{{ detail.startDate }}~{{ detail.endDate }}</p>
            <p class="exh-price">¥{{ detail.price || 0 }}/人</p>
          </div>
        </div>

        <!-- 票种 -->
        <div class="section">
          <h3 class="sec-title">选择票种 <span class="req">*</span></h3>
          <div class="ticket-list">
            <div
              v-for="t in ticketTypes" :key="t"
              :class="['ticket-row', { sel: ticketType === t }]"
              @click="ticketType = t"
            >
              <span :class="['radio', { on: ticketType === t }]"></span>
              <span class="ticket-name">{{ t }}</span>
              <span class="ticket-price">¥{{ detail.price || 0 }}</span>
            </div>
          </div>
        </div>

        <!-- 日期 -->
        <div class="section">
          <h3 class="sec-title">选择观展日期 <span class="req">*</span></h3>
          <p class="sec-sub">DATE</p>
          <select v-model="visitDate" class="date-select">
            <option value="">请选择观展日期</option>
            <option v-for="d in dateOptions" :key="d" :value="d">{{ d }}</option>
          </select>
        </div>

        <!-- 数量 -->
        <div class="section">
          <h3 class="sec-title">购票数量 <span class="req">*</span></h3>
          <p class="sec-sub">QUANTITY</p>
          <div class="qty-box">
            <span class="qty-hint">选择张数（每人至多可购五张）</span>
            <div class="qty-ctrl">
              <button class="q-btn" @click="quantity > 1 && quantity--">−</button>
              <span class="q-num">{{ quantity }}</span>
              <button class="q-btn" @click="quantity < 5 && quantity++">+</button>
            </div>
          </div>
        </div>

        <!-- 观展须知 -->
        <div class="section">
          <h3 class="sec-title">观展须知</h3>
          <p class="sec-sub">NOTICE</p>
          <div class="notice">
            <p>1.订单生成后仅有30分钟支付时效，超时自动取消并释放库存。</p>
            <p>2.仅已支付订单申请退款。</p>
            <p>3.请确认观展日期，闭馆日无法核验入场。</p>
          </div>
        </div>

        <!-- 底部结算 -->
        <div class="bottom">
          <div class="bt-left">
            <span class="bt-label">合计金额：</span>
            <span class="bt-price">¥{{ totalPrice }}</span>
          </div>
          <button :class="['bt-submit', { on: canSubmit }]" :disabled="!canSubmit || submitting" @click="handleSubmit">
            {{ submitting ? '提交中...' : '确认下单' }}
          </button>
        </div>

      </div>
    </template>

    <div v-if="toast" class="toast">{{ toast }}</div>
  </div>
</template>

<style scoped>
.order-page { flex: 1; min-height: 0; overflow-y: auto; background: #EAEFF2; }
.status-wrap { display: flex; justify-content: center; padding-top: 200px; }
.spinner { width: 36px; height: 36px; border: 3px solid #e0e0e0; border-top-color: #5A5E61; border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.err { color: #e74c3c; }

/* 顶部黑条 */
.top-bar { height: 64px; background: #000; display: flex; align-items: center; padding: 0 40px; }
.top-back { background: none; border: none; color: #fff; font-size: 20px; cursor: pointer; }

/* 内容区 */
.content { max-width: 1198px; margin: 0 auto; padding: 0 40px 40px; }

/* 标题 */
.header { padding: 36px 0 0; }
.title { font-size: 36px; font-weight: 700; color: #000; margin: 0; }
.subtitle { font-size: 16px; color: #999; margin: 8px 0 0; }

/* Tab */
.tab-bar { display: flex; gap: 32px; margin-top: 28px; border-bottom: 1px solid #ddd; padding-bottom: 0; }
.tab { font-size: 18px; color: #999; cursor: pointer; padding-bottom: 10px; position: relative; }
.tab.active { color: #000; font-weight: 700; }
.tab.active::after { content: ''; position: absolute; bottom: -1px; left: 0; right: 0; height: 2px; background: #000; }

/* 展览卡片 */
.exh-card { background: #fff; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); display: flex; margin-top: 32px; overflow: hidden; }
.exh-img { width: 280px; height: 360px; object-fit: cover; flex-shrink: 0; }
.exh-info { flex: 1; padding: 32px; display: flex; flex-direction: column; justify-content: center; gap: 10px; }
.exh-title { font-size: 22px; font-weight: 700; color: #000; margin: 0; }
.exh-line { font-size: 15px; color: #888; margin: 0; }
.exh-addr { font-size: 14px; }
.exh-time { font-size: 14px; }
.exh-price { font-size: 22px; font-weight: 700; color: #e74c3c; margin: 6px 0 0; }

/* 区块 */
.section { margin-top: 36px; }
.sec-title { font-size: 20px; font-weight: 700; color: #000; margin: 0; }
.req { color: #e74c3c; }
.sec-sub { font-size: 13px; color: #bbb; letter-spacing: 3px; margin: 4px 0 14px; }

/* 票种列表 */
.ticket-list { background: #fff; border-radius: 10px; overflow: hidden; }
.ticket-row { display: flex; align-items: center; height: 56px; padding: 0 24px; border-bottom: 1px solid #f0f0f0; cursor: pointer; }
.ticket-row:last-child { border-bottom: none; }
.ticket-name { font-size: 17px; color: #333; flex: 1; }
.ticket-price { font-size: 17px; font-weight: 600; color: #e74c3c; }
.radio { width: 22px; height: 22px; border-radius: 50%; border: 2px solid #d0d0d0; margin-right: 16px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; }
.radio.on { border-color: #e74c3c; }
.radio.on::after { content: ''; width: 10px; height: 10px; border-radius: 50%; background: #e74c3c; }

/* 日期 */
.date-select { width: 100%; height: 50px; padding: 0 20px; border: none; border-radius: 10px; background: #fff; font-size: 16px; color: #333; outline: none; appearance: none; background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%23999' stroke-width='2'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E"); background-repeat: no-repeat; background-position: right 16px center; }

/* 数量 */
.qty-box { background: #fff; border-radius: 10px; padding: 16px 24px; display: flex; align-items: center; justify-content: space-between; }
.qty-hint { font-size: 15px; color: #999; }
.qty-ctrl { display: flex; align-items: center; gap: 16px; }
.q-btn { width: 36px; height: 36px; border-radius: 50%; border: 1px solid #ddd; background: #f5f5f5; font-size: 20px; cursor: pointer; display: flex; align-items: center; justify-content: center; color: #555; }
.q-num { font-size: 20px; font-weight: 700; min-width: 28px; text-align: center; }

/* 须知 */
.notice { background: #fff; border-radius: 10px; padding: 18px 24px; font-size: 14px; color: #999; line-height: 2; }
.notice p { margin: 0; }

/* 底部结算 */
.bottom { display: flex; align-items: center; justify-content: space-between; margin-top: 40px; padding: 20px 24px; background: #747778; border-radius: 8px; }
.bt-left { display: flex; align-items: baseline; }
.bt-label { font-size: 18px; color: rgba(255,255,255,0.85); }
.bt-price { font-size: 32px; font-weight: 700; color: #fff; margin-left: 8px; }
.bt-submit { padding: 16px 48px; border: none; border-radius: 8px; font-size: 18px; font-weight: 600; cursor: pointer; background: #ccc; color: #fff; transition: all 0.3s; }
.bt-submit.on { background: #e74c3c; }
.bt-submit.on:hover { background: #c0392b; }
.bt-submit:disabled { cursor: not-allowed; }

.toast { position: fixed; top: 50%; left: 50%; transform: translate(-50%,-50%); background: rgba(0,0,0,0.8); color: #fff; padding: 16px 40px; border-radius: 8px; font-size: 18px; z-index: 999; }
</style>
