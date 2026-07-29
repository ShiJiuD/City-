<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import type { Role } from '../types'
import bgImage from '../assets/loginbackground.png'
import logoImage from '../assets/loginlogo.png'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const role = ref<Role>('user')
const showPwd = ref(false)

const form = reactive({
  phone: '',
  password: '',
  remember: false,
})

const loading = ref(false)

// ===== Toast =====
const toast = ref({ visible: false, msg: '' })
let toastTimer: ReturnType<typeof setTimeout> | null = null

function showToast(msg: string) {
  if (toastTimer) clearTimeout(toastTimer)
  toast.value = { visible: true, msg }
  toastTimer = setTimeout(() => { toast.value.visible = false }, 3000)
}

function closeToast() {
  if (toastTimer) clearTimeout(toastTimer)
  toast.value.visible = false
}

onMounted(() => {
  const reason = route.query.reason
  const success = route.query.success
  const msg = success || reason
  if (msg && typeof msg === 'string') {
    showToast(msg)
    router.replace({ query: {} })
  }
})

async function handleLogin() {
  if (!form.phone.trim()) { showToast('请输入账号'); return }
  if (!/^1[3-9]\d{9}$/.test(form.phone.trim())) { showToast('手机号格式错误'); return }
  if (!form.password) { showToast('请输入密码'); return }

  loading.value = true
  try {
    await auth.login(role.value, form.phone, form.password)
    localStorage.setItem('role', role.value)
    if (form.remember) {
      localStorage.setItem('rememberPhone', form.phone)
    } else {
      localStorage.removeItem('rememberPhone')
    }
    router.push('/home')
  } catch (e: any) {
    showToast(e.message || '登录失败，请重试')
  } finally {
    loading.value = false
  }
}

const rememberedPhone = localStorage.getItem('rememberPhone')
if (rememberedPhone) {
  form.phone = rememberedPhone
  form.remember = true
}
</script>

<template>
  <div class="login-page" :style="{ backgroundImage: `url(${bgImage})` }">
    <Transition name="toast">
      <div v-if="toast.visible" class="toast" @click="closeToast">
        <span class="toast-icon">✕</span>
        <span class="toast-text">{{ toast.msg }}</span>
      </div>
    </Transition>

    <div class="login-layout">
      <!-- ===== 左侧 ===== -->
      <div class="left-section">
        <div class="left-content">
          <img :src="logoImage" alt="logo" class="logo-img" />
          <div class="left-text">
            <h2 class="slogan">艺览，看见每一场展览</h2>
            <p class="subtitle">基于AI个性化推荐美术展览</p>
          </div>
        </div>
      </div>

      <!-- ===== 右侧卡片 ===== -->
      <div class="right-section">
        <div class="login-card">
          <h1 class="card-title">欢迎使用 艺览</h1>
          <p class="card-subtitle">账号登录</p>

          <div class="role-tabs">
            <span :class="{ active: role === 'user' }" @click="role = 'user'">普通用户</span>
            <span class="role-sep">|</span>
            <span :class="{ active: role === 'admin' }" @click="role = 'admin'">管理人员</span>
          </div>

          <form @submit.prevent="handleLogin" class="login-form">
            <input v-model="form.phone" type="tel" maxlength="11" placeholder="请输入账号" class="form-input" />

            <div class="pwd-wrap">
              <input v-model="form.password" :type="showPwd ? 'text' : 'password'" maxlength="16" placeholder="请输入密码" class="form-input" />
              <span class="pwd-eye" @click="showPwd = !showPwd">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                  <circle cx="12" cy="12" r="3"/>
                  <line v-if="showPwd" x1="1" y1="1" x2="23" y2="23"/>
                </svg>
              </span>
            </div>

            <label class="remember-row">
              <input type="checkbox" v-model="form.remember" />
              <span>记住账号</span>
            </label>

            <button type="submit" :disabled="loading" class="login-btn">
              {{ loading ? '登录中...' : '登录' }}
            </button>
          </form>

          <div class="footer-links">
            <router-link to="/forgot-password">忘记密码</router-link>
            <router-link to="/register">账号注册</router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ===== 全局布局 ===== */
.login-page {
  width: 100vw; height: 100vh;
  background-size: cover; background-position: center; background-repeat: no-repeat;
}
.login-layout {
  display: flex; max-width: 1440px; margin: 0 auto; width: 100%; height: 100%;
}

