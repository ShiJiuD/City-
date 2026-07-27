<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { sendCode, verifyCode, resetPassword } from '../api/auth'
import type { Role } from '../types'

const router = useRouter()

// 步骤：1=输入手机号, 2=输入验证码, 3=设置新密码
const step = ref(1)
const role = ref<Role>('user')

const form = reactive({
  phone: '',
  code: '',
  password: '',
  confirmPassword: '',
})

const loading = ref(false)
const errorMsg = ref('')
const successMsg = ref('')

/** 发送验证码 */
async function handleSendCode() {
  errorMsg.value = ''
  if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    errorMsg.value = '手机号格式错误'
    return
  }

  loading.value = true
  try {
    await sendCode({ phone: form.phone, role: role.value })
    successMsg.value = '验证码已发送（测试环境填 666666）'
    step.value = 2
  } catch (e: any) {
    errorMsg.value = e.message || '发送失败'
  } finally {
    loading.value = false
  }
}

/** 校验验证码 */
async function handleVerifyCode() {
  errorMsg.value = ''
  loading.value = true
  try {
    await verifyCode({ phone: form.phone, code: form.code, role: role.value })
    successMsg.value = '验证通过，请设置新密码'
    step.value = 3
  } catch (e: any) {
    errorMsg.value = e.message || '验证码错误'
  } finally {
    loading.value = false
  }
}

/** 重置密码 */
async function handleResetPassword() {
  errorMsg.value = ''
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
    await resetPassword({
      phone: form.phone,
      role: role.value,
      password: form.password,
    })
    alert('密码修改成功，即将跳转到登录页')
    router.push('/login')
  } catch (e: any) {
    errorMsg.value = e.message || '修改失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="forgot-container">
    <h1>找回密码</h1>

    <!-- 步骤提示 -->
    <div class="steps">
      <div :class="{ active: step >= 1, done: step > 1 }">输入手机号</div>
      <div :class="{ active: step >= 2, done: step > 2 }">验证身份</div>
      <div :class="{ active: step >= 3 }">设置密码</div>
    </div>

    <!-- 角色切换 -->
    <div class="role-tabs">
      <button :class="{ active: role === 'user' }" @click="role = 'user'">
        C端用户
      </button>
      <button :class="{ active: role === 'admin' }" @click="role = 'admin'">
        B端管理员
      </button>
    </div>

    <!-- Step 1: 输入手机号 -->
    <form v-if="step === 1" @submit.prevent="handleSendCode">
      <div class="form-item">
        <label>手机号</label>
        <input
          v-model="form.phone"
          type="tel"
          maxlength="11"
          placeholder="请输入11位手机号"
        />
      </div>
      <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>
      <button type="submit" :disabled="loading" class="submit-btn">
        {{ loading ? '发送中...' : '获取验证码' }}
      </button>
    </form>

    <!-- Step 2: 输入验证码 -->
    <form v-if="step === 2" @submit.prevent="handleVerifyCode">
      <p v-if="successMsg" class="success-msg">{{ successMsg }}</p>
      <div class="form-item">
        <label>验证码</label>
        <input
          v-model="form.code"
          type="text"
          maxlength="6"
          placeholder="请输入6位验证码"
        />
      </div>
      <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>
      <button type="submit" :disabled="loading" class="submit-btn">
        {{ loading ? '校验中...' : '验证' }}
      </button>
    </form>

    <!-- Step 3: 设置新密码 -->
    <form v-if="step === 3" @submit.prevent="handleResetPassword">
      <div class="form-item">
        <label>新密码</label>
        <input
          v-model="form.password"
          type="password"
          placeholder="6-16位新密码"
        />
      </div>
      <div class="form-item">
        <label>确认密码</label>
        <input
          v-model="form.confirmPassword"
          type="password"
          placeholder="再次输入新密码"
        />
      </div>
      <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>
      <button type="submit" :disabled="loading" class="submit-btn">
        {{ loading ? '提交中...' : '确认修改' }}
      </button>
    </form>

    <p class="back-link">
      <router-link to="/login">返回登录</router-link>
    </p>
  </div>
</template>

<style scoped>
.forgot-container {
  max-width: 400px;
  margin: 80px auto;
  padding: 32px;
}
h1 { text-align: center; margin-bottom: 24px; }

.steps {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-bottom: 24px;
  font-size: 13px;
  color: #999;
}
.steps .active { color: #1890ff; font-weight: bold; }
.steps .done { color: #52c41a; }

.role-tabs {
  display: flex;
  margin-bottom: 20px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #ddd;
}
.role-tabs button {
  flex: 1; padding: 8px; border: none;
  background: #f5f5f5; cursor: pointer; font-size: 13px;
}
.role-tabs button.active { background: #1890ff; color: #fff; }

.form-item { margin-bottom: 16px; }
.form-item label { display: block; font-size: 14px; color: #666; margin-bottom: 6px; }
.form-item input {
  width: 100%; padding: 10px 12px; border: 1px solid #ddd;
  border-radius: 6px; font-size: 14px; outline: none; box-sizing: border-box;
}
.form-item input:focus { border-color: #1890ff; }

.error-msg { color: #ff4d4f; font-size: 13px; margin: 0 0 12px; }
.success-msg { color: #52c41a; font-size: 13px; margin: 0 0 12px; }

.submit-btn {
  width: 100%; padding: 12px; background: #1890ff; color: #fff;
  border: none; border-radius: 6px; font-size: 16px; cursor: pointer;
}
.submit-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.back-link { text-align: center; margin-top: 20px; font-size: 14px; }
.back-link a { color: #1890ff; text-decoration: none; }
</style>