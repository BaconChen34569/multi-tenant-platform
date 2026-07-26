<template>
  <div class="login">
    <h2>登录</h2>
    <input v-model="username" placeholder="用户名" />
    <br />
    <input v-model="password" type="password" placeholder="密码" />
    <br />
    <button @click="doLogin">登录</button>
    <p>提示：alice/123 属租户A，bob/123 属租户B。登录后在右上角切换租户。</p>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../api'

const router = useRouter()
const username = ref('')
const password = ref('')

async function doLogin() {
  await login(username.value, password.value)
  localStorage.setItem('token', 'demo')
  localStorage.setItem('tenantId', '1')
  router.push('/org')
}
</script>
