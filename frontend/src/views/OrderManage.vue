<template>
  <div class="page-card">
    <div class="page-header">
      <div class="page-title-group">
        <h2 class="page-title">{{ isStaffOrAdmin ? '全校教材征订审核与发放台账' : '我的教材征订申报单' }}</h2>
        <span class="page-subtitle">
          {{ isStaffOrAdmin ? '按“审核 -> 开票 -> 备货出库 -> 领书确认”规程办理师生教材申领；若余量不足将自动触发缺书挂起' : '跟踪已提交课程教材征订申请的教务审核、开票与书库备货发放进度' }}
        </span>
      </div>
      <el-button type="primary" plain @click="$router.push('/books')" v-if="!isStaffOrAdmin">
        <el-icon style="margin-right: 4px;"><Plus /></el-icon>前往书目库申报教材
      </el-button>
    </div>

    <!-- 检索栏 -->
    <div class="search-bar">
      <el-select v-model="searchStatus" placeholder="全部办理状态" clearable style="width: 180px">
        <el-option label="待审核" :value="0" />
        <el-option label="已审核" :value="1" />
        <el-option label="已开票" :value="2" />
        <el-option label="待领取" :value="3" />
        <el-option label="已领取" :value="4" />
        <el-option label="已取消" :value="5" />
      </el-select>
      <el-button type="primary" @click="handleSearch">筛选订单</el-button>
      <el-button @click="handleReset">重置筛选</el-button>
    </div>

    <!-- 订单列表 -->
    <el-table :data="orders" v-loading="loading" stripe empty-text="暂无符合筛选条件的教材征订记录">
      <el-table-column prop="orderNo" label="征订单号" width="190">
        <template #default="{ row }">
          <span class="mono-code">{{ row.orderNo }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="user.realName" label="申报人" width="110" v-if="isStaffOrAdmin" />
      <el-table-column prop="book.name" label="教材名称" min-width="200" show-overflow-tooltip />
      <el-table-column prop="quantity" label="申领册数" width="95" align="center">
        <template #default="{ row }">{{ row.quantity }} 册</template>
      </el-table-column>
      <el-table-column prop="totalPrice" label="核算总价" width="115" align="right">
        <template #default="{ row }">
          <span class="price-text">¥{{ Number(row.totalPrice || 0).toFixed(2) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="流转状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="申报时间" width="175" />
      <el-table-column label="业务办理" width="240" fixed="right" align="center">
        <template #default="{ row }">
          <!-- 教务/书库工作人员操作 -->
          <template v-if="isStaffOrAdmin">
            <el-button type="success" link @click="handleReview(row, true)" v-if="row.status === 0">
              审核通过
            </el-button>
            <el-button type="danger" link @click="handleReview(row, false)" v-if="row.status === 0">
              驳回申请
            </el-button>
            <el-button type="primary" link @click="handleInvoice(row)" v-if="row.status === 1">
              开具领书单
            </el-button>
            <el-button type="warning" link @click="handlePreparePickup(row)" v-if="row.status === 2">
              核验出库
            </el-button>
            <el-button type="success" link @click="handleConfirmPickup(row)" v-if="row.status === 3">
              确认领书
            </el-button>
            <span v-if="row.status >= 4" style="color: var(--text-secondary); font-size: 12px;">已办结归档</span>
          </template>
          <!-- 师生用户操作 -->
          <template v-else>
            <el-button type="danger" link @click="handleCancel(row)" v-if="row.status <= 1">
              撤回申报
            </el-button>
            <span v-else style="color: var(--text-secondary); font-size: 12px;">书库流转中</span>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadOrders"
        @current-change="loadOrders"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../stores/user'
import {
  listOrders, getMyOrders, reviewOrder, invoiceOrder,
  preparePickup, confirmPickup, cancelOrder
} from '../api'

const userStore = useUserStore()
const isStaffOrAdmin = computed(() => userStore.hasRole(['ADMIN', 'STAFF']))

const loading = ref(false)
const orders = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const searchStatus = ref(null)

const getStatusType = (status) => {
  const types = ['warning', 'info', 'primary', 'primary', 'success', 'danger']
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = ['待审核', '已审核', '已开票', '待领取', '已领取', '已取消']
  return texts[status] || '未知'
}

const loadOrders = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: searchStatus.value
    }
    const res = isStaffOrAdmin.value
      ? await listOrders(params)
      : await getMyOrders(params)
    orders.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('加载订单失败', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  loadOrders()
}

const handleReset = () => {
  searchStatus.value = null
  pageNum.value = 1
  loadOrders()
}

const handleReview = (row, approved) => {
  const action = approved ? '审核通过' : '驳回'
  ElMessageBox.confirm(`确定要${action}单号为 ${row.orderNo} 的征订申请吗？`, '教务审核确认', { type: 'warning' })
    .then(async () => {
      await reviewOrder(row.id, { approved, remark: '' })
      ElMessage.success(`${action}操作完成`)
      loadOrders()
    })
    .catch(() => {})
}

const handleInvoice = (row) => {
  ElMessageBox.confirm('确定为该征订单开具领书结算凭证吗？', '开票确认', { type: 'warning' })
    .then(async () => {
      await invoiceOrder(row.id)
      ElMessage.success('开票成功')
      loadOrders()
    })
    .catch(() => {})
}

const handlePreparePickup = (row) => {
  ElMessageBox.confirm('确定核减馆藏库存并转为待领书状态吗？若余量不足将自动登记缺书。', '核验出库确认', { type: 'warning' })
    .then(async () => {
      await preparePickup(row.id)
      ElMessage.success('书库备货出库成功')
      loadOrders()
    })
    .catch(() => {})
}

const handleConfirmPickup = (row) => {
  ElMessageBox.confirm('确认申报师生已在书库窗口完成教材实领签收吗？', '领书签收确认', { type: 'warning' })
    .then(async () => {
      await confirmPickup(row.id)
      ElMessage.success('教材领用确认完成')
      loadOrders()
    })
    .catch(() => {})
}

const handleCancel = (row) => {
  ElMessageBox.confirm('确定要撤回该笔教材征订申请吗？', '撤回确认', { type: 'warning' })
    .then(async () => {
      await cancelOrder(row.id)
      ElMessage.success('征订申请已撤回')
      loadOrders()
    })
    .catch(() => {})
}

onMounted(() => {
  loadOrders()
})
</script>
