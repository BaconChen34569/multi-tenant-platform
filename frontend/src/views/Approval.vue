<template>
  <div>
    <h2>发起审批（责任链 + 工厂）</h2>
    <input v-model="title" placeholder="标题" />
    <input v-model.number="amount" type="number" placeholder="金额" />
    <br />
    <label><input type="checkbox" value="LEADER" v-model="nodes" />主管</label>
    <label><input type="checkbox" value="HR" v-model="nodes" />HR</label>
    <label><input type="checkbox" value="FINANCE" v-model="nodes" />财务</label>
    <br />
    <button @click="run">提交</button>
    <pre>{{ result }}</pre>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { runApproval } from '../api'

const title = ref('报销申请')
const amount = ref(1200)
const nodes = ref(['LEADER', 'HR'])
const result = ref('')

async function run() {
  const r = await runApproval({
    applicant: '张三',
    title: title.value,
    amount: amount.value,
    nodeTypes: nodes.value
  })
  result.value = r.data.data
}
</script>
