import request from '../utils/request'

// 认证相关
export const login = (data) => request.post('/auth/login', data)
export const register = (data) => request.post('/auth/register', data)

// 用户相关
export const getCurrentUser = () => request.get('/users/current')
export const listUsers = (params) => request.get('/users', { params })
export const createUser = (data) => request.post('/users', data)
export const updateUser = (id, data) => request.put(`/users/${id}`, data)
export const deleteUser = (id) => request.delete(`/users/${id}`)
export const changePassword = (data) => request.post('/users/change-password', data)
export const updateUserStatus = (id, status) => request.put(`/users/${id}/status`, null, { params: { status } })

// 教材相关
export const listBooks = (params) => request.get('/books', { params })
export const getBook = (id) => request.get(`/books/${id}`)
export const createBook = (data) => request.post('/books', data)
export const updateBook = (id, data) => request.put(`/books/${id}`, data)
export const deleteBook = (id) => request.delete(`/books/${id}`)
export const getCategories = () => request.get('/books/categories')

// 库存相关
export const listInventory = (params) => request.get('/inventory', { params })
export const getInventoryByBook = (bookId) => request.get(`/inventory/book/${bookId}`)
export const updateWarningQuantity = (id, warningQuantity) => 
  request.put(`/inventory/${id}/warning`, null, { params: { warningQuantity } })
export const getLowStockList = () => request.get('/inventory/low-stock')

// 订单相关
export const createOrder = (data) => request.post('/orders', data)
export const getMyOrders = (params) => request.get('/orders/my', { params })
export const listOrders = (params) => request.get('/orders', { params })
export const getOrder = (id) => request.get(`/orders/${id}`)
export const reviewOrder = (id, data) => request.post(`/orders/${id}/review`, data)
export const invoiceOrder = (id) => request.post(`/orders/${id}/invoice`)
export const preparePickup = (id) => request.post(`/orders/${id}/prepare-pickup`)
export const confirmPickup = (id) => request.post(`/orders/${id}/confirm-pickup`)
export const cancelOrder = (id) => request.post(`/orders/${id}/cancel`)

// 缺书登记相关
export const listShortages = (params) => request.get('/shortages', { params })
export const getShortage = (id) => request.get(`/shortages/${id}`)
export const createShortage = (bookId, requiredQuantity) => 
  request.post('/shortages', null, { params: { bookId, requiredQuantity } })
export const sendToPurchase = (id) => request.post(`/shortages/${id}/send-purchase`)

// 采购相关
export const listPurchases = (params) => request.get('/purchases', { params })
export const getPurchase = (id) => request.get(`/purchases/${id}`)
export const createPurchase = (data) => request.post('/purchases', data)
export const createFromShortage = (data) => request.post('/purchases/from-shortage', data)
export const startPurchase = (id) => request.post(`/purchases/${id}/start`)
export const completePurchase = (id, data) => request.post(`/purchases/${id}/complete`, data)

// 进出库相关
export const listStockIn = (params) => request.get('/stock/in', { params })
export const stockIn = (data) => request.post('/stock/in', data)
export const listStockOut = (params) => request.get('/stock/out', { params })
export const stockOut = (data) => request.post('/stock/out', data)
