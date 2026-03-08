import { createRouter, createWebHistory } from 'vue-router'
import { checkInit } from '../api/auth';

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/',
            name: 'home',
            component: () => import('../views/DashboardView.vue'),
            meta: { requiresAuth: true }
        },
        {
            path: '/login',
            name: 'login',
            component: () => import('../views/LoginView.vue'),
            meta: { requiresAuth: false }
        },
        {
            path: '/init',
            name: 'init',
            component: () => import('../views/InitView.vue'),
            meta: { requiresAuth: false }
        },
        {
            path: '/dashboard',
            name: 'dashboard',
            component: () => import('../views/DashboardView.vue'),
            meta: { requiresAuth: true }
        },
        {
            path: '/clients',
            name: 'clients',
            component: () => import('../views/ClientManageView.vue'),
            meta: { requiresAuth: true }
        },
        {
            path: '/accounts',
            name: 'accounts',
            component: () => import('../views/UserManageView.vue'),
            meta: { requiresAuth: true }
        }
        ,
        {
            path: '/clientRules',
            name: 'clientRules',
            component: () => import('../views/ClientRulesView.vue'),
            meta: { requiresAuth: true }
        }
    ],
})

// 路由守卫
router.beforeEach(async (to: any, from: any, next: any) => {
    const token = localStorage.getItem('token');
    const requiresAuth = to.meta.requiresAuth;

    // 已登录用户
    if (token) {
        if (to.path === '/login' || to.path === '/init') {
            next('/');
        } else {
            next();
        }
        return;
    }

    // 未登录用户
    if (to.path === '/init') {
        try {
            const needInit = await checkInit();
            if (!needInit) {
                next('/login');
                return;
            }
            next();
        } catch (e) {
            console.error(e);
            next();
        }
        return;
    }

    if (to.path === '/login' || to.path === '/') {
        try {
            const needInit = await checkInit();
            if (needInit) {
                next('/init');
                return;
            }
        } catch (e) {
            console.error(e);
        }
    }

    if (requiresAuth) {
        next('/login');
    } else {
        next();
    }
});

export default router
