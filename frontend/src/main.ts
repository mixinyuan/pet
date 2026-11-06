/**
 * 应用入口：Pinia → Router → Element Plus 依次安装。
 * bindRouter 在挂载前调用，保证 axios 401 拦截器随时可执行路由跳转。
 */
import { createPinia } from 'pinia'
import { createApp, watch } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'

import { bindRouter } from '@/api/http'
import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/auth'
import { useCartStore } from './stores/cart'
import { useFavoriteStore } from './stores/favorite'
import './styles/base.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

bindRouter(router)

// 身份变化（登录成功/登出/401 清理）时统一重置跨用户状态并按新身份重拉购物车，
// 对齐旧页 loadStorefrontData / lockPage 行为——避免下一个用户看到上一个用户的券与收藏。
const auth = useAuthStore()
const cart = useCartStore()
const favorites = useFavoriteStore()
watch(
  () => auth.token,
  () => {
    favorites.resetFavorites()
    cart.resetCoupons()
    cart.closeDrawer()
    cart.loadCart().catch(() => cart.resetLocal())
  },
)

app.mount('#app')
