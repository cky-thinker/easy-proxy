/// <reference types="vite/client" />

// 声明vue-router模块，确保TypeScript能够正确识别
declare module 'vue-router' {
  import { RouteRecordRaw, Router } from 'vue-router'
  export { RouteRecordRaw, Router }
  export function createRouter(options: any): Router
  export function createWebHistory(base?: string): any
  export const RouterLink: any
  export const RouterView: any
}