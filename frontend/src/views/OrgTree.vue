<template>
  <div>
    <h2>部门树（Redis 缓存）</h2>
    <button @click="load">刷新</button>
    <ul>
      <li v-for="d in list" :key="d.id">{{ d.name }} (id={{ d.id }})</li>
    </ul>
    <p>多租户隔离：切换右上角租户后刷新，列表只显示该租户数据。</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getOrgTree } from '../api'

const list = ref([])
async function load() {
  const r = await getOrgTree()
  list.value = r.data.data
}
onMounted(load)
</script>
