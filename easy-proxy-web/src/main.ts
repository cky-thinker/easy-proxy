import {createApp, createVNode} from 'vue'
import {createPinia} from 'pinia'
import * as Icons from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'

import './assets/styles/main.css'

const app = createApp(App)
for (const [key, component] of Object.entries(Icons)) {
  app.component(key, component)
}
// Icon自定组件
const Icon = (props: { icon: any }) => {
  const { icon } = props;
  // @ts-ignore
  return createVNode(Icons[icon]);
}
app.component("Icon", Icon);

app.use(createPinia())
app.use(router)

app.mount('#app')
