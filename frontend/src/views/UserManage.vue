<template>
  <div class="page-card">
    <div class="page-header">
      <div class="page-title-group">
        <h2 class="page-title">教务角色与系统用户管理</h2>
        <span class="page-subtitle">维护教务管理员、任课教师、班级学生代表、书库管理员及采购专员账号与岗位授权</span>
      </div>
      <el-button type="primary" @click="handleAdd">
        <el-icon style="margin-right: 4px;"><Plus /></el-icon>新增系统账号
      </el-button>
    </div>

    <!-- 检索栏 -->
    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="检索工号学号 / 姓名 / 联系电话"
        style="width: 280px"
        clearable
        prefix-icon="Search"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="searchRole" placeholder="全部岗位角色" clearable style="width: 170px">
        <el-option label="教务管理员" value="ADMIN" />
        <el-option label="任课教师" value="TEACHER" />
        <el-option label="学生代表" value="STUDENT" />
        <el-option label="书库管理员" value="STAFF" />
        <el-option label="采购专员" value="PURCHASER" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询用户</el-button>
      <el-button @click="handleReset">重置条件</el-button>
    </div>

    <!-- 用户列表 -->
    <el-table :data="users" v-loading="loading" stripe empty-text="未检索到匹配的系统用户记录">
      <el-table-column prop="username" label="工号 / 账号" width="150">
        <template #default="{ row }">
          <span class="mono-code">{{ row.username }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="realName" label="真实姓名" width="125" />
      <el-table-column prop="role" label="岗位角色" width="130" align="center">
        <template #default="{ row }">
          <el-tag :type="getRoleType(row.role)" size="small">{{ getRoleText(row.role) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="联系电话" width="150" />
      <el-table-column prop="email" label="电子邮箱" min-width="180" show-overflow-tooltip />
      <el-table-column prop="status" label="启用状态" width="105" align="center">
        <template #default="{ row }">
          <el-switch
            v-model="row.status"
            :active-value="1"
            :inactive-value="0"
            @change="handleStatusChange(row)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" width="175" />
      <el-table-column label="账户操作" width="150" fixed="right" align="center">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleEdit(row)">编辑资料</el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
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
        @size-change="loadUsers"
        @current-change="loadUsers"
      />
    </div>
  </div>

  <!-- 添加/编辑用户对话框 -->
  <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑教务系统用户信息' : '新增教务系统用户'" width="500px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
      <el-form-item label="工号 / 账号" prop="username">
        <el-input v-model="form.username" placeholder="请输入登录工号、学号或用户名" :disabled="isEdit" />
      </el-form-item>
      <el-form-item label="登录密码" :prop="isEdit ? '' : 'password'">
        <el-input
          v-model="form.password"
          type="password"
          :placeholder="isEdit ? '留空则保持原密码不变' : '请输入初始登录密码'"
          show-password
        />
      </el-form-item>
      <el-form-item label="真实姓名" prop="realName">
        <el-input v-model="form.realName" placeholder="请输入教职工或学生姓名" />
      </el-form-item>
      <el-form-item label="岗位角色" prop="role">
        <el-select v-model="form.role" placeholder="请选择对应的业务角色" style="width: 100%">
          <el-option label="教务管理员" value="ADMIN" />
          <el-option label="任课教师" value="TEACHER" />
          <el-option label="学生代表" value="STUDENT" />
          <el-option label="书库管理员" value="STAFF" />
          <el-option label="采购专员" value="PURCHASER" />
        </el-select>
      </el-form-item>
      <el-form-item label="联系电话" prop="phone">
        <el-input v-model="form.phone" placeholder="请输入手机或办公电话" />
      </el-form-item>
      <el-form-item label="电子邮箱" prop="email">
        <el-input v-model="form.email" placeholder="请输入院系办公邮箱" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitLoading">保存账户</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listUsers, createUser, updateUser, deleteUser, updateUserStatus } from '../api'

const loading = ref(false)
const users = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')
const searchRole = ref('')

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const submitLoading = ref(false)
const form = reactive({
  id: null,
  username: '',
  password: '',
  realName: '',
  role: '',
  phone: '',
  email: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const getRoleType = (role) => {
  const types = { ADMIN: 'danger', TEACHER: 'primary', STUDENT: 'success', STAFF: 'warning', PURCHASER: 'info' }
  return types[role] || 'info'
}

const getRoleText = (role) => {
  const texts = { ADMIN: '教务管理员', TEACHER: '任课教师', STUDENT: '学生代表', STAFF: '书库管理员', PURCHASER: '采购专员' }
  return texts[role] || role
}

const loadUsers = async () => {
  loading.value = true
  try {
    const res = await listUsers({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: searchKeyword.value,
      role: searchRole.value
    })
    users.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('加载用户失败', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  loadUsers()
}

const handleReset = () => {
  searchKeyword.value = ''
  searchRole.value = ''
  pageNum.value = 1
  loadUsers()
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(form, { id: null, username: '', password: '', realName: '', role: '', phone: '', email: '' })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, { ...row, password: '' })
  dialogVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除用户“${row.realName || row.username}”吗？`, '删除确认', { type: 'warning' })
    .then(async () => {
      await deleteUser(row.id)
      ElMessage.success('删除成功')
      loadUsers()
    })
    .catch(() => {})
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateUser(form.id, form)
    } else {
      await createUser(form)
    }
    ElMessage.success(isEdit.value ? '账户信息更新成功' : '新用户创建成功')
    dialogVisible.value = false
    loadUsers()
  } catch (error) {
    console.error('保存失败', error)
  } finally {
    submitLoading.value = false
  }
}

const handleStatusChange = async (row) => {
  try {
    await updateUserStatus(row.id, row.status)
    ElMessage.success('账户状态更新成功')
  } catch (error) {
    row.status = row.status === 1 ? 0 : 1
    console.error('更新状态失败', error)
  }
}

onMounted(() => {
  loadUsers()
})
</script>
