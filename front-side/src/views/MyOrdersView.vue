<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import FooterBar from '../components/FooterBar.vue'
import Pagination from '../components/Pagination.vue'
import { getOrders, cancelOrder, refundOrder } from '../api/order'
import type { OrderVO, OrderItemVO } from '../types'
import all1 from '../assets/home/all1.jpg'
import all2 from '../assets/home/all2.jpg'
import all3 from '../assets/home/all3.png'
import all4 from '../assets/home/all4.png'
import all5 from '../assets/home/all5.jpg'
import all6 from '../assets/home/all6.jpg'

const router = useRouter()
const posters = [all1, all2, all3, all4, all5, all6]

const tabs = [
  { label: '全部', value: undefined },
  { label: '待支付', value: 0 },
  { label: '已支付', value: 1 },
  { label: '已取消', value: 2 },
  { label: '已退款', value: 3 },
]

const activeTab = ref<number | undefined>(undefined)
const loading = ref(true)
const orders = ref<OrderVO[]>([])
const currentPage = ref(1)
const pageSize = 4
const total = ref(0)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

const statusLabel: Record<number, string> = { 0: '待支付', 1: '已支付', 2: '已取消', 3: '已退款' }
const statusColor: Record<number, string> = { 0: '#f0ad4e', 1: '#5bc0de', 2: '#999', 3: '#5cb85c' }

async function fetchOrders() {
  loading.value = true
  try {
    const res = await getOrders({ status: activeTab.value, page: currentPage.value, size: pageSize })
    if (res.code === 1) {
      orders.value = res.data.records
      total.value = res.data.total
    }
  } catch { orders.value = []; total.value = 0 }
  finally { loading.value = false }
}

function switchTab(val: number | undefined) {
  if (activeTab.value === val) return
  activeTab.value = val
  currentPage.value = 1
}

async function handleCancel(orderNo: string) {
  try {
    const res = await cancelOrder(orderNo)
    if (res.code === 1) fetchOrders()
  } catch { /* ignore */ }
}

async function handleRefund(orderNo: string) {
  try {
    const res = await refundOrder(orderNo)
    if (res.code === 1) fetchOrders()
  } catch { /* ignore */ }
}

function goToPage(p: number) {
  if (p >= 1 && p <= totalPages.value) { currentPage.value = p }
}

function formatDate(d: string): string {
  return d?.replace('T', ' ').slice(0, 16) || ''
}

watch(activeTab, () => fetchOrders())
watch(currentPage, () => fetchOrders())
onMounted(() => fetchOrders())
</script>

<template>
  <div class="orders-page">
    <!-- 顶栏 -->
    <header class="sub-nav">
      <button class="sub-back" @click="router.push('/profile')">&lt; 返回</button>
      <div class="sub-breadcrumb">
        <span class="sub-breadcrumb-dim">个人中心 &gt;</span>
        <span class="sub-breadcrumb-active">我的订单</span>
      </div>
    </header>

    <div class="content">
      <!-- 标题 -->
      <div class="header">
        <h1 class="title">我的订单</h1>
        <p class="subtitle">管理您的展览预约订单</p>
      </div>

      <!-- Tab -->
      <div class="tab-bar">
        <button
          v-for="tab in tabs" :key="tab.label"
          :class="['tab', { active: activeTab === tab.value }]"
          @click="switchTab(tab.value)"
        >{{ tab.label }}</button>
      </div>

      <!-- 加载 -->
      <div v-if="loading" class="status-wrap"><div class="spinner"></div></div>

      <!-- 空 -->
      <div v-else-if="orders.length === 0" class="status-wrap">
        <p class="empty-text">暂无订单</p>
      </div>

      <!-- 订单卡片 -->
      <div v-else class="order-list">
        <div v-for="order in orders" :key="order.orderNo" class="order-card">
          <!-- 订单头部 -->
          <div class="order-head">
            <span class="order-no">订单号：{{ order.orderNo }}</span>
            <span class="order-status" :style="{ color: statusColor[order.status] }">
              {{ statusLabel[order.status] }}
            </span>
          </div>

          <!-- 每个明细 -->
          <div v-for="(item, idx) in order.items" :key="idx" class="order-item">
            <img :src="posters[item.exhibitionId % posters.length]" class="item-img" />
            <div class="item-info">
              <p class="item-title">{{ item.exhibitionTitle }}</p>
              <p class="item-line">票种：{{ item.ticketType }}</p>
              <p class="item-line">数量：{{ item.quantity }} 张</p>
              <p class="item-line">观展：{{ formatDate(item.visitDate) }}</p>
            </div>
            <div class="item-price">¥{{ item.unitPrice }}</div>
          </div>

          <!-- 订单底部 -->
          <div class="order-foot">
            <span class="order-time">下单时间：{{ formatDate(order.createTime) }}</span>
            <span class="order-amount">合计：¥{{ order.totalAmount }}</span>
            <button
              v-if="order.status === 0"
              class="btn btn-cancel"
              @click="handleCancel(order.orderNo)"
            >取消订单</button>
            <button
              v-if="order.status === 1"
              class="btn btn-refund"
              @click="handleRefund(order.orderNo)"
            >申请退款</button>
            <span v-if="order.status === 2" class="btn-label gray">订单已取消</span>
            <span v-if="order.status === 3" class="btn-label green">退款已到账</span>
          </div>
        </div>
      </div>

      <Pagination :current-page="currentPage" :total-pages="totalPages" @page-change="goToPage" />
    </div>

    <FooterBar />
  </div>
