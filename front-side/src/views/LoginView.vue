<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import type { Role } from '../types'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

// 角色切换（user / admin）
const role = ref<Role>('user')

// 表单数据
const form = reactive({
  phone: '',
  password: '',
})

// 加载态
const loading = ref(false)

// 顶部错误横幅
const errorBanner = ref({
  visible: false,
  msg: '',
})
let dismissTimer: ReturnType<typeof setTimeout> | null = null

// 显示顶部错误横幅，3秒后自动消失
function showError(msg: string) {
  // 清除旧定时器
  if (dismissTimer) clearTimeout(dismissTimer)
  errorBanner.value = { visible: true, msg }
  dismissTimer = setTimeout(() => {
    errorBanner.value.visible = false
  }, 3000)
}

// 手动关闭
function closeBanner() {
  if (dismissTimer) clearTimeout(dismissTimer)
  errorBanner.value.visible = false
}

// 读取路由守卫传来的错误原因
onMounted(() => {
  const reason = route.query.reason
  if (reason && typeof reason === 'string') {
    showError(reason)
    router.replace({ query: {} })
  }
})

async function handleLogin() {
  loading.value = true

  try {
    await auth.login(role.value, form.phone, form.password)
    localStorage.setItem('role', role.value)
    router.push('/home')
  } catch (e: any) {
    showError(e.message || '登录失败，请重试')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <!-- ===== 顶部错误横幅 ===== -->
    <Transition name="banner">
      <div v-if="errorBanner.visible" class="error-banner" @click="closeBanner">
        <span class="banner-icon">!</span>
        <span class="banner-text">{{ errorBanner.msg }}</span>
        <button class="banner-close">×</button>
      </div>
    </Transition>

    <div class="login-container">
      <h1>ciTY 美术馆</h1>

      <!-- 角色切换标签 -->
      <div class="role-tabs">
        <button
          :class="{ active: role === 'user' }"
          @click="role = 'user'"
        >普通用户</button>
        <button
          :class="{ active: role === 'admin' }"
          @click="role = 'admin'"
        >管理员</button>
      </div>

      <!-- 登录表单 -->
      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-item">
          <label>手机号</label>
          <input
            v-model="form.phone"
            type="tel"
            maxlength="11"
            placeholder="请输入11位手机号"
          />
        </div>

        <div class="form-item">
          <label>密码</label>
          <input
            v-model="form.password"
            type="password"
            maxlength="16"
            placeholder="请输入密码"
          />
        </div>

        <button type="submit" :disabled="loading" class="submit-btn">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>

      <!-- 底部链接 -->
      <div class="footer-links">
        <router-link to="/register">注册新账号</router-link>
        <router-link to="/forgot-password">忘记密码</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  background: #f7f8fa;
}

/* ===== 顶部错误横幅 ===== */
.error-banner {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 20px;
  background: #fff2f0;
  border-bottom: 1px solid #ffccc7;
  color: #ff4d4f;
  cursor: pointer;
  z-index: 1000;
}

.banner-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #ff4d4f;
  color: #fff;
  font-size: 12px;
  font-weight: bold;
  flex-shrink: 0;
}

.banner-text {
  flex: 1;
  font-size: 14px;
}

.banner-close {
  background: none;
  border: none;
  font-size: 18px;
  color: #ff4d4f;
  cursor: pointer;
  flex-shrink: 0;
  padding: 0 4px;
  line-height: 1;
}

/* 横幅进出动画 */
.banner-enter-active {
  transition: transform 0.3s ease, opacity 0.3s ease;
}
.banner-leave-active {
  transition: transform 0.3s ease, opacity 0.3s ease;
}
.banner-enter-from {
  transform: translateY(-100%);
  opacity: 0;
}
.banner-leave-to {
  transform: translateY(-100%);
  opacity: 0;
}

/* ===== 登录卡片 ===== */
.login-container {
  max-width: 400px;
  margin: 0 auto;
  padding: 80px 32px 32px;
}

h1 {
  text-align: center;
  margin-bottom: 32px;
}

.role-tabs {
  display: flex;
  margin-bottom: 24px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #ddd;
}

.role-tabs button {
  flex: 1;
  padding: 10px;
  border: none;
  background: #f5f5f5;
  cursor: pointer;
  font-size: 14px;
  transition: 0.2s;
}

.role-tabs button.active {
  background: #1890ff;
  color: #fff;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-item label {
  font-size: 14px;
  color: #666;
}

.form-item input {
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.form-item input:focus {
  border-color: #1890ff;
}

.submit-btn {
  padding: 12px;
  background: #1890ff;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  cursor: pointer;
  transition: opacity 0.2s;
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.footer-links {
  display: flex;
  justify-content: space-between;
  margin-top: 20px;
}

.footer-links a {
  font-size: 13px;
  color: #1890ff;
  text-decoration: none;
}
</style>