/* ===== Toast ===== */
.toast {
  position: fixed; top: 40px; left: 50%; transform: translateX(-50%);
  display: flex; align-items: center; gap: 8px; padding: 10px 20px;
  background: #fff0f0; border: 1px solid #ffd4d4; border-radius: 6px;
  cursor: pointer; z-index: 9999;
}
.toast-icon {
  display: inline-flex; align-items: center; justify-content: center;
  width: 16px; height: 16px; border-radius: 50%; background: #ff4d4f;
  color: #fff; font-size: 9px; font-weight: bold; flex-shrink: 0;
}
.toast-text { font-size: 13px; color: #ff4d4f; }
.toast-enter-active, .toast-leave-active { transition: all 0.3s ease; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translateX(-50%) translateY(-12px); }

/* ===== 左侧 ===== */
.left-section {
  flex: 1; display: flex; align-items: center; justify-content: center;
}
.left-content {
  display: flex; flex-direction: column; align-items: center;
}
.logo-img {
  width: 251px; height: 122px; object-fit: contain;
}
.left-text {
  margin-top: 120px; display: flex; flex-direction: column; align-items: center; gap: 10px;
}
.slogan {
  font-size: 42px; font-weight: 700; margin: 0; color: #111;
}
.subtitle {
  font-size: 16px; font-weight: 400; margin: 0; color: #333;
}

/* ===== 右侧卡片 ===== */
.right-section {
  width: 580px; display: flex; align-items: center; justify-content: center;
  padding: 6vh 60px 6vh 20px; flex-shrink: 0;
}
.login-card {
  width: 100%; background: #fff; border-radius: 20px;
  padding: 60px 56px 56px;
  box-shadow: 0 8px 60px rgba(0, 0, 0, 0.15);
}

/* ===== 卡片头部 ===== */
.card-title {
  font-size: 32px; font-weight: 700; margin: 0; color: #111;
}
.card-subtitle {
  font-size: 20px; color: #1a1a1a; font-weight: 500;
  margin: 8px 0 0;
  padding-bottom: 8px;
  border-bottom: 3px solid #1a1a1a;
  display: inline-block;
}

/* ===== 角色切换 ===== */
.role-tabs {
  display: flex; align-items: center; justify-content: center; gap: 28px;
  margin-top: 36px; margin-bottom: 32px;
}
.role-tabs span {
  cursor: pointer; font-size: 18px; color: #1a1a1a;
  padding-bottom: 4px; border-bottom: 2px solid transparent; transition: 0.2s;
}
.role-tabs span.active {
  font-weight: 700; border-bottom: 3px solid #1a1a1a;
}
.role-sep {
  color: #ccc; cursor: default !important; padding-bottom: 0 !important;
  border-bottom: none !important; font-weight: 300 !important;
}

/* ===== 表单 ===== */
.login-form {
  display: flex; flex-direction: column; gap: 20px;
}
.form-input {
  width: 100%; height: 52px; padding: 0 18px;
  border: 1px solid #bbb; border-radius: 8px;
  font-size: 16px; color: #333; outline: none; background: #fff;
  box-sizing: border-box; transition: border-color 0.2s;
}
.form-input::placeholder { color: #aaa; }
.form-input:focus { border-color: #1a1a1a; }

/* 密码框 */
.pwd-wrap { position: relative; }
.pwd-wrap .form-input { padding-right: 48px; }
.pwd-eye {
  position: absolute; right: 14px; top: 50%; transform: translateY(-50%);
  cursor: pointer; display: flex; align-items: center; opacity: 0.5; transition: opacity 0.2s;
}
.pwd-eye:hover { opacity: 0.8; }

/* ===== 记住账号 ===== */
.remember-row {
  display: flex; align-items: center; gap: 8px; font-size: 16px; color: #1a1a1a; cursor: pointer;
}
.remember-row input[type='checkbox'] {
  accent-color: #1a1a1a; cursor: pointer; width: 18px; height: 18px; margin: 0;
}

/* ===== 登录按钮 ===== */
.login-btn {
  width: 100%; height: 52px;
  background: #fff; color: #1a1a1a; border: 1px solid #1a1a1a; border-radius: 8px;
  font-size: 17px; cursor: pointer; transition: 0.2s;
  display: flex; align-items: center; justify-content: center;
}
.login-btn:hover { background: #f5f5f5; }
.login-btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* ===== 底部链接 ===== */
.footer-links {
  display: flex; justify-content: space-between; margin-top: 40px; padding: 0;
}
.footer-links a {
  font-size: 15px; color: #1a1a1a; text-decoration: none;
}
.footer-links a:hover { opacity: 0.7; }
</style>