</template>

<style scoped>
.orders-page { flex: 1; min-height: 0; background: #EAEFF2; overflow-y: auto; display: flex; flex-direction: column; }

/* 顶栏 */
.sub-nav { height: 64px; background: #5A5E61; display: flex; align-items: center; justify-content: space-between; padding: 0 211px 0 77px; flex-shrink: 0; }
.sub-back { background: none; border: none; color: #fff; font-size: 16px; cursor: pointer; }
.sub-back:hover { opacity: 0.7; }
.sub-breadcrumb { display: flex; align-items: center; gap: 6px; font-size: 14px; }
.sub-breadcrumb-dim { color: rgba(255,255,255,0.6); }
.sub-breadcrumb-active { color: #fff; font-weight: 700; }

/* 内容 */
.content { max-width: 1198px; width: 100%; margin: 0 auto; padding: 0 40px 80px; }

/* 标题 */
.header { padding: 40px 0 0; }
.title { font-size: 36px; font-weight: 700; color: #000; margin: 0; }
.subtitle { font-size: 16px; color: #999; margin: 8px 0 0; }

/* Tab */
.tab-bar { display: flex; gap: 32px; margin-top: 28px; border-bottom: 1px solid #ddd; }
.tab { background: none; border: none; font-size: 18px; color: #999; cursor: pointer; padding: 0 0 10px; position: relative; }
.tab.active { color: #000; font-weight: 700; }
.tab.active::after { content: ''; position: absolute; bottom: -1px; left: 0; right: 0; height: 2px; background: #000; }

/* 状态 */
.status-wrap { display: flex; justify-content: center; padding-top: 100px; }
.spinner { width: 36px; height: 36px; border: 3px solid #e0e0e0; border-top-color: #5A5E61; border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.empty-text { font-size: 16px; color: #999; }

/* 订单列表 */
.order-list { display: flex; flex-direction: column; gap: 24px; margin-top: 28px; }

/* 订单卡片 */
.order-card { background: #fff; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); overflow: hidden; }

.order-head { display: flex; justify-content: space-between; align-items: center; padding: 16px 24px; border-bottom: 1px solid #f0f0f0; }
.order-no { font-size: 14px; color: #888; }
.order-status { font-size: 15px; font-weight: 700; }

/* 明细项 */
.order-item { display: flex; align-items: center; padding: 16px 24px; gap: 16px; border-bottom: 1px solid #f8f8f8; }
.item-img { width: 120px; height: 90px; object-fit: cover; border-radius: 8px; flex-shrink: 0; }
.item-info { flex: 1; display: flex; flex-direction: column; gap: 4px; }
.item-title { font-size: 16px; font-weight: 700; color: #000; margin: 0; }
.item-line { font-size: 13px; color: #888; margin: 0; }
.item-price { font-size: 18px; font-weight: 700; color: #e74c3c; }

/* 底部 */
.order-foot { display: flex; align-items: center; justify-content: flex-end; gap: 16px; padding: 14px 24px; background: #f5f5f5; }
.order-time { font-size: 13px; color: #bbb; margin-right: auto; }
.order-amount { font-size: 16px; font-weight: 600; color: #000; }

/* 按钮 */
.btn { padding: 8px 20px; border: none; border-radius: 6px; font-size: 14px; cursor: pointer; transition: opacity 0.2s; }
.btn:hover { opacity: 0.85; }
.btn-cancel { background: #f0f0f0; color: #666; }
.btn-refund { background: #e74c3c; color: #fff; }
.btn-label { font-size: 14px; font-weight: 600; }
.btn-label.gray { color: #999; }
.btn-label.green { color: #5cb85c; }
</style>
