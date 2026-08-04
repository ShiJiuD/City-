<script setup lang="ts">
defineProps<{
  currentPage: number
  totalPages: number
}>()

const emit = defineEmits<{
  'page-change': [page: number]
}>()

function goTo(page: number) {
  emit('page-change', page)
}
</script>

<template>
  <div v-if="totalPages > 1" class="pagination">
    <!-- 上一页 -->
    <button
      class="pagination-btn"
      :disabled="currentPage === 1"
      @click="goTo(currentPage - 1)"
    >
      上一页
    </button>

    <!-- 页码 -->
    <div class="pagination-numbers">
      <button
        v-for="page in totalPages"
        :key="page"
        class="pagination-num"
        :class="{ 'pagination-num--active': page === currentPage }"
        @click="goTo(page)"
      >
        {{ page }}
      </button>
    </div>

    <!-- 下一页 -->
    <button
      class="pagination-btn"
      :disabled="currentPage === totalPages"
      @click="goTo(currentPage + 1)"
    >
      下一页
    </button>
  </div>
</template>

<style scoped>
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 40px;
  margin-bottom: 20px;
}

.pagination-btn {
  height: 38px;
  padding: 0 20px;
  background: #fff;
  border: 1px solid #d0d0d0;
  border-radius: 8px;
  font-size: 14px;
  color: #555;
  cursor: pointer;
  transition: background 0.2s, color 0.2s, border-color 0.2s;
}

.pagination-btn:hover:not(:disabled) {
  background: #f2f2f2;
  border-color: #aaa;
  color: #333;
}

.pagination-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.pagination-numbers {
  display: flex;
  gap: 6px;
}

.pagination-num {
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border: 1px solid #d0d0d0;
  border-radius: 8px;
  font-size: 14px;
  color: #555;
  cursor: pointer;
  transition: background 0.2s, color 0.2s, border-color 0.2s;
}

.pagination-num:hover {
  background: #f2f2f2;
  border-color: #aaa;
}

.pagination-num--active {
  background: #111;
  color: #fff;
  border-color: #111;
  border-radius: 50%;
}

.pagination-num--active:hover {
  background: #333;
  border-color: #333;
  color: #fff;
  border-radius: 50%;
}
</style>
