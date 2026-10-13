import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    ...(import.meta.env.VITE_WORKOS_DESKTOP === 'true' ? [{ path: '/desktop-data', name: 'desktop-data', component: () => import('@/pages/DesktopDataPage.vue') }] : []),
    { path: '/', redirect: '/today' },
    { path: '/today', name: 'today', component: () => import('@/pages/TodayPage.vue') },
    { path: '/inbox', name: 'inbox', component: () => import('@/pages/InboxPage.vue') },
    { path: '/items', name: 'items', component: () => import('@/pages/ItemsPage.vue') },
    { path: '/projects', name: 'projects', component: () => import('@/pages/ProjectsPage.vue') },
    {
      path: '/projects/:id',
      name: 'project-detail',
      component: () => import('@/pages/ProjectDetailPage.vue')
    },
    { path: '/daily', name: 'daily', component: () => import('@/pages/DailyPage.vue') }
  ]
})

export default router
