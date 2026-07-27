<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-brand">
        <span class="logo-mark">协</span>
        <span class="name">协同办公平台</span>
      </div>
      <h2 class="login-title">欢迎登录</h2>
      <p class="login-sub">多租户协同办公 · 中台演示系统</p>

      <div class="form-row">
        <label>用户名</label>
        <input class="input" v-model="username" placeholder="请输入用户名" />
      </div>
      <div class="form-row">
        <label>密码</label>
        <input class="input" type="password" v-model="password" placeholder="请输入密码" />
      </div>
      <button class="btn btn-primary btn-block" @click="doLogin">登 录</button>

      <div class="login-tip">
        演示账号：alice / 123（租户 A）　·　bob / 123（租户 B）<br />
        登录后可在顶部切换租户，直观演示数据隔离
      </div>
    </div>
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
  router.push('/dashboard')
}
</script>
