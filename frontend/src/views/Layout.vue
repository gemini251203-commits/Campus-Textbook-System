<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="sidebar-logo">教材订购管理系统</div>
      <el-menu :default-active="route.path" router>
        <template v-for="item in menuItems" :key="item.path">
          <el-menu-item :index="item.path" v-if="hasAccess(item.roles)">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </aside>

    <div class="admin-main-wrap">
      <header class="admin-topbar">
        <span style="font-weight: 600; color: #606266;">{{ currentPageTitle }}</span>
        <div style="display: flex; align-items: center; gap: 12px;">
          <span style="font-size: 13px; color: #606266;">
            {{ userStore.user?.realName || userStore.user?.username }}（{{ roleText }}）
          </span>
          <el-button size="small" @click="passwordDialogVisible = true">修改密码</el-button>
          <el-button size="small" type="danger" plain @click="handleLogout">退出</el-button>
        </div>
      </header>

      <main class="admin-content">
        <router-view />
      </main>
    </div>
  </div>

  <el-dialog v-model="passwordDialogVisible" title="修改密码" width="400px">
    <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="80px">
      <el-form-item label="原密码" prop="oldPassword">
        <el-input v-model="passwordForm.oldPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="passwordForm.newPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="passwordDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleChangePassword" :loading="passwordLoading">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../stores/user'
import { changePassword } from '../api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const menuItems = [
  { path: '/dashboard', title: '系统首页', icon: 'HomeFilled', roles: null },
  { path: '/books', title: '教材管理', icon: 'Reading', roles: null },
  { path: '/orders', title: '订单管理', icon: 'ShoppingCart', roles: null },
  { path: '/inventory', title: '库存管理', icon: 'Box', roles: ['ADMIN', 'STAFF', 'PURCHASER'] },
  { path: '/shortages', title: '缺书登记', icon: 'Warning', roles: ['ADMIN', 'STAFF', 'PURCHASER'] },
  { path: '/purchases', title: '采购管理', icon: 'Goods', roles: ['ADMIN', 'PURCHASER'] },
  { path: '/stock', title: '出入库记录', icon: 'Operation', roles: ['ADMIN', 'STAFF', 'PURCHASER'] },
  { path: '/users', title: '用户管理', icon: 'User', roles: ['ADMIN'] }
]

const currentPageTitle = computed(() => {
  const matched = menuItems.find((item) => item.path === route.path)
  return matched ? matched.title : '系统首页'
})

const roleText = computed(() => {
  const roleMap = {
    ADMIN: '管理员',
    TEACHER: '教师',
    STUDENT: '学生',
    STAFF: '书库管理员',
    PURCHASER: '采购员'
  }
  return roleMap[userStore.user?.role] || '用户'
})

const hasAccess = (roles) => {
  if (!roles) return true
  return userStore.hasRole(roles)
}

const passwordDialogVisible = ref(false)
const passwordFormRef = ref(null)
const passwordLoading = ref(false)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

const handleChangePassword = async () => {
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return
  passwordLoading.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功')
    passwordDialogVisible.value = false
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch (error) {
    console.error('修改密码失败', error)
  } finally {
    passwordLoading.value = false
  }
}

const handleLogout = () => {
  ElMessageBox.confirm('确定退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    userStore.logout()
    router.push('/login')
  }).catch(() => {})
}
</script>
