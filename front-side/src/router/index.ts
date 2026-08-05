import { createRouter, createWebHashHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useCityStore } from '../stores/city'

// 懒加载路由页面
const HomeView = () => import('../views/HomeView.vue')
const LoginView = () => import('../views/LoginView.vue')
const RegisterView = () => import('../views/RegisterView.vue')
const ForgotPasswordView = () => import('../views/ForgotPasswordView.vue')
const ExhibitionsView = () => import('../views/ExhibitionsView.vue')
const GalleriesView = () => import('../views/GalleriesView.vue')
const SelectCityView = () => import('../views/SelectCityView.vue')

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', redirect: '/home' },
    {
      path: '/home',
      name: 'home',
      component: HomeView,
      meta: { requiresAuth: true },
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { guest: true },
    },
    {
      path: '/register',
      name: 'register',
      component: RegisterView,
      meta: { guest: true },
    },
    {
      path: '/forgot-password',
      name: 'forgotPassword',
      component: ForgotPasswordView,
      meta: { guest: true },
    },
    {
      path: '/exhibitions',
      name: 'exhibitions',
      component: ExhibitionsView,
      meta: { requiresAuth: true },
    },
    {
      path: '/galleries',
      name: 'galleries',
      component: GalleriesView,
      meta: { requiresAuth: true },
    },
    {
      path: '/select-city',
      name: 'selectCity',
      component: SelectCityView,
      meta: { requiresAuth: true },
    },
  ],
})

// ===== 全局前置守卫 =====
router.beforeEach(async (to) => {
  const auth = useAuthStore()

  // 如果已有 token 但 user 为空（页面刷新场景）→ 先调 profile 恢复
  if (localStorage.getItem('token') && !auth.user) {
    try {
      await auth.refreshProfile()
    } catch (e: any) {
      // refreshProfile 失败会自动清 token，带错误信息跳转登录页
      if (to.name !== 'login') {
        return {
          name: 'login',
          query: { reason: e.message || '登录已过期，请重新登录' },
        }
      }
    }
  }

  // 未登录用户访问需要认证的页面 → 跳转登录页
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { name: 'login' }
  }

  // 已登录用户访问登录/注册/忘记密码页 → 重定向到首页
  if (to.meta.guest && auth.isLoggedIn) {
    return { path: '/home' }
  }

  // ===== 城市选择守卫 =====
  // 已登录、访问需要认证的页面（非城市选择页本身）、且未选择城市 → 跳转城市选择页
  if (
    auth.isLoggedIn &&
    to.meta.requiresAuth &&
    to.name !== 'selectCity'
  ) {
    const cityStore = useCityStore()
    if (!cityStore.hasCity) {
      return { name: 'selectCity' }
    }
  }

  return true
})

export default router
