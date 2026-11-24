import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '首页', icon: 'HomeFilled' }
      },
      {
        path: 'books',
        name: 'Books',
        component: () => import('../views/BookManage.vue'),
        meta: { title: '教材管理', icon: 'Reading', roles: ['ADMIN', 'STAFF', 'TEACHER', 'STUDENT'] }
      },
      {
        path: 'inventory',
        name: 'Inventory',
        component: () => import('../views/InventoryManage.vue'),
        meta: { title: '库存管理', icon: 'Box', roles: ['ADMIN', 'STAFF', 'PURCHASER'] }
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('../views/OrderManage.vue'),
        meta: { title: '订购管理', icon: 'ShoppingCart', roles: ['ADMIN', 'STAFF', 'TEACHER', 'STUDENT'] }
      },
      {
        path: 'shortages',
        name: 'Shortages',
        component: () => import('../views/ShortageManage.vue'),
        meta: { title: '缺书登记', icon: 'Warning', roles: ['ADMIN', 'STAFF', 'PURCHASER'] }
      },
      {
        path: 'purchases',
        name: 'Purchases',
        component: () => import('../views/PurchaseManage.vue'),
        meta: { title: '采购管理', icon: 'Goods', roles: ['ADMIN', 'PURCHASER'] }
      },
      {
        path: 'stock',
        name: 'Stock',
        component: () => import('../views/StockManage.vue'),
        meta: { title: '进出库记录', icon: 'Operation', roles: ['ADMIN', 'STAFF', 'PURCHASER'] }
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('../views/UserManage.vue'),
        meta: { title: '用户管理', icon: 'User', roles: ['ADMIN'] }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  
  if (to.path === '/login') {
    next()
  } else if (!token) {
    next('/login')
  } else {
    next()
  }
})

export default router
