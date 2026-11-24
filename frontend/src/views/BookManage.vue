<template>
  <div class="page-card">
    <div class="page-header">
      <div class="page-title-group">
        <h2 class="page-title">高校教材标准书目库</h2>
        <span class="page-subtitle">维护全校各学科开课选用教材档案，任课教师与班级代表可直接发起在线征订</span>
      </div>
      <el-button type="primary" @click="handleAdd" v-if="userStore.hasRole(['ADMIN', 'STAFF'])">
        <el-icon style="margin-right: 4px;"><Plus /></el-icon>新增入编教材
      </el-button>
    </div>

    <!-- 检索条件栏 -->
    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="检索教材书名 / ISBN 书号 / 主编作者"
        style="width: 300px"
        clearable
        prefix-icon="Search"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="searchCategory" placeholder="全部学科分类" clearable style="width: 170px">
        <el-option v-for="cat in categories" :key="cat" :label="cat" :value="cat" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询书目</el-button>
      <el-button @click="handleReset">重置条件</el-button>
    </div>

    <!-- 教材书目台账表 -->
    <el-table :data="books" v-loading="loading" stripe empty-text="未检索到符合条件的教材书目">
      <el-table-column prop="isbn" label="标准书号 (ISBN)" width="185">
        <template #default="{ row }">
          <span class="mono-code">{{ row.isbn || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="教材名称" min-width="210" show-overflow-tooltip />
      <el-table-column prop="author" label="主编 / 著者" width="135" show-overflow-tooltip />
      <el-table-column prop="publisher" label="出版单位" width="180" show-overflow-tooltip />
      <el-table-column prop="category" label="学科分类" width="120" align="center">
        <template #default="{ row }">
          <el-tag type="info" effect="plain" size="small">{{ row.category || '通识教材' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="price" label="定价" width="105" align="right">
        <template #default="{ row }">
          <span class="price-text">¥{{ Number(row.price || 0).toFixed(2) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="业务操作" width="210" fixed="right" align="center">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleView(row)">详情</el-button>
          <el-button type="success" link @click="handleOrder(row)" v-if="userStore.hasRole(['TEACHER', 'STUDENT'])">
            申报征订
          </el-button>
          <el-button type="primary" link @click="handleEdit(row)" v-if="userStore.hasRole(['ADMIN', 'STAFF'])">
            编目修订
          </el-button>
          <el-button type="danger" link @click="handleDelete(row)" v-if="userStore.isAdmin">
            删除
          </el-button>
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
        @size-change="loadBooks"
        @current-change="loadBooks"
      />
    </div>
  </div>

  <!-- 教材详情对话框 -->
  <el-dialog v-model="detailDialogVisible" title="教材标准书目档案卡" width="520px">
    <div class="book-detail-sheet" v-if="detailRow">
      <div class="book-detail-row">
        <div class="book-detail-label">教材名称</div>
        <div class="book-detail-val" style="font-weight: 700;">{{ detailRow.name }}</div>
      </div>
      <div class="book-detail-row">
        <div class="book-detail-label">ISBN 书号</div>
        <div class="book-detail-val mono-code">{{ detailRow.isbn || '-' }}</div>
      </div>
      <div class="book-detail-row">
        <div class="book-detail-label">主编作者</div>
        <div class="book-detail-val">{{ detailRow.author || '-' }}</div>
      </div>
      <div class="book-detail-row">
        <div class="book-detail-label">出版单位</div>
        <div class="book-detail-val">{{ detailRow.publisher || '-' }}</div>
      </div>
      <div class="book-detail-row">
        <div class="book-detail-label">学科分类</div>
        <div class="book-detail-val">{{ detailRow.category || '-' }}</div>
      </div>
      <div class="book-detail-row">
        <div class="book-detail-label">核定单价</div>
        <div class="book-detail-val price-text">¥{{ Number(detailRow.price || 0).toFixed(2) }}</div>
      </div>
      <div class="book-detail-row">
        <div class="book-detail-label">内容提要</div>
        <div class="book-detail-val">{{ detailRow.description || '暂无教材内容提要说明' }}</div>
      </div>
    </div>
    <template #footer>
      <el-button type="primary" @click="detailDialogVisible = false">关闭档案卡</el-button>
    </template>
  </el-dialog>

  <!-- 添加/编辑教材对话框 -->
  <el-dialog v-model="dialogVisible" :title="isEdit ? '修订教材书目档案' : '新增入编教材书目'" width="580px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
      <el-form-item label="ISBN 书号" prop="isbn">
        <el-input v-model="form.isbn" placeholder="如 978-7-111-64124-7" />
      </el-form-item>
      <el-form-item label="教材名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入完整教材书名" />
      </el-form-item>
      <el-form-item label="主编作者" prop="author">
        <el-input v-model="form.author" placeholder="请输入主编或著者姓名" />
      </el-form-item>
      <el-form-item label="出版单位" prop="publisher">
        <el-input v-model="form.publisher" placeholder="如 高等教育出版社 / 清华大学出版社" />
      </el-form-item>
      <el-form-item label="教材定价" prop="price">
        <el-input-number v-model="form.price" :min="0" :precision="2" style="width: 200px;" />
      </el-form-item>
      <el-form-item label="学科分类" prop="category">
        <el-input v-model="form.category" placeholder="如 计算机类 / 数学统计 / 公共基础" />
      </el-form-item>
      <el-form-item label="初始馆藏" prop="initialQuantity" v-if="!isEdit">
        <el-input-number v-model="form.initialQuantity" :min="0" style="width: 200px;" />
      </el-form-item>
      <el-form-item label="教材简介" prop="description">
        <el-input v-model="form.description" type="textarea" :rows="3" placeholder="填写适用专业、课程性质或版次说明" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitLoading">保存书目</el-button>
    </template>
  </el-dialog>

  <!-- 申报征订对话框 -->
  <el-dialog v-model="orderDialogVisible" title="课程教材征订申报单" width="440px">
    <el-form ref="orderFormRef" :model="orderForm" :rules="orderRules" label-width="88px">
      <el-form-item label="征订教材">
        <span style="font-weight: 700; color: var(--text-color);">{{ orderForm.bookName }}</span>
      </el-form-item>
      <el-form-item label="单册定价">
        <span class="price-text">¥{{ Number(orderForm.price || 0).toFixed(2) }}</span>
      </el-form-item>
      <el-form-item label="征订册数" prop="quantity">
        <el-input-number v-model="orderForm.quantity" :min="1" :max="500" />
      </el-form-item>
      <el-form-item label="预计总额">
        <span class="price-text" style="font-size: 18px;">
          ¥{{ (orderForm.price * orderForm.quantity).toFixed(2) }}
        </span>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="orderDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmitOrder" :loading="orderLoading">提交征订申请</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../stores/user'
import { listBooks, createBook, updateBook, deleteBook, getCategories, createOrder } from '../api'

const userStore = useUserStore()

const loading = ref(false)
const books = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')
const searchCategory = ref('')
const categories = ref([])

const detailDialogVisible = ref(false)
const detailRow = ref(null)

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const submitLoading = ref(false)
const form = reactive({
  id: null,
  isbn: '',
  name: '',
  author: '',
  publisher: '',
  price: 0,
  category: '',
  description: '',
  initialQuantity: 0
})

const rules = {
  name: [{ required: true, message: '请输入书名', trigger: 'blur' }]
}

const orderDialogVisible = ref(false)
const orderFormRef = ref(null)
const orderLoading = ref(false)
const orderForm = reactive({
  bookId: null,
  bookName: '',
  price: 0,
  quantity: 1
})

const orderRules = {
  quantity: [{ required: true, message: '请输入征订册数', trigger: 'blur' }]
}

const loadBooks = async () => {
  loading.value = true
  try {
    const res = await listBooks({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: searchKeyword.value,
      category: searchCategory.value
    })
    books.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('加载教材失败', error)
  } finally {
    loading.value = false
  }
}

const loadCategories = async () => {
  try {
    const res = await getCategories()
    categories.value = res.data || []
  } catch (error) {
    console.error('加载分类失败', error)
  }
}

const handleSearch = () => {
  pageNum.value = 1
  loadBooks()
}

const handleReset = () => {
  searchKeyword.value = ''
  searchCategory.value = ''
  pageNum.value = 1
  loadBooks()
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(form, {
    id: null,
    isbn: '',
    name: '',
    author: '',
    publisher: '',
    price: 0,
    category: '',
    description: '',
    initialQuantity: 0
  })
  dialogVisible.value = true
}

const handleView = (row) => {
  detailRow.value = row
  detailDialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要从标准书目库删除《${row.name}》吗？`, '删除确认', { type: 'warning' })
    .then(async () => {
      await deleteBook(row.id)
      ElMessage.success('删除成功')
      loadBooks()
    })
    .catch(() => {})
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateBook(form.id, form)
    } else {
      await createBook(form)
    }
    ElMessage.success(isEdit.value ? '书目修订成功' : '教材入编成功')
    dialogVisible.value = false
    loadBooks()
    loadCategories()
  } catch (error) {
    console.error('保存失败', error)
  } finally {
    submitLoading.value = false
  }
}

const handleOrder = (row) => {
  orderForm.bookId = row.id
  orderForm.bookName = row.name
  orderForm.price = row.price
  orderForm.quantity = 1
  orderDialogVisible.value = true
}

const handleSubmitOrder = async () => {
  const valid = await orderFormRef.value.validate().catch(() => false)
  if (!valid) return

  orderLoading.value = true
  try {
    await createOrder({
      bookId: orderForm.bookId,
      quantity: orderForm.quantity
    })
    ElMessage.success('征订申请提交成功，请等待教务书库审核')
    orderDialogVisible.value = false
  } catch (error) {
    console.error('提交订单失败', error)
  } finally {
    orderLoading.value = false
  }
}

onMounted(() => {
  loadBooks()
  loadCategories()
})
</script>
