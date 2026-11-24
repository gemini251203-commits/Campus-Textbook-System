<template>
  <div>
    <!-- 入库流水账 -->
    <div class="page-card">
      <div class="page-header">
        <div class="page-title-group">
          <h2 class="page-title">书库教材入库流水账</h2>
          <span class="page-subtitle">记录采购到货验收与新书入编产生的全部入库批次凭证</span>
        </div>
        <el-tag type="success" effect="plain">入库台账凭证</el-tag>
      </div>

      <el-table :data="stockInList" v-loading="stockInLoading" stripe empty-text="暂无教材入库流水记录">
        <el-table-column prop="batchNo" label="入库批次号" width="190">
          <template #default="{ row }">
            <span class="mono-code">{{ row.batchNo || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="book.name" label="教材名称" min-width="210" show-overflow-tooltip />
        <el-table-column prop="book.isbn" label="标准书号 (ISBN)" width="185">
          <template #default="{ row }">
            <span class="mono-code">{{ row.book?.isbn || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="入库变动" width="110" align="center">
          <template #default="{ row }">
            <span class="success-num">+{{ row.quantity }} 册</span>
          </template>
        </el-table-column>
        <el-table-column prop="operator.realName" label="经办人" width="110" align="center">
          <template #default="{ row }">{{ row.operator?.realName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="台账摘要" min-width="160" show-overflow-tooltip />
        <el-table-column prop="createTime" label="入库登账时间" width="180" />
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="stockInPageNum"
          v-model:page-size="stockInPageSize"
          :total="stockInTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadStockIn"
          @current-change="loadStockIn"
        />
      </div>
    </div>

    <!-- 出库流水账 -->
    <div class="page-card">
      <div class="page-header">
        <div class="page-title-group">
          <h2 class="page-title">书库教材出库发放流水账</h2>
          <span class="page-subtitle">记录师生征订单核验出库与教材发放扣减明细，实现账实相符</span>
        </div>
        <el-tag type="warning" effect="plain">出库发放凭证</el-tag>
      </div>

      <el-table :data="stockOutList" v-loading="stockOutLoading" stripe empty-text="暂无教材出库发放记录">
        <el-table-column prop="book.name" label="教材名称" min-width="210" show-overflow-tooltip />
        <el-table-column prop="book.isbn" label="标准书号 (ISBN)" width="185">
          <template #default="{ row }">
            <span class="mono-code">{{ row.book?.isbn || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="出库变动" width="110" align="center">
          <template #default="{ row }">
            <span class="danger-num">-{{ row.quantity }} 册</span>
          </template>
        </el-table-column>
        <el-table-column prop="order.orderNo" label="关联征订单号" width="190">
          <template #default="{ row }">
            <span class="mono-code">{{ row.order?.orderNo || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="operator.realName" label="经办人" width="110" align="center">
          <template #default="{ row }">{{ row.operator?.realName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="发放摘要" min-width="160" show-overflow-tooltip />
        <el-table-column prop="createTime" label="出库登账时间" width="180" />
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="stockOutPageNum"
          v-model:page-size="stockOutPageSize"
          :total="stockOutTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadStockOut"
          @current-change="loadStockOut"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listStockIn, listStockOut } from '../api'

// 入库记录
const stockInLoading = ref(false)
const stockInList = ref([])
const stockInTotal = ref(0)
const stockInPageNum = ref(1)
const stockInPageSize = ref(10)

// 出库记录
const stockOutLoading = ref(false)
const stockOutList = ref([])
const stockOutTotal = ref(0)
const stockOutPageNum = ref(1)
const stockOutPageSize = ref(10)

const loadStockIn = async () => {
  stockInLoading.value = true
  try {
    const res = await listStockIn({
      pageNum: stockInPageNum.value,
      pageSize: stockInPageSize.value
    })
    stockInList.value = res.data?.records || []
    stockInTotal.value = res.data?.total || 0
  } catch (error) {
    console.error('加载入库记录失败', error)
  } finally {
    stockInLoading.value = false
  }
}

const loadStockOut = async () => {
  stockOutLoading.value = true
  try {
    const res = await listStockOut({
      pageNum: stockOutPageNum.value,
      pageSize: stockOutPageSize.value
    })
    stockOutList.value = res.data?.records || []
    stockOutTotal.value = res.data?.total || 0
  } catch (error) {
    console.error('加载出库记录失败', error)
  } finally {
    stockOutLoading.value = false
  }
}

onMounted(() => {
  loadStockIn()
  loadStockOut()
})
</script>
