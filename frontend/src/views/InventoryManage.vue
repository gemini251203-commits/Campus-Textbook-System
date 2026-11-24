<template>
  <div class="page-card">
    <div class="page-header">
      <div class="page-title-group">
        <h2 class="page-title">馆藏书库库存台账</h2>
        <span class="page-subtitle">实时监控中心书库各教材现存余量与安全库存警戒线，核验征订发放保障能力</span>
      </div>
    </div>

    <!-- 检索栏 -->
    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="检索教材名称 / ISBN 书号"
        style="width: 280px"
        clearable
        prefix-icon="Search"
        @keyup.enter="handleSearch"
      />
      <el-checkbox v-model="lowStockOnly" border @change="handleSearch">仅看低于警戒线书目</el-checkbox>
      <el-button type="primary" @click="handleSearch">查询库存</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <!-- 库存列表 -->
    <el-table :data="inventoryList" v-loading="loading" stripe empty-text="暂无符合条件的馆藏库存记录">
      <el-table-column prop="book.isbn" label="标准书号 (ISBN)" width="185">
        <template #default="{ row }">
          <span class="mono-code">{{ row.book?.isbn || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="book.name" label="教材名称" min-width="210" show-overflow-tooltip />
      <el-table-column prop="book.author" label="主编 / 著者" width="130" show-overflow-tooltip />
      <el-table-column prop="quantity" label="现存余量" width="120" align="center">
        <template #default="{ row }">
          <span :class="row.quantity <= row.warningQuantity ? 'danger-num' : 'success-num'">
            {{ row.quantity }} 册
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="warningQuantity" label="安全警戒线" width="120" align="center">
        <template #default="{ row }">
          <span>{{ row.warningQuantity }} 册</span>
        </template>
      </el-table-column>
      <el-table-column label="库存状态" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="row.quantity <= row.warningQuantity ? 'danger' : 'success'" size="small">
            {{ row.quantity <= row.warningQuantity ? '库存不足' : '馆藏充足' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="updateTime" label="最近盘点时间" width="180" />
      <el-table-column label="管理操作" width="140" fixed="right" align="center">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleEditWarning(row)">调整警戒线</el-button>
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
        @size-change="loadInventory"
        @current-change="loadInventory"
      />
    </div>
  </div>

  <!-- 设置预警对话框 -->
  <el-dialog v-model="warningDialogVisible" title="设置馆藏库存安全警戒线" width="420px">
    <el-form label-width="100px">
      <el-form-item label="教材名称">
        <span style="font-weight: 700;">{{ currentBook?.book?.name }}</span>
      </el-form-item>
      <el-form-item label="当前现存量">
        <span class="mono-code">{{ currentBook?.quantity }} 册</span>
      </el-form-item>
      <el-form-item label="安全警戒线">
        <el-input-number v-model="warningQuantity" :min="1" :max="1000" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="warningDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmitWarning" :loading="warningLoading">确认保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listInventory, updateWarningQuantity } from '../api'

const loading = ref(false)
const inventoryList = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')
const lowStockOnly = ref(false)

const warningDialogVisible = ref(false)
const warningLoading = ref(false)
const currentBook = ref(null)
const warningQuantity = ref(10)

const loadInventory = async () => {
  loading.value = true
  try {
    const res = await listInventory({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: searchKeyword.value,
      lowStock: lowStockOnly.value || undefined
    })
    inventoryList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('加载库存失败', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  loadInventory()
}

const handleReset = () => {
  searchKeyword.value = ''
  lowStockOnly.value = false
  pageNum.value = 1
  loadInventory()
}

const handleEditWarning = (row) => {
  currentBook.value = row
  warningQuantity.value = row.warningQuantity
  warningDialogVisible.value = true
}

const handleSubmitWarning = async () => {
  warningLoading.value = true
  try {
    await updateWarningQuantity(currentBook.value.id, warningQuantity.value)
    ElMessage.success('安全警戒线更新成功')
    warningDialogVisible.value = false
    loadInventory()
  } catch (error) {
    console.error('设置失败', error)
  } finally {
    warningLoading.value = false
  }
}

onMounted(() => {
  loadInventory()
})
</script>
