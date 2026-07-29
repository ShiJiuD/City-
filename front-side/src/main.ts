import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './style.css'
import App from './App.vue'
import router from './router/index.ts'

const app=createApp(App)

app.use(createPinia())   //  必须在 router 之前注册
app.use(router)

app.mount('#app')