<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { registerUser } from '../api/auth'

const router = useRouter()
const auth = useAuthStore()

const form = reactive({
  phone: '',
  password: '',
  confirmPassword: '',
})

const loading = ref(false)
const errorMsg = ref('')

async function handleRegister() {
  errorMsg.value = ''

  // 前端校验
  if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    errorMsg.value = '手机号格式错误'
    return
  }
  if (form.password.length < 6 || form.password.length > 16) {
    errorMsg.value = '密码长度需为6-16位'
    return
  }
  if (form.password !== form.confirmPassword) {
    errorMsg.value = '两次密码输入不一致'
    return
  }

  loading.value = true
  try {
    const res = await registerUser({ phone: form.phone, password: form.password })
    // 注册成功自动返回 token，直接登录
    const data = res.data
    localStorage.setItem('token', data.token)
    localStorage.setItem('role', 'user')
    auth.token = data.token
    auth.user = {
      id: data.id,
      phone: data.phone,
      role: 'user',
      displayName: data.nickname || '',
      status: 0,
    }
    router.push('/')
  } catch (e: any) {
    errorMsg.value = e.message || '注册失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="register-container">
    <h1>注册新账号</h1>

    <form @submit.prevent="handleRegister" class="register-form">
      <div class="form-item">
        <label>手机号</label>
        <input v-model="form.phone" type="tel" maxlength="11" placeholder="请输入11位手机号" />
      </div>
      <div class="form-item">
        <label>密码</label>
        <input v-model="form.password" type="password" placeholder="6-16位密码" />
      </div>
      <div class="form-item">
        <label>确认密码</label>
        <input v-model="form.confirmPassword" type="password" placeholder="再次输入密码" />
      </div>

      <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>

      <button type="submit" :disabled="loading" class="submit-btn">
        {{ loading ? '注册中...' : '注册' }}
      </button>
    </form>

    <p class="back-link">
      已有账号？<router-link to="/login">去登录</router-link>
    </p>
  </div>
</template>

<style scoped>
.register-container {
  max-width: 400px;
  margin: 80px auto;
  padding: 32px;
}
h1 { text-align: center; margin-bottom: 32px; }
.register-form { display: flex; flex-direction: column; gap: 16px; }
.form-item { display: flex; flex-direction: column; gap: 6px; }
.form-item label { font-size: 14px; color: #666; }
.form-item input {
  padding: 10px 12px; border: 1px solid #ddd;
  border-radius: 6px; font-size: 14px; outline: none;
}
.form-item input:focus { border-color: #1890ff; }
.error-msg { color: #ff4d4f; font-size: 13px; margin: 0; }
.submit-btn {
  padding: 12px; background: #1890ff; color: #fff;
  border: none; border-radius: 6px; font-size: 16px; cursor: pointer;
}
.submit-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.back-link { text-align: center; margin-top: 20px; font-size: 14px; }
.back-link a { color: #1890ff; text-decoration: none; }
</style>