<template>
  <div class="app">
    <header v-if="loggedIn">
      <span>当前租户：{{ tenantId || '未选择' }}</span>
      <button @click="switchTenant('1')">切到租户A</button>
      <button @click="switchTenant('2')">切到租户B</button>
      <router-link to="/org">部门树</router-link>
      <router-link to="/approval">发起审批</router-link>
    </header>
    <router-view />
  </div>
</template>

<script setup>
import { ref } from 'vue'

const tenantId = ref(localStorage.getItem('tenantId'))
const loggedIn = ref(!!localStorage.getItem('token'))

function switchTenant(t) {
  localStorage.setItem('tenantId', t)
  tenantId.value = t
}
</script>

<style>
.app { font-family: sans-serif; padding: 16px; }
header { margin-bottom: 12px; }
header button { margin: 0 4px; }
header a { margin: 0 6px; }
</style>
