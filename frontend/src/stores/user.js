import { defineStore } from 'pinia'
import { getCurrentUser } from '../api'

export const useUserStore = defineStore('user', {
  state: () => ({
    user: JSON.parse(localStorage.getItem('user') || 'null'),
    token: localStorage.getItem('token') || ''
  }),
  
  getters: {
    isLoggedIn: (state) => !!state.token,
    userRole: (state) => state.user?.role || '',
    isAdmin: (state) => state.user?.role === 'ADMIN',
    isStaff: (state) => state.user?.role === 'STAFF',
    isPurchaser: (state) => state.user?.role === 'PURCHASER',
    isTeacher: (state) => state.user?.role === 'TEACHER',
    isStudent: (state) => state.user?.role === 'STUDENT'
  },
  
  actions: {
    setLogin(token, user) {
      this.token = token
      this.user = user
      localStorage.setItem('token', token)
      localStorage.setItem('user', JSON.stringify(user))
    },
    
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    },
    
    async fetchUser() {
      try {
        const res = await getCurrentUser()
        this.user = res.data
        localStorage.setItem('user', JSON.stringify(res.data))
      } catch (error) {
        console.error('获取用户信息失败', error)
      }
    },
    
    hasRole(roles) {
      if (!this.user) return false
      if (Array.isArray(roles)) {
        return roles.includes(this.user.role)
      }
      return this.user.role === roles
    }
  }
})
