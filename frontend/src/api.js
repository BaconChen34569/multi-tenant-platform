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

export const runApproval = (payload) => api.post('/approval/run', payload)
