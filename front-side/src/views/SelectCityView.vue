<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useCityStore } from '../stores/city'
import FooterBar from '../components/FooterBar.vue'
import searchIcon from '../assets/home/search.png'

const router = useRouter()
const cityStore = useCityStore()

// ===== 城市数据 =====
const popularCities = ['南京', '北京', '深圳', '上海', '苏州']
const allCities = ['上海', '北京', '南京', '广州', '深圳', '苏州', '长沙', '青岛']

// ===== 搜索 =====
const searchText = ref('')

/** 根据搜索词过滤全部城市 */
const filteredCities = computed(() => {
  const kw = searchText.value.trim()
  if (!kw) return allCities
  return allCities.filter(c => c.includes(kw))
})

/** 搜索时热门城市是否可见 */
const showPopular = computed(() => !searchText.value.trim())

// ===== 选择城市 =====
function selectCity(city: string) {
  cityStore.setCity(city)
  router.push('/home')
}

// ===== 申请开通 =====
function handleApply() {
  // TODO: 弹出申请开通弹窗或跳转
  alert('申请开通功能开发中，敬请期待')
}
</script>

<template>
  <div class="select-city-page">
    <!-- ===== 主内容区 ===== -->
    <main class="sc-main">
      <div class="sc-content">
        <!-- 标题区 -->
        <h1 class="sc-title">选择您所在的城市</h1>
        <p class="sc-subtitle">切换城市以查看该城市的美术馆与展览</p>
        <div class="sc-divider"></div>

        <!-- 模块1：当前城市 + 搜索 -->
        <section class="sc-section">
          <h2 class="section-heading">
            <span class="heading-bar"></span>
            当前城市 / CURRENT
          </h2>
          <div class="current-city-row">
            <span class="current-city-pill">{{ cityStore.currentCity || '未选择' }}</span>
            <div class="search-wrap">
              <input
                v-model="searchText"
                type="text"
                placeholder="搜索城市"
                class="city-search-input"
              />
              <img :src="searchIcon" alt="搜索" class="city-search-icon" />
            </div>
          </div>
        </section>

        <!-- 模块2：热门城市 -->
        <section v-if="showPopular" class="sc-section">
          <h2 class="section-heading">
            <span class="heading-bar"></span>
            热门城市 / POPULAR
          </h2>
          <div class="city-tags">
            <button
              v-for="city in popularCities"
              :key="city"
              :class="['city-tag', { selected: city === cityStore.currentCity }]"
              @click="selectCity(city)"
            >
              {{ city }}
            </button>
          </div>
        </section>

        <!-- 模块3：全部城市 -->
        <section class="sc-section">
          <h2 class="section-heading">
            <span class="heading-bar"></span>
            全部城市 / BOTH
          </h2>
          <div v-if="filteredCities.length" class="city-grid">
            <button
              v-for="city in filteredCities"
              :key="city"
              :class="['city-tag', { selected: city === cityStore.currentCity }]"
              @click="selectCity(city)"
            >
              {{ city }}
            </button>
          </div>
          <p v-else class="no-result">未找到匹配的城市</p>
        </section>

        <!-- 模块4：底部提示 -->
        <p class="bottom-tip">
          未找到所在城市？
          <button class="apply-link" @click="handleApply">申请开通</button>
        </p>
      </div>
    </main>

    <FooterBar />
  </div>
</template>

<style scoped>
/* ===== 页面容器 ===== */
.select-city-page {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #E8EEF0;
  overflow-y: auto;
}

/* ===== 主内容 ===== */
.sc-main {
  flex: 1;
  padding: 48px 20px 48px;
}

.sc-content {
  max-width: 720px;
  margin: 0 auto;
}

/* 标题 */
.sc-title {
  font-size: 28px;
  font-weight: 700;
  color: #111;
  text-align: center;
  margin: 0 0 8px;
}

.sc-subtitle {
  font-size: 14px;
  color: #999;
  text-align: center;
  margin: 0 0 20px;
}

.sc-divider {
  width: 100%;
  height: 1px;
  background: #d0d0d0;
  margin-bottom: 28px;
}

/* ===== Section 通用 ===== */
.sc-section {
  margin-bottom: 28px;
}

.section-heading {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 700;
  color: #111;
  margin: 0 0 16px;
}

.heading-bar {
  display: inline-block;
  width: 4px;
  height: 18px;
  background: #000;
  border-radius: 2px;
  flex-shrink: 0;
}

/* 当前城市胶囊 + 搜索 */
.current-city-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.current-city-pill {
  display: inline-block;
  padding: 10px 28px;
  background: #000;
  color: #fff;
  font-size: 15px;
  font-weight: 500;
  border-radius: 9999px;
  flex-shrink: 0;
}

/* 搜索框 */
.search-wrap {
  position: relative;
  flex: 1;
  max-width: 320px;
}

.city-search-input {
  width: 100%;
  height: 46px;
  padding: 0 48px 0 20px;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 9999px;
  font-size: 15px;
  color: #333;
  outline: none;
  box-sizing: border-box;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.city-search-input::placeholder {
  color: #bbb;
}

.city-search-input:focus {
  border-color: #000;
  box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.08);
}

.city-search-icon {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  width: 18px;
  height: 18px;
  object-fit: contain;
  pointer-events: none;
}

/* 城市标签 */
.city-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.city-tag {
  padding: 10px 28px;
  background: #fff;
  color: #333;
  border: 1px solid #333;
  border-radius: 10px;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.city-tag:hover {
  background: #f5f5f5;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.city-tag.selected {
  background: #000;
  color: #fff;
  border-color: #000;
}

/* 全部城市网格 */
.city-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.no-result {
  font-size: 14px;
  color: #bbb;
  padding: 16px 0;
}

/* 底部提示 */
.bottom-tip {
  text-align: center;
  font-size: 13px;
  color: #999;
  margin: 36px 0 0;
}

.apply-link {
  background: none;
  border: none;
  color: #C59B27;
  font-size: 13px;
  cursor: pointer;
  text-decoration: underline;
  padding: 0;
}

.apply-link:hover {
  opacity: 0.7;
}
</style>
