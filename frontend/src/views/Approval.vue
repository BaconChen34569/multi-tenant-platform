<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">审批中心</div>
        <div class="page-desc">责任链编排演示，支持发起、待办处理、责任链轨迹留存与历史查询</div>
      </div>
    </div>

    <div class="segmented mb-16">
      <button :class="{ active: tab === 'run' }" @click="tab = 'run'">发起审批</button>
      <button :class="{ active: tab === 'todos' }" @click="switchTab('todos')">待办</button>
      <button :class="{ active: tab === 'history' }" @click="switchTab('history')">历史</button>
    </div>

    <!-- 发起审批：责任链演示 -->
    <div class="card" v-if="tab === 'run'">
      <div class="form-inline mb-16">
        <input class="input" v-model="title" placeholder="标题" />
        <input class="input" v-model.number="amount" type="number" placeholder="金额" />
      </div>
      <div class="row mb-16">
        <label class="row"><input type="checkbox" value="LEADER" v-model="nodes" /> 主管</label>
        <label class="row"><input type="checkbox" value="HR" v-model="nodes" /> 人事</label>
        <label class="row"><input type="checkbox" value="FINANCE" v-model="nodes" /> 财务</label>
      </div>
      <div class="row">
        <button class="btn btn-default" @click="run">责任链执行</button>
        <button class="btn btn-primary" @click="submit">提交为待办</button>
      </div>
      <pre v-if="result" class="result">{{ result }}</pre>
    </div>

    <!-- 待办 -->
    <div class="table-wrap" v-else-if="tab === 'todos'">
      <table class="mt-table">
        <thead>
          <tr><th>ID</th><th>标题</th><th>申请人</th><th>金额</th><th>节点</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-for="t in todos" :key="t.id">
            <td>{{ t.id }}</td>
            <td>{{ t.title }}</td>
            <td>{{ t.applicant }}</td>
            <td class="amount">{{ fmtMoney(t.amount) }}</td>
            <td>{{ nodeText(t.nodeTypes) }}</td>
            <td>
              <button class="btn btn-link btn-sm" @click="decide(t.id, 'APPROVED')">通过</button>
              <button class="btn btn-link btn-sm" style="color: var(--color-danger)" @click="decide(t.id, 'REJECTED')">驳回</button>
            </td>
          </tr>
          <tr v-if="!todos.length"><td class="empty" colspan="5">暂无待办</td></tr>
        </tbody>
      </table>
    </div>

    <!-- 历史 -->
    <div class="card" v-else>
      <div class="form-inline mb-16">
        <span class="text-muted">状态筛选：</span>
        <select class="input" v-model="histStatus" @change="loadHistory">
          <option value="">全部</option>
          <option value="APPROVED">已通过</option>
          <option value="REJECTED">已驳回</option>
          <option value="PENDING">待审批</option>
        </select>
      </div>
      <div class="table-wrap">
        <table class="mt-table">
        <thead>
          <tr><th>ID</th><th>标题</th><th>申请人</th><th>金额</th><th>状态</th><th>节点</th><th>责任链轨迹</th></tr>
        </thead>
        <tbody>
          <tr v-for="t in history" :key="t.id">
            <td>{{ t.id }}</td>
            <td>{{ t.title }}</td>
            <td>{{ t.applicant }}</td>
            <td class="amount">{{ fmtMoney(t.amount) }}</td>
            <td><span class="tag" :class="statusClass(t.status)">{{ statusText(t.status) }}</span></td>
            <td>{{ nodeText(t.nodeTypes) }}</td>
            <td class="trace">{{ t.executionLog || '—' }}</td>
          </tr>
            <tr v-if="!history.length"><td class="empty" colspan="5">暂无记录</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { runApproval, submitApproval, getTodos, getHistory, decideApproval } from '../api'

const tab = ref('run')
const title = ref('报销申请')
const amount = ref(1200)
const nodes = ref(['LEADER', 'HR'])
const result = ref('')

const todos = ref([])
const history = ref([])
const histStatus = ref('')

async function run() {
  const r = await runApproval({
    applicant: '张三', title: title.value, amount: amount.value, nodeTypes: nodes.value
  })
  result.value = r.data.data
}
async function submit() {
  const r = await submitApproval({
    applicant: '张三', title: title.value, amount: amount.value, nodeTypes: nodes.value
  })
  result.value = '已提交，任务 ID：' + r.data.data
}
async function switchTab(t) {
  tab.value = t
  if (t === 'todos') await loadTodos()
  if (t === 'history') await loadHistory()
}
async function loadTodos() {
  const r = await getTodos()
  todos.value = r.data.data
}
async function loadHistory() {
  const r = await getHistory(histStatus.value)
  history.value = r.data.data
}
async function decide(id, decision) {
  if (!confirm('确认' + (decision === 'APPROVED' ? '通过' : '驳回') + '？')) return
  await decideApproval(id, decision)
  await loadTodos()
}
function statusText(s) {
  return s === 'APPROVED' ? '已通过' : s === 'REJECTED' ? '已驳回' : '待审批'
}
function statusClass(s) {
  return s === 'APPROVED' ? 'tag-success' : s === 'REJECTED' ? 'tag-danger' : 'tag-info'
}
const NODE_LABELS = { LEADER: '主管', HR: '人事', FINANCE: '财务' }
function nodeText(s) {
  if (!s) return ''
  return s.split(',').map(k => NODE_LABELS[k.trim()] || k).join('、')
}
function fmtMoney(v) {
  return v == null ? '—' : '¥' + Number(v).toFixed(2)
}
</script>

<style scoped>
.result {
  background: var(--color-fill-1);
  padding: 12px;
  border-radius: 6px;
  font-size: 13px;
  white-space: pre-wrap;
  margin: 16px 0 0;
  color: var(--color-text-2);
}
.trace {
  font-size: 12px;
  line-height: 1.5;
  color: var(--color-text-3);
  white-space: pre-wrap;
  max-width: 280px;
}
.amount {
  text-align: center;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
</style>
