import {createRouter, createWebHistory} from 'vue-router'
import Layout from '../layout/index.vue'
import Client from '@/views/client/index.vue'
import User from '@/views/user/index.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: Layout,
      children: [{
        path: 'client',
        component: Client
      }, {
        path: 'User',
        component: User
      }]
    }
  ]
})

export default router
