<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { sendCode, verifyCode, resetPassword } from '../api/auth'
import type { Role } from '../types'
import bgImage from '../assets/loginbackground.png'
import logoImage from '../assets/loginlogo.png'

const router = useRouter()

const step = ref(1)
const role = ref<Role>('user')

const form = reactive({
  phone: '',
  code: '',
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

async function handleSendCode() {
  if (!form.phone.trim()) { showToast('请输入手机号'); return }
  if (!/^1[3-9]\d{9}$/.test(form.phone.trim())) { showToast('手机号格式错误'); return }

  loading.value = true
  try {
    await sendCode({ phone: form.phone, role: role.value })
    showToast('验证码已发送（测试环境填 666666）')
    step.value = 2
  } catch (e: any) {
    showToast(e.message || '发送失败')
  } finally {
    loading.value = false
  }
}

async function handleVerifyCode() {
  if (!form.code.trim()) { showToast('请输入验证码'); return }

  loading.value = true
  try {
    await verifyCode({ phone: form.phone, code: form.code, role: role.value })
    showToast('验证通过，请设置新密码')
    step.value = 3
  } catch (e: any) {
    showToast(e.message || '验证码错误')
  } finally {
    loading.value = false
  }
}

async function handleResetPassword() {
  if (!form.password) { showToast('请输入新密码'); return }
  if (form.password.length < 6 || form.password.length > 16) { showToast('密码长度需为6-16位'); return }
  if (form.password !== form.confirmPassword) { showToast('两次密码输入不一致'); return }

  loading.value = true
  try {
    await resetPassword({ phone: form.phone, role: role.value, password: form.password })
    showToast('密码修改成功，即将跳转到登录页')
    setTimeout(() => router.push('/login'), 1500)
  } catch (e: any) {
    showToast(e.message || '修改失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="forgot-page" :style="{ backgroundImage: `url(${bgImage})` }">
    <Transition name="toast">
      <div v-if="toast.visible" class="toast" @click="closeToast">
        <span class="toast-icon">✕</span>
        <span class="toast-text">{{ toast.msg }}</span>
      </div>
    </Transition>

    <div class="forgot-layout">
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
        <div class="forgot-card">
          <h1 class="card-title">欢迎使用 艺览</h1>
          <p class="card-subtitle">找回密码</p>

          <!-- 步骤指示器 -->
          <div class="steps">
            <span :class="{ active: step === 1, done: step > 1 }">输入手机号</span>
            <span class="step-arrow">→</span>
            <span :class="{ active: step === 2, done: step > 2 }">验证身份</span>
            <span class="step-arrow">→</span>
            <span :class="{ active: step === 3 }">设置密码</span>
          </div>

          <!-- 角色切换 -->
          <div class="role-tabs">
            <span :class="{ active: role === 'user' }" @click="role = 'user'">普通用户</span>
            <span class="role-sep">|</span>
            <span :class="{ active: role === 'admin' }" @click="role = 'admin'">管理人员</span>
          </div>

          <!-- Step 1 -->
          <form v-if="step === 1" @submit.prevent="handleSendCode" class="forgot-form">
            <input v-model="form.phone" type="tel" maxlength="11" placeholder="请输入账号" class="form-input" />
            <button type="submit" :disabled="loading" class="forgot-btn">
              {{ loading ? '发送中...' : '获取验证码' }}
            </button>
          </form>

          <!-- Step 2 -->
          <form v-if="step === 2" @submit.prevent="handleVerifyCode" class="forgot-form">
            <input v-model="form.code" type="text" maxlength="6" placeholder="请输入验证码" class="form-input" />
            <button type="submit" :disabled="loading" class="forgot-btn">
              {{ loading ? '校验中...' : '验证' }}
            </button>
          </form>

          <!-- Step 3 -->
          <form v-if="step === 3" @submit.prevent="handleResetPassword" class="forgot-form">
            <input v-model="form.password" type="password" placeholder="请输入新密码（6-16位）" class="form-input" />
            <input v-model="form.confirmPassword" type="password" placeholder="请再次输入新密码" class="form-input" />
            <button type="submit" :disabled="loading" class="forgot-btn">
              {{ loading ? '提交中...' : '确认修改' }}
            </button>
          </form>

          <p class="footer-link">
            <router-link to="/login">返回登录</router-link>
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ===== 全局布局 ===== */
.forgot-page {
  width: 100vw; height: 100vh;
  background-size: cover; background-position: center; background-repeat: no-repeat;
}
.forgot-layout {
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
  width: 480px; display: flex; align-items: center; justify-content: center;
  padding: 6vh 60px 6vh 30px; flex-shrink: 0;
}
.forgot-card {
  width: 100%; background: #fff; border-radius: 20px;
  padding: 52px 44px 44px;
  box-shadow: 0 8px 60px rgba(0, 0, 0, 0.15);
}

/* ===== 卡片头部 ===== */
.card-title { font-size: 28px; font-weight: 700; margin: 0; color: #111; }
.card-subtitle {
  font-size: 18px; color: #1a1a1a; font-weight: 500;
  margin: 6px 0 0; padding-bottom: 8px;
  border-bottom: 3px solid #1a1a1a; display: inline-block;
}

/* ===== 步骤指示器 ===== */
.steps {
  display: flex; align-items: center; justify-content: center; gap: 6px;
  margin-top: 28px; margin-bottom: 6px; font-size: 14px;
}
.steps span { color: #ccc; }
.steps span.active { color: #1a1a1a; font-weight: 700; }
.steps span.done { color: #52c41a; }
.step-arrow { color: #ddd !important; font-weight: 400 !important; }

/* ===== 角色切换 ===== */
.role-tabs {
  display: flex; align-items: center; justify-content: center; gap: 24px;
  margin-top: 24px; margin-bottom: 28px;
}
.role-tabs span {
  cursor: pointer; font-size: 17px; color: #1a1a1a;
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
.forgot-form {
  display: flex; flex-direction: column; gap: 18px;
}
.form-input {
  width: 100%; height: 48px; padding: 0 16px;
  border: 1px solid #bbb; border-radius: 8px;
  font-size: 15px; color: #333; outline: none; background: #fff;
  box-sizing: border-box; transition: border-color 0.2s;
}
.form-input::placeholder { color: #aaa; }
.form-input:focus { border-color: #1a1a1a; }

/* ===== 按钮 ===== */
.forgot-btn {
  width: 100%; height: 48px;
  background: #fff; color: #1a1a1a; border: 1px solid #1a1a1a; border-radius: 8px;
  font-size: 16px; cursor: pointer; transition: 0.2s;
  display: flex; align-items: center; justify-content: center;
}
.forgot-btn:hover { background: #f5f5f5; }
.forgot-btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* ===== 底部链接 ===== */
.footer-link {
  text-align: center; margin-top: 36px; font-size: 14px;
}
.footer-link a { color: #1a1a1a; text-decoration: none; }
.footer-link a:hover { opacity: 0.7; }
</style>
