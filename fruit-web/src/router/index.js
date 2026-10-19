import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/Login.vue') },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/upload',
    children: [
      { path: 'upload', name: 'upload', component: () => import('../views/Upload.vue'), meta: { title: '图片识别' } },
      { path: 'list', name: 'list', component: () => import('../views/ImageList.vue'), meta: { title: '识别记录' } },
      { path: 'detail/:id', name: 'detail', component: () => import('../views/ImageDetail.vue'), meta: { title: '识别详情' } },
      { path: 'stats', name: 'stats', component: () => import('../views/Stats.vue'), meta: { title: '数据统计' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 没 token 一律回登录页
router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) return '/login'
  if (to.path === '/login' && token) return '/'
  return true
})

export default router
