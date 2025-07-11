import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/',
            name: 'home',
            component: () => import('../views/TailwindView.vue'),
        },
        {
            path: '/proxy',
            name: 'proxy',
            component: () => import('../views/ProxyView.vue'),
        },
        {
            path: '/tailwind',
            name: 'tailwind',
            component: () => import('../views/TailwindView.vue'),
        },
    ],
})

export default router
