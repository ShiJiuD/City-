import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const CITY_KEY = 'user_city'

export const useCityStore = defineStore('city', () => {
  // ===== 状态 =====
  const currentCity = ref<string>(localStorage.getItem(CITY_KEY) || '')

  // ===== 计算属性 =====
  const hasCity = computed(() => !!currentCity.value)

  // ===== 设置城市 =====
  function setCity(city: string) {
    currentCity.value = city
    localStorage.setItem(CITY_KEY, city)
  }

  // ===== 清除城市 =====
  function clearCity() {
    currentCity.value = ''
    localStorage.removeItem(CITY_KEY)
  }

  return { currentCity, hasCity, setCity, clearCity }
})
