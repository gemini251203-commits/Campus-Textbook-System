<template>
  <div>
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-content">
          <h3>{{ stats.bookCount }}</h3>
          <p>教材总数</p>
        </div>
        <div class="stat-icon primary">
          <el-icon><Reading /></el-icon>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-content">
          <h3>{{ stats.orderCount }}</h3>
          <p>待审核订单</p>
        </div>
        <div class="stat-icon success">
          <el-icon><ShoppingCart /></el-icon>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-content">
          <h3>{{ stats.lowStockCount }}</h3>
          <p>库存预警数</p>
        </div>
        <div class="stat-icon warning">
          <el-icon><Warning /></el-icon>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-content">
          <h3>{{ stats.purchaseCount }}</h3>
          <p>待处理采购单</p>
        </div>
        <div class="stat-icon info">
          <el-icon><Goods /></el-icon>
        </div>
      </div>
    </div>

    <div class="page-card" v-if="hasAccess(['TEACHER', 'STUDENT'])">
      <div class="page-header">
        <h2 class="page-title">最近提交的订单</h2>
        <el-button type="primary" link @click="$router.push('/orders')">查看全部</el-button>
      </div>
      <el-table :data="myOrders" v-loading="ordersLoading" stripe>
        <el-table-column prop="orderNo" label="订单编号" width="180" />
        <el-table-column prop="book.name" label="教材名称" min-width="200" />
        <el-table-column prop="quantity" label="数量" width="90" />
        <el-table-column prop="totalPrice" label="金额" width="110">
          <template #default="{ row }">¥{{ row.totalPrice }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="180" />
      </el-table>
    </div>

    <div class="page-card" v-if="hasAccess(['ADMIN', 'STAFF', 'PURCHASER'])">
      <div class="page-header">
        <h2 class="page-title">低库存预警列表</h2>
        <el-button type="primary" link @click="$router.push('/inventory')">库存管理</el-button>
      </div>
      <el-table :data="lowStockList" v-loading="lowStockLoading" stripe>
        <el-table-column prop="book.name" label="教材名称" min-width="220" />
        <el-table-column prop="book.isbn" label="ISBN" width="180" />
        <el-table-column prop="quantity" label="当前库存" width="120" />
        <el-table-column prop="warningQuantity" label="预警阈值" width="120" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.quantity === 0 ? 'danger' : 'warning'" size="small">
              {{ row.quantity === 0 ? '缺货' : '库存偏低' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '../stores/user'
import { listBooks, getMyOrders, listOrders, getLowStockList, listPurchases } from '../api'

const userStore = useUserStore()

const stats = reactive({
  bookCount: 0,
  orderCount: 0,
  lowStockCount: 0,
  purchaseCount: 0
})

const myOrders = ref([])
const ordersLoading = ref(false)
const lowStockList = ref([])
const lowStockLoading = ref(false)

const hasAccess = (roles) => {
  return userStore.hasRole(roles) || userStore.isAdmin
}

const getStatusType = (status) => {
  const types = ['warning', 'info', 'primary', 'primary', 'success', 'danger']
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = ['待审核', '已审核', '已开票', '待领取', '已领取', '已取消']
  return texts[status] || '未知'
}

const loadData = async () => {
  try {
    const booksRes = await listBooks({ pageNum: 1, pageSize: 1 })
    stats.bookCount = booksRes.data?.total || 0

    if (hasAccess(['ADMIN', 'STAFF'])) {
      const ordersRes = await listOrders({ pageNum: 1, pageSize: 1, status: 0 })
      stats.orderCount = ordersRes.data?.total || 0
    }

    if (hasAccess(['ADMIN', 'STAFF', 'PURCHASER'])) {
      lowStockLoading.value = true
      const lowStockRes = await getLowStockList()
      lowStockList.value = lowStockRes.data || []
      stats.lowStockCount = lowStockList.value.length
      lowStockLoading.value = false
    }

    if (hasAccess(['ADMIN', 'PURCHASER'])) {
      const purchasesRes = await listPurchases({ pageNum: 1, pageSize: 1, status: 0 })
      stats.purchaseCount = purchasesRes.data?.total || 0
    }

    if (hasAccess(['TEACHER', 'STUDENT'])) {
      ordersLoading.value = true
      const myOrdersRes = await getMyOrders({ pageNum: 1, pageSize: 5 })
      myOrders.value = myOrdersRes.data?.records || []
      ordersLoading.value = false
    }
  } catch (error) {
    console.error('加载数据失败', error)
  }
}

onMounted(() => {
  loadData()
})
</script>
