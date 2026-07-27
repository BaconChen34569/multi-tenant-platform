<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">组织部门</div>
        <div class="page-desc">当前租户下的部门列表（Redis 缓存），切换顶部租户后自动刷新</div>
      </div>
      <div class="page-actions">
        <input class="input" v-model="keyword" placeholder="搜索部门名" @keyup.enter="doSearch" />
        <button class="btn btn-primary btn-sm" @click="doSearch">搜索</button>
        <button class="btn btn-default btn-sm" @click="resetSearch">重置</button>
        <button class="btn btn-default" @click="load">刷新</button>
      </div>
    </div>

    <div class="card">
      <ul class="org-list">
        <li v-for="d in list" :key="d.id">
          <span class="org-dot"></span>
          <span>{{ d.name }}</span>
          <span class="org-id">ID: {{ d.id }}</span>
        </li>
      </ul>
      <div class="empty" v-if="!list.length">暂无部门数据</div>

      <div class="pager" v-if="total > pageSize">
        <button class="btn btn-default btn-sm" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
        <span class="pager-info">第 {{ page }} / {{ totalPages }} 页 · 共 {{ total }} 条</span>
        <button class="btn btn-default btn-sm" :disabled="page >= totalPages" @click="changePage(page + 1)">下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getOrgPage } from '../api'

const list = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const keyword = ref('')
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

async function load() {
  const r = await getOrgPage(page.value, pageSize.value, keyword.value)
  const d = r.data.data
  list.value = d.records || []
  total.value = d.total || 0
}
function changePage(p) {
  if (p < 1 || p > totalPages.value) return
  page.value = p
  load()
}
function doSearch() {
  page.value = 1
  load()
}
function resetSearch() {
  keyword.value = ''
  page.value = 1
  load()
}
onMounted(load)
</script>
