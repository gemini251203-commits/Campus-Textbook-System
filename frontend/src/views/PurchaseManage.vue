<template>
  <div class="page-card">
    <div class="page-header">
      <div class="page-title-group">
        <h2 class="page-title">教材采购补货计划管理</h2>
        <span class="page-subtitle">统筹管理出版社教材采购订单、发运跟进与到货验收入库，入库后自动补充中心书库余量</span>
      </div>
      <el-button type="primary" @click="handleAdd">
        <el-icon style="margin-right: 4px;"><Plus /></el-icon>新建采购计划单
      </el-button>
    </div>

    <!-- 检索栏 -->
    <div class="search-bar">
      <el-select v-model="searchStatus" placeholder="全部采购状态" clearable style="width: 180px">
        <el-option label="待采购" :value="0" />
        <el-option label="采购中" :value="1" />
        <el-option label="已入库" :value="2" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询采购单</el-button>
      <el-button @click="handleReset">重置条件</el-button>
    </div>

    <!-- 采购单列表 -->
    <el-table :data="purchases" v-loading="loading" stripe empty-text="暂无教材采购计划记录">
      <el-table-column prop="book.name" label="教材名称" min-width="210" show-overflow-tooltip />
      <el-table-column prop="book.isbn" label="标准书号 (ISBN)" width="185">
        <template #default="{ row }">
          <span class="mono-code">{{ row.book?.isbn || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="quantity" label="采购册数" width="105" align="center">
        <template #default="{ row }">
          <span class="mono-code">{{ row.quantity }} 册</span>
        </template>
      </el-table-column>
      <el-table-column prop="expectedDate" label="预计到货日期" width="130" align="center">
        <template #default="{ row }">{{ row.expectedDate || '-' }}</template>
      </el-table-column>
      <el-table-column prop="purchaser.realName" label="经办专员" width="110" align="center">
        <template #default="{ row }">{{ row.purchaser?.realName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="status" label="执行状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="建单时间" width="175" />
      <el-table-column label="采购执行" width="180" fixed="right" align="center">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleStart(row)" v-if="row.status === 0">
            启动采购
          </el-button>
          <el-button type="success" link @click="handleComplete(row)" v-if="row.status === 1">
            到货验收入库
          </el-button>
          <span v-if="row.status === 2" style="color: var(--text-secondary); font-size: 12px;">已验收入库</span>
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
        @size-change="loadPurchases"
        @current-change="loadPurchases"
      />
    </div>
  </div>

  <!-- 新建采购单对话框 -->
  <el-dialog v-model="addDialogVisible" title="新建教材采购计划单" width="520px">
    <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="108px">
      <el-form-item label="采购教材" prop="bookId">
        <el-select v-model="addForm.bookId" filterable placeholder="输入教材名称或 ISBN 检索" style="width: 100%">
          <el-option
            v-for="book in bookList"
            :key="book.id"
            :label="`${book.name} (${book.isbn || '无书号'})`"
            :value="book.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="计划采购册数" prop="quantity">
        <el-input-number v-model="addForm.quantity" :min="1" :max="10000" />
      </el-form-item>
      <el-form-item label="预计到货日期">
        <el-date-picker
          v-model="addForm.expectedDate"
          type="date"
          placeholder="选择预计到货日期"
          value-format="YYYY-MM-DD"
          style="width: 100%;"
        />
      </el-form-item>
      <el-form-item label="采购备注">
        <el-input v-model="addForm.remark" type="textarea" :rows="3" placeholder="填写发行商、合同批次或备注信息" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="addDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmitAdd" :loading="addLoading">提交采购单</el-button>
    </template>
  </el-dialog>

  <!-- 完成入库对话框 -->
  <el-dialog v-model="completeDialogVisible" title="教材到货验收入库单" width="440px">
    <el-form ref="completeFormRef" :model="completeForm" label-width="108px">
      <el-form-item label="教材名称">
        <span style="font-weight: 700;">{{ currentPurchase?.book?.name }}</span>
      </el-form-item>
      <el-form-item label="计划采购数">
        <span class="mono-code">{{ currentPurchase?.quantity }} 册</span>
      </el-form-item>
      <el-form-item label="实收合格册数">
        <el-input-number v-model="completeForm.actualQuantity" :min="1" :max="10000" />
      </el-form-item>
      <el-form-item label="验收备注">
        <el-input v-model="completeForm.remark" type="textarea" :rows="3" placeholder="填写到货批次验收情况" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="completeDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmitComplete" :loading="completeLoading">确认入库登账</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listPurchases, createPurchase, startPurchase, completePurchase, listBooks } from '../api'

const loading = ref(false)
const purchases = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const searchStatus = ref(null)

const bookList = ref([])
const addDialogVisible = ref(false)
const addLoading = ref(false)
const addFormRef = ref(null)
const addForm = reactive({
  bookId: null,
  quantity: 1,
  expectedDate: '',
  remark: ''
})

const addRules = {
  bookId: [{ required: true, message: '请选择教材', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入采购数量', trigger: 'blur' }]
}

const completeDialogVisible = ref(false)
const completeLoading = ref(false)
const currentPurchase = ref(null)
const completeForm = reactive({
  actualQuantity: 0,
  remark: ''
})

const getStatusType = (status) => {
  const types = ['warning', 'primary', 'success']
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = ['待采购', '采购中', '已入库']
  return texts[status] || '未知'
}

const loadPurchases = async () => {
  loading.value = true
  try {
    const res = await listPurchases({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: searchStatus.value
    })
    purchases.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('加载采购单失败', error)
  } finally {
    loading.value = false
  }
}

const loadBooks = async () => {
  try {
    const res = await listBooks({ pageNum: 1, pageSize: 1000 })
    bookList.value = res.data?.records || []
  } catch (error) {
    console.error('加载教材列表失败', error)
  }
}

const handleSearch = () => {
  pageNum.value = 1
  loadPurchases()
}

const handleReset = () => {
  searchStatus.value = null
  pageNum.value = 1
  loadPurchases()
}

const handleAdd = () => {
  addForm.bookId = null
  addForm.quantity = 1
  addForm.expectedDate = ''
  addForm.remark = ''
  addDialogVisible.value = true
}

const handleSubmitAdd = async () => {
  const valid = await addFormRef.value.validate().catch(() => false)
  if (!valid) return

  addLoading.value = true
  try {
    await createPurchase(addForm)
    ElMessage.success('采购计划单创建成功')
    addDialogVisible.value = false
    loadPurchases()
  } catch (error) {
    console.error('创建采购单失败', error)
  } finally {
    addLoading.value = false
  }
}

const handleStart = (row) => {
  ElMessageBox.confirm(`确定启动《${row.book?.name || '该教材'}》的采购发运流程吗？`, '采购启动确认', { type: 'warning' })
    .then(async () => {
      await startPurchase(row.id)
      ElMessage.success('已转为采购中状态')
      loadPurchases()
    })
    .catch(() => {})
}

const handleComplete = (row) => {
  currentPurchase.value = row
  completeForm.actualQuantity = row.quantity
  completeForm.remark = ''
  completeDialogVisible.value = true
}

const handleSubmitComplete = async () => {
  completeLoading.value = true
  try {
    await completePurchase(currentPurchase.value.id, completeForm)
    ElMessage.success('验收入库成功，馆藏库存已同步增加')
    completeDialogVisible.value = false
    loadPurchases()
  } catch (error) {
    console.error('入库失败', error)
  } finally {
    completeLoading.value = false
  }
}

onMounted(() => {
  loadPurchases()
  loadBooks()
})
</script>
