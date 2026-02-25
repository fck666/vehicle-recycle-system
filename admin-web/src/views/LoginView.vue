<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { login } from '../api/auth'
import { useAuthStore } from '../stores/auth'
import { useRouter } from 'vue-router'

const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const form = reactive<{ username: string; password: string }>({ username: 'fcc', password: '12345' })

async function submit() {
  loading.value = true
  try {
    const res = await login({ username: form.username, password: form.password })
    auth.setToken(res.token)
    await auth.loadMe()
    ElMessage.success('登录成功')
    router.replace('/')
  } catch {
    ElMessage.error('登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="wrap">
    <el-card class="card">
      <template #header>
        <div class="title">管理后台登录</div>
      </template>
      <el-form label-width="90px" @submit.prevent>
        <el-form-item label="用户名" required>
          <el-input v-model="form.username" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input v-model="form.password" type="password" show-password autocomplete="current-password" @keyup.enter="submit" />
        </el-form-item>
        <el-button type="primary" :loading="loading" style="width:100%;" @click="submit">登录</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.wrap {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-bg-color-page);
}
.card {
  width: 420px;
}
.title {
  font-weight: 600;
}
</style>
