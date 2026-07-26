import { createRouter, createWebHistory } from 'vue-router'
import Login from './views/Login.vue'
import OrgTree from './views/OrgTree.vue'
import Approval from './views/Approval.vue'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: Login },
  { path: '/org', component: OrgTree },
  { path: '/approval', component: Approval }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
