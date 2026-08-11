<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useCityStore } from '../stores/city'
import { updateProfile } from '../api/profile'
import FooterBar from '../components/FooterBar.vue'
import profileAvatar from '../assets/profile/Profile-Picture.png'

const router = useRouter()
const auth = useAuthStore()
const cityStore = useCityStore()

const nickname = ref('')
const city = ref('')
const saving = ref(false)
const message = ref('')

onMounted(() => {
  nickname.value = auth.user?.displayName || ''
  city.value = cityStore.currentCity || ''
})

function handleCancel() {
  router.push('/profile')
}

async function handleSave() {
  if (!nickname.value.trim()) {
    message.value = '昵称不能为空'
    return
  }
  saving.value = true
  message.value = ''
  try {
    const res = await updateProfile(nickname.value.trim())
    if (res.code === 1) {
      if (auth.user) auth.user.displayName = nickname.value.trim()
      message.value = '保存成功'
      setTimeout(() => router.push('/profile'), 600)
    } else {
      message.value = res.msg || '保存失败'
    }
  } catch (e: any) {
    message.value = e.message || '保存失败'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="edit-page">
    <div class="edit-content">
      <h1 class="edit-title">编辑个人资料</h1>
      <div class="edit-underline"></div>

      <div class="edit-card">
        <!-- 1. 头像 -->
        <div class="form-row">
          <span class="form-label">头像</span>
          <img :src="profileAvatar" alt="头像" class="avatar-img" />
        </div>

        <!-- 2. 昵称 -->
        <div class="form-row">
          <span class="form-label">昵称</span>
          <input
            v-model="nickname"
            type="text"
            class="form-input"
            placeholder="请输入昵称"
          />
        </div>

        <!-- 3. 所在城市 -->
        <div class="form-row">
          <span class="form-label">所在城市</span>
          <input
            v-model="city"
            type="text"
            class="form-input"
            placeholder="请输入城市"
          />
        </div>

        <!-- 4. 手机号 -->
        <div class="form-row">
          <span class="form-label">手机号</span>
          <input
            type="text"
            class="form-input"
            :value="auth.user?.phone || ''"
            disabled
          />
          <span class="form-hint">手机号暂不支持修改</span>
        </div>

        <p v-if="message" class="form-msg">{{ message }}</p>

        <!-- 按钮 -->
        <div class="form-btns">
          <button class="btn-cancel" @click="handleCancel">取消</button>
          <button class="btn-save" :disabled="saving" @click="handleSave">
            {{ saving ? '保存中...' : '保存修改' }}
          </button>
        </div>
      </div>
    </div>

    <FooterBar />
  </div>
</template>

<style scoped>
.edit-page { flex: 1; min-height: 0; background: #E8EEF0; overflow-y: auto; }

.edit-content { max-width: 1440px; margin: 0 auto; padding: 48px 40px 80px; }

.edit-title { font-size: 36px; font-weight: 700; color: #000; margin: 0 0 12px; }
.edit-underline { width: 140px; height: 3.2px; background: #000; margin-bottom: 32px; }

/* 卡片 1199×803 */
.edit-card { width: 1199px; height: 803px; background: #fff; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); padding: 40px 60px; box-sizing: border-box; display: flex; flex-direction: column; gap: 20px; }

.form-row { display: flex; flex-direction: column; gap: 10px; }
.form-label { font-size: 20px; font-weight: 400; color: #111; }

.avatar-img { width: 130px; height: 130px; border-radius: 50%; object-fit: cover; margin-left: 14px; }

.form-input { width: 1109px; height: 46px; padding: 0 16px; margin-left: 14px; border: 1px solid #d0d0d0; border-radius: 8px; font-size: 15px; color: #333; outline: none; transition: border-color 0.2s; }
.form-input:focus { border-color: #999; }
.form-input:disabled { background: #f5f5f5; color: #999; }

.form-hint { font-size: 12px; color: #bbb; }

.form-msg { font-size: 14px; color: #e74c3c; margin: 0; }

.form-btns { display: flex; justify-content: flex-end; gap: 33px; margin-top: auto; }
.btn-cancel, .btn-save { width: 135px; height: 44px; border-radius: 10px; font-size: 16px; cursor: pointer; transition: background 0.2s, transform 0.2s; border: none; }
.btn-cancel { background: #fff; color: #333; border: 1px solid #d0d0d0; }
.btn-cancel:hover { background: #f5f5f5; }
.btn-save { background: #000; color: #fff; }
.btn-save:hover:not(:disabled) { background: #333; }
.btn-save:disabled { opacity: 0.6; cursor: not-allowed; }
</style>
