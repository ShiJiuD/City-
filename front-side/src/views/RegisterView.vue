<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { registerUser } from '../api/auth'
import bgImage from '../assets/loginbackground.png'
import logoImage from '../assets/loginlogo.png'

const router = useRouter()

const form = reactive({
  phone: '',
  password: '',
  confirmPassword: '',
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

async function handleRegister() {
  if (!form.phone.trim()) { showToast('请输入手机号'); return }
  if (!/^1[3-9]\d{9}$/.test(form.phone.trim())) { showToast('手机号格式错误'); return }
  if (!form.password) { showToast('请输入密码'); return }
  if (form.password.length < 6 || form.password.length > 16) { showToast('密码长度需为6-16位'); return }
  if (form.password !== form.confirmPassword) { showToast('两次密码输入不一致'); return }

  loading.value = true
  try {
    await registerUser({ phone: form.phone, password: form.password })
    router.push({ name: 'login', query: { success: '注册成功，请登录' } })
  } catch (e: any) {
    showToast(e.message || '注册失败，请重试')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="reg-page" :style="{ backgroundImage: `url(${bgImage})` }">
    <Transition name="toast">
      <div v-if="toast.visible" class="toast" @click="closeToast">
        <span class="toast-icon">✕</span>
        <span class="toast-text">{{ toast.msg }}</span>
      </div>
    </Transition>

    <div class="reg-layout">
      <div class="left-section">
        <div class="left-content">
          <img :src="logoImage" alt="logo" class="logo-img" />
          <div class="left-text">
            <h2 class="slogan">艺览，看见每一场展览</h2>
            <p class="subtitle">基于AI个性化推荐美术展览</p>
          </div>
        </div>
      </div>

      <div class="right-section">
        <div class="reg-card">
          <h1 class="card-title">欢迎使用 艺览</h1>
          <p class="card-subtitle">账号注册</p>

          <form @submit.prevent="handleRegister" class="reg-form">
            <input v-model="form.phone" type="tel" maxlength="11" placeholder="请输入账号" class="form-input" />
            <input v-model="form.password" type="password" placeholder="请输入密码（6-16位）" class="form-input" />
            <input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" class="form-input" />

            <button type="submit" :disabled="loading" class="reg-btn">
              {{ loading ? '注册中...' : '注册' }}
            </button>
          </form>

          <p class="footer-link">
            已有账号？<router-link to="/login">去登录</router-link>
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ===== 全局布局 ===== */
.reg-page {
  width: 100vw; height: 100vh;
  background-size: cover; background-position: center; background-repeat: no-repeat;
}
.reg-layout {
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
.logo-img { width: 251px; height: 122px; object-fit: contain; }
.left-text {
  margin-top: 120px; display: flex; flex-direction: column; align-items: center; gap: 10px;
}
.slogan { font-size: 42px; font-weight: 700; margin: 0; color: #111; }
.subtitle { font-size: 16px; font-weight: 400; margin: 0; color: #333; }

/* ===== 右侧卡片 ===== */
.right-section {
  width: 580px; display: flex; align-items: center; justify-content: center;
  padding: 6vh 60px 6vh 20px; flex-shrink: 0;
}
.reg-card {
  width: 100%; background: #fff; border-radius: 20px;
  padding: 60px 56px 56px;
  box-shadow: 0 8px 60px rgba(0, 0, 0, 0.15);
}

/* ===== 卡片头部 ===== */
.card-title { font-size: 32px; font-weight: 700; margin: 0; color: #111; }
.card-subtitle {
  font-size: 20px; color: #1a1a1a; font-weight: 500;
  margin: 8px 0 0; padding-bottom: 8px;
  border-bottom: 3px solid #1a1a1a; display: inline-block;
}

/* ===== 表单 ===== */
.reg-form {
  display: flex; flex-direction: column; gap: 20px;
  margin-top: 36px;
}
.form-input {
  width: 100%; height: 52px; padding: 0 18px;
  border: 1px solid #bbb; border-radius: 8px;
  font-size: 16px; color: #333; outline: none; background: #fff;
  box-sizing: border-box; transition: border-color 0.2s;
}
.form-input::placeholder { color: #aaa; }
.form-input:focus { border-color: #1a1a1a; }

/* ===== 按钮 ===== */
.reg-btn {
  width: 100%; height: 52px;
  background: #fff; color: #1a1a1a; border: 1px solid #1a1a1a; border-radius: 8px;
  font-size: 17px; cursor: pointer; transition: 0.2s;
  display: flex; align-items: center; justify-content: center;
}
.reg-btn:hover { background: #f5f5f5; }
.reg-btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* ===== 底部链接 ===== */
.footer-link {
  text-align: center; margin-top: 40px; font-size: 15px; color: #1a1a1a;
}
.footer-link a { color: #1a1a1a; text-decoration: none; font-weight: 500; }
</style>
