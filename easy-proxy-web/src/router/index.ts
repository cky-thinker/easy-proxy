import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/',
            name: 'home',
            component: () => import('../views/TailwindView.vue'),
            meta: { requiresAuth: false }
        },
        {
            path: '/login',
            name: 'login',
            component: () => import('../views/LoginView.vue'),
            meta: { requiresAuth: false }
        },
        {
            path: '/proxy',
            name: 'proxy',
            component: () => import('../views/ProxyView.vue'),
            meta: { requiresAuth: true }
        },
        {
            path: '/tailwind',
            name: 'tailwind',
            component: () => import('../views/TailwindView.vue'),
            meta: { requiresAuth: false }
        },
    ],
})

// 路由守卫
router.beforeEach((to: any, from: any, next: any) => {
    const token = localStorage.getItem('token');
    const requiresAuth = to.meta.requiresAuth;

    if (requiresAuth && !token) {
        // 需要认证但没有token，跳转到登录页
        next('/login');
    } else if (to.path === '/login' && token) {
        // 已登录用户访问登录页，跳转到首页
        next('/');
    } else {
        next();
    }
});

export default router
