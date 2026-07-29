<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

function handleLogout() {
  auth.logout()
}
</script>

<template>
  <div class="home">
    <header class="home-header">
      <span class="welcome">欢迎，{{ auth.user?.displayName }}</span>
      <button class="logout-btn" @click="handleLogout">退出登录</button>
    </header>
    <main class="home-main">
      <p>登陆成功</p>
    </main>
  </div>
</template>

<style scoped>
.home {
  min-height: 100vh;
  background: #f7f8fa;
}

.home-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: #fff;
  border-bottom: 1px solid #eee;
}

.welcome {
  font-size: 14px;
  color: #333;
}

.logout-btn {
  padding: 6px 16px;
  background: #fff;
  color: #ff4d4f;
  border: 1px solid #ff4d4f;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  transition: 0.2s;
}

.logout-btn:hover {
  background: #ff4d4f;
  color: #fff;
}

.home-main {
  padding: 40px;
  text-align: center;
}
</style>
