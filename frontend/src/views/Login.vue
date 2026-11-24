<template>
  <div class="login-container">
    <div class="login-box">
      <h2 class="login-title">高校教材订购管理系统</h2>

      <el-form ref="formRef" :model="form" :rules="rules" size="large">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名（admin / teacher1 / purchaser1）"
            prefix-icon="User"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item style="margin-bottom: 0;">
          <el-button
            type="primary"
            style="width: 100%;"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="quick-demo-panel">
        <div class="quick-demo-title">演示账号一键登录（免输密码）</div>
        <div class="quick-demo-grid">
          <el-button size="small" @click="quickLogin('admin')">管理员</el-button>
          <el-button size="small" @click="quickLogin('teacher1')">任课教师</el-button>
          <el-button size="small" @click="quickLogin('purchaser1')">采购员</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../api'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const doLogin = async () => {
  loading.value = true
  try {
    const res = await login(form)
    userStore.setLogin(res.data.token, res.data.user)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (error) {
    console.error('登录失败', error)
  } finally {
    loading.value = false
  }
}

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  await doLogin()
}

const quickLogin = async (username) => {
  form.username = username
  form.password = 'admin123'
  await doLogin()
}
</script>
