<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const isLoginPage = computed(() => route.path === '/login')
const isAdmin = computed(() => (auth.me?.roles ?? []).includes('ADMIN'))

function onSelect(key: string) {
  router.push(key)
}

async function logout() {
  await auth.logout()
  router.push('/login')
}
</script>

<template>
  <router-view v-if="isLoginPage" />
  <el-container v-else class="app">
    <el-aside width="220px" class="aside">
      <div class="brand">管理后台</div>
      <el-menu :default-active="(route.path as string)" @select="onSelect" class="menu">
        <el-menu-item index="/">概览</el-menu-item>
        <el-menu-item index="/vehicles">车型管理</el-menu-item>
        <el-menu-item v-if="isAdmin" index="/vehicle-mappings">车型关联</el-menu-item>
        <el-menu-item index="/material-prices">材料价格</el-menu-item>
        <el-menu-item index="/material-templates">估值方式</el-menu-item>
        <el-menu-item v-if="isAdmin" index="/users">用户管理</el-menu-item>
        <el-sub-menu index="/jobs">
          <template #title>抓取任务</template>
          <el-menu-item index="/jobs/material-price">材料抓取</el-menu-item>
          <el-menu-item index="/jobs/vehicle">车型抓取</el-menu-item>
          <el-menu-item v-if="isAdmin" index="/jobs/miit-cp">工信部抓取</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="title">{{ route.meta.title ?? route.name }}</div>
        <div class="spacer" />
        <div class="user">{{ auth.me?.username }}</div>
        <el-button link type="primary" @click="logout">退出</el-button>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.app {
  min-height: 100vh;
}
.aside {
  border-right: 1px solid var(--el-border-color);
  background: var(--el-bg-color);
}
.brand {
  padding: 16px;
  font-weight: 600;
}
.menu {
  border-right: none;
}
.header {
  display: flex;
  align-items: center;
  border-bottom: 1px solid var(--el-border-color);
  background: var(--el-bg-color);
}
.title {
  font-weight: 600;
}
.spacer {
  flex: 1;
}
.user {
  margin-right: 8px;
}
.main {
  background: var(--el-bg-color-page);
  padding: 16px;
}
</style>
