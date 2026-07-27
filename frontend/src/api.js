import axios from 'axios'

const api = axios.create({ baseURL: '/api' })

// 请求拦截：自动带上当前租户头，后端据此做数据隔离
api.interceptors.request.use(config => {
  const tid = localStorage.getItem('tenantId')
  if (tid) config.headers['X-Tenant-Id'] = tid
  return config
})

export const login = (username, password) =>
  api.post('/auth/login', { username, password })

export const getOrgTree = () => api.get('/org/tree')
export const getOrgPage = (page, size, keyword) => api.get('/org/page', { params: { page, size, keyword } })

export const runApproval = (payload) => api.post('/approval/run', payload)
export const submitApproval = (payload) => api.post('/approval/submit', payload)
export const getTodos = () => api.get('/approval/todos')
export const getHistory = (status) => api.get('/approval/history', { params: { status } })
export const decideApproval = (id, decision) => api.post('/approval/decide', { id, decision })

export const getEmployees = (keyword, page, pageSize) =>
  api.get('/employee/list', { params: { keyword, page, pageSize } })
export const addEmployee = (emp) => api.post('/employee', emp)
export const updateEmployee = (emp) => api.put('/employee', emp)
export const deleteEmployee = (id) => api.delete('/employee/' + id)

export const getDashboardStats = () => api.get('/dashboard/stats')
