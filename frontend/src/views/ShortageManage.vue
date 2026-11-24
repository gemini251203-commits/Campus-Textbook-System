<template>
  <div class="page-card">
    <div class="page-header">
      <div class="page-title-group">
        <h2 class="page-title">教材缺书挂起登记台账</h2>
        <span class="page-subtitle">汇总因馆藏余量不足而自动挂起或人工上报的缺书书目，支持一键转派采购专员生成补货计划</span>
      </div>
    </div>

    <!-- 检索栏 -->
    <div class="search-bar">
      <el-select v-model="searchStatus" placeholder="全部处理状态" clearable style="width: 180px">
        <el-option label="待处理" :value="0" />
        <el-option label="已发送采购" :value="1" />
        <el-option label="已采购完成" :value="2" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询记录</el-button>
      <el-button @click="handleReset">重置条件</el-button>
    </div>

    <!-- 缺书登记列表 -->
    <el-table :data="shortages" v-loading="loading" stripe empty-text="当前暂无缺书挂起记录">
      <el-table-column prop="book.name" label="教材名称" min-width="210" show-overflow-tooltip />
      <el-table-column prop="book.isbn" label="标准书号 (ISBN)" width="185">
        <template #default="{ row }">
          <span class="mono-code">{{ row.book?.isbn || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="requiredQuantity" label="申报需求量" width="110" align="center">
        <template #default="{ row }">{{ row.requiredQuantity }} 册</template>
      </el-table-column>
      <el-table-column prop="currentQuantity" label="登记时馆藏" width="110" align="center">
        <template #default="{ row }">{{ row.currentQuantity }} 册</template>
      </el-table-column>
      <el-table-column label="缺口册数" width="110" align="center">
        <template #default="{ row }">
          <span class="danger-num">
            {{ Math.max(0, row.requiredQuantity - row.currentQuantity) }} 册
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="挂起状态" width="125" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="挂起登记时间" width="180" />
      <el-table-column label="调拨操作" width="170" fixed="right" align="center">
        <template #default="{ row }">
          <el-button
            type="primary"
            link
            @click="handleCreatePurchase(row)"
            v-if="row.status === 0"
          >
            生成补货采购单
          </el-button>
          <span v-else style="color: var(--text-secondary); font-size: 12px;">已转入采购流程</span>
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
        @size-change="loadShortages"
        @current-change="loadShortages"
      />
    </div>
  </div>

  <!-- 创建采购单对话框 -->
  <el-dialog v-model="purchaseDialogVisible" title="由缺书记录生成补货采购单" width="500px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="108px">
      <el-form-item label="缺书教材">
        <span style="font-weight: 700;">{{ currentShortage?.book?.name }}</span>
      </el-form-item>
      <el-form-item label="需求总册数">
        <span class="mono-code">{{ currentShortage?.requiredQuantity }} 册</span>
      </el-form-item>
      <el-form-item label="计划采购册数" prop="quantity">
        <el-input-number v-model="form.quantity" :min="1" :max="10000" />
      </el-form-item>
      <el-form-item label="预计到货日期">
        <el-date-picker
          v-model="form.expectedDate"
          type="date"
          placeholder="选择预计到货日期"
          value-format="YYYY-MM-DD"
          style="width: 100%;"
        />
      </el-form-item>
      <el-form-item label="采购说明">
        <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="可填写出版社征订批次或加急备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="purchaseDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitLoading">下达采购计划</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listShortages, createFromShortage } from '../api'

const loading = ref(false)
const shortages = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const searchStatus = ref(null)

const purchaseDialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)
const currentShortage = ref(null)
const form = reactive({
  quantity: 0,
  expectedDate: '',
  remark: ''
})

const rules = {
  quantity: [{ required: true, message: '请输入采购数量', trigger: 'blur' }]
}

const getStatusType = (status) => {
  const types = ['warning', 'primary', 'success']
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = ['待处理', '已发送采购', '已采购完成']
  return texts[status] || '未知'
}

const loadShortages = async () => {
  loading.value = true
  try {
    const res = await listShortages({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: searchStatus.value
    })
    shortages.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('加载缺书登记失败', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  loadShortages()
}

const handleReset = () => {
  searchStatus.value = null
  pageNum.value = 1
  loadShortages()
}

const handleCreatePurchase = (row) => {
  currentShortage.value = row
  form.quantity = Math.max(1, row.requiredQuantity - row.currentQuantity)
  form.expectedDate = ''
  form.remark = ''
  purchaseDialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    await createFromShortage({
      shortageId: currentShortage.value.id,
      quantity: form.quantity,
      expectedDate: form.expectedDate,
      remark: form.remark
    })
    ElMessage.success('补货采购单创建成功')
    purchaseDialogVisible.value = false
    loadShortages()
  } catch (error) {
    console.error('创建采购单失败', error)
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  loadShortages()
})
</script>
