<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">员工管理</div>
        <div class="page-desc">按当前租户隔离查询（Redis 缓存提速），支持员工检索与维护</div>
      </div>
    </div>

    <div class="toolbar">
      <div class="toolbar-left">
        <input class="input" v-model="keyword" placeholder="搜索姓名" @keyup.enter="search" />
        <button class="btn btn-default" @click="search">查询</button>
        <button class="btn btn-default" @click="reset">重置</button>
      </div>
      <button class="btn btn-primary" @click="openAdd">新增员工</button>
    </div>

    <div class="table-wrap">
      <table class="mt-table">
        <thead>
          <tr><th>ID</th><th>姓名</th><th>部门ID</th><th>职位</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-for="e in list" :key="e.id">
            <td>{{ e.id }}</td>
            <td>{{ e.name }}</td>
            <td>{{ e.deptId }}</td>
            <td>{{ e.title }}</td>
            <td>
              <button class="btn btn-link btn-sm" @click="openEdit(e)">编辑</button>
              <button class="btn btn-link btn-sm" style="color: var(--color-danger)" @click="remove(e.id)">删除</button>
            </td>
          </tr>
          <tr v-if="!list.length"><td class="empty" colspan="5">暂无数据</td></tr>
        </tbody>
      </table>
    </div>

    <div class="pager" v-if="total > 0">
      <button class="btn btn-default btn-sm" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
      <span class="pager-info">第 {{ page }} / {{ totalPages }} 页 · 共 {{ total }} 条</span>
      <button class="btn btn-default btn-sm" :disabled="page >= totalPages" @click="changePage(page + 1)">下一页</button>
      <select class="input pager-size" v-model.number="pageSize" @change="search">
        <option :value="10">10 条/页</option>
        <option :value="20">20 条/页</option>
        <option :value="50">50 条/页</option>
      </select>
    </div>

    <div class="modal-mask" v-if="showModal" @click.self="showModal = false">
      <div class="modal">
        <div class="modal-header">{{ form.id ? '编辑员工' : '新增员工' }}</div>
        <div class="modal-body">
          <div class="form-row">
            <label>姓名</label>
            <input class="input" v-model="form.name" placeholder="姓名" />
          </div>
          <div class="form-row">
            <label>部门 ID</label>
            <input class="input" v-model.number="form.deptId" type="number" placeholder="部门 ID" />
          </div>
          <div class="form-row">
            <label>职位</label>
            <input class="input" v-model="form.title" placeholder="职位" />
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-default" @click="showModal = false">取消</button>
          <button class="btn btn-primary" @click="save">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getEmployees, addEmployee, updateEmployee, deleteEmployee } from '../api'

const list = ref([])
const keyword = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))
const showModal = ref(false)
const form = ref({ id: null, name: '', deptId: null, title: '' })

async function load() {
  const r = await getEmployees(keyword.value, page.value, pageSize.value)
  const d = r.data.data
  list.value = d.records || []
  total.value = d.total || 0
}
// 搜索时回到第一页
function search() {
  page.value = 1
  load()
}
// 重置：清空关键字并回到第一页
function reset() {
  keyword.value = ''
  page.value = 1
  load()
}
function openAdd() {
  form.value = { id: null, name: '', deptId: null, title: '' }
  showModal.value = true
}
function openEdit(e) {
  form.value = { ...e }
  showModal.value = true
}
async function save() {
  if (form.value.id) {
    await updateEmployee(form.value)
  } else {
    await addEmployee(form.value)
  }
  showModal.value = false
  await load() // 写操作后缓存已失效，重新加载当前页
}
async function remove(id) {
  if (!confirm('确认删除？')) return
  await deleteEmployee(id)
  await load()
}
function changePage(p) {
  if (p < 1 || p > totalPages.value) return
  page.value = p
  load()
}
onMounted(load)
</script>
