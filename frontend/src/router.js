import { createRouter, createWebHistory } from 'vue-router'
import Login from './views/Login.vue'
import OrgTree from './views/OrgTree.vue'
import Approval from './views/Approval.vue'
import Employee from './views/Employee.vue'
import Dashboard from './views/Dashboard.vue'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: Login, meta: { title: '登录' } },
  { path: '/dashboard', component: Dashboard, meta: { title: '数据概览' } },
  { path: '/org', component: OrgTree, meta: { title: '组织部门' } },
  { path: '/employee', component: Employee, meta: { title: '员工管理' } },
  { path: '/approval', component: Approval, meta: { title: '审批中心' } }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
