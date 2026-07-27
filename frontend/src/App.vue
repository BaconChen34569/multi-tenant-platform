<template>
  <div class="layout" v-if="route.path !== '/login'">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <div class="sidebar-logo">
        <span class="logo-mark">协</span>
        <span class="name">协同办公平台</span>
      </div>
      <nav class="sidebar-menu">
        <router-link
          v-for="m in menus"
          :key="m.path"
          :to="m.path"
          class="sidebar-item"
        >
          <span class="mi"><IconSvg :name="m.icon" /></span>
          <span class="txt">{{ m.title }}</span>
        </router-link>
      </nav>
      <div class="sidebar-footer">多租户 · 中台演示</div>
    </aside>

    <!-- 主区 -->
    <div class="layout-main">
      <header class="header">
        <div class="header-title">{{ route.meta.title || '控制台' }}</div>
        <div class="header-right">
          <select class="tenant-select" v-model="tenantId" @change="switchTenant">
            <option value="1">租户 A</option>
            <option value="2">租户 B</option>
          </select>
          <div class="user">
            <span class="avatar">U</span>
            <span>当前用户</span>
          </div>
          <button class="btn-link" @click="logout">退出</button>
        </div>
      </header>
      <main class="content">
        <router-view />
      </main>
    </div>
  </div>

  <!-- 登录页单独全屏 -->
  <router-view v-else />
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import IconSvg from './components/IconSvg.vue'

const route = useRoute()
const router = useRouter()

const tenantId = ref(localStorage.getItem('tenantId') || '1')

const menus = [
  { path: '/dashboard', title: '数据概览', icon: 'dashboard' },
  { path: '/org', title: '组织部门', icon: 'org' },
  { path: '/employee', title: '员工管理', icon: 'employee' },
  { path: '/approval', title: '审批中心', icon: 'approval' }
]

function switchTenant(e) {
  localStorage.setItem('tenantId', e.target.value)
  // 切换租户后整页刷新，让各页面按新租户重新拉取数据（直观演示隔离）
  location.reload()
}
function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('tenantId')
  router.push('/login')
}
</script>
