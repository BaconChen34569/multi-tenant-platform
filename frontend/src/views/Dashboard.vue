<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">数据概览</div>
        <div class="page-desc">当前租户视角下的平台核心指标，切换顶部租户后自动刷新 —— 多租户隔离的直观体现</div>
      </div>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div>
          <div class="num">{{ stats.deptCount }}</div>
          <div class="label">部门数</div>
        </div>
        <div class="icon blue"><IconSvg name="org" /></div>
      </div>
      <div class="stat-card">
        <div>
          <div class="num">{{ stats.employeeCount }}</div>
          <div class="label">员工数</div>
        </div>
        <div class="icon blue"><IconSvg name="users" /></div>
      </div>
      <div class="stat-card">
        <div>
          <div class="num">{{ stats.pendingCount }}</div>
          <div class="label">待审批</div>
        </div>
        <div class="icon orange"><IconSvg name="clock" /></div>
      </div>
      <div class="stat-card">
        <div>
          <div class="num">{{ stats.approvedCount }}</div>
          <div class="label">已通过</div>
        </div>
        <div class="icon green"><IconSvg name="check" /></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getDashboardStats } from '../api'
import IconSvg from '../components/IconSvg.vue'

const stats = ref({ deptCount: 0, employeeCount: 0, pendingCount: 0, approvedCount: 0 })

onMounted(async () => {
  const r = await getDashboardStats()
  stats.value = r.data.data
})
</script>
