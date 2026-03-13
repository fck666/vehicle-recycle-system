<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { AdminUser } from '../api/adminUsers'
import { createUser, resetUserPassword, revokeUserSessions, searchUsers, updateUser, updateUserRoles } from '../api/adminUsers'
import { HttpError } from '../api/client'
import type { Page } from '../api/types'

const loading = ref(false)
const q = ref('')
const page = ref(0)
const size = ref(20)
const result = ref<Page<AdminUser>>({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 })

const roleOptions = ['ADMIN', 'OPERATOR', 'USER', 'SERVICE']
const statusOptions = ['ACTIVE', 'INACTIVE']

const createDialog = ref(false)
const editDialog = ref(false)

const createForm = reactive<{ username: string; password: string; phone: string; status: string; roles: string[] }>({
  username: '',
  password: '',
  phone: '',
  status: 'ACTIVE',
  roles: ['USER'],
})

const editForm = reactive<{ id: number; username: string; phone: string; status: string; roles: string[] }>({
  id: 0,
  username: '',
  phone: '',
  status: 'ACTIVE',
  roles: [],
})

const selectedUser = computed(() => result.value.content.find((u) => u.id === editForm.id) ?? null)

async function load() {
  loading.value = true
  try {
    result.value = await searchUsers(q.value, page.value, size.value)
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  createForm.username = ''
  createForm.password = ''
  createForm.phone = ''
  createForm.status = 'ACTIVE'
  createForm.roles = ['USER']
  createDialog.value = true
}

async function submitCreate() {
  if (!createForm.username.trim() || !createForm.password.trim()) {
    ElMessage.warning('用户名和密码必填')
    return
  }
  try {
    await createUser({
      username: createForm.username.trim(),
      password: createForm.password,
      phone: createForm.phone.trim() ? createForm.phone.trim() : null,
      status: createForm.status,
      roles: createForm.roles,
    })
    ElMessage.success('创建成功')
    createDialog.value = false
    load()
  } catch {
    ElMessage.error('创建失败（可能用户名/手机号已存在）')
  }
}

function openEdit(u: AdminUser) {
  editForm.id = u.id
  editForm.username = u.username ?? ''
  editForm.phone = u.phone ?? ''
  editForm.status = u.status ?? 'ACTIVE'
  editForm.roles = Array.isArray(u.roles) && u.roles.length ? [...u.roles] : ['USER']
  editDialog.value = true
}

async function submitEdit() {
  try {
    await updateUser(editForm.id, {
      username: editForm.username.trim() ? editForm.username.trim() : null,
      phone: editForm.phone.trim() ? editForm.phone.trim() : null,
      status: editForm.status,
    })
    await updateUserRoles(editForm.id, editForm.roles)
    ElMessage.success('保存成功')
    editDialog.value = false
    load()
  } catch (e) {
    ElMessage.error(resolveEditError(e))
  }
}

function resolveEditError(e: unknown): string {
  if (!(e instanceof HttpError)) {
    return '保存失败，请稍后重试'
  }
  if (e.status === 403) {
    return '保存失败：当前会话无权限，或正在修改自己的管理员角色/状态'
  }
  if (e.status === 409) {
    return '保存失败：不允许移除系统最后一个管理员'
  }
  if (e.status === 400) {
    return '保存失败：用户名或手机号可能已被占用'
  }
  return `保存失败：HTTP ${e.status}`
}

async function doResetPassword(u: AdminUser) {
  const r = (await ElMessageBox.prompt('输入新密码', '重置密码', { inputType: 'password' })) as any
  const pw = String(r?.value ?? '').trim()
  if (!pw) return
  try {
    await resetUserPassword(u.id, pw)
    ElMessage.success('已重置密码并踢下线')
  } catch {
    ElMessage.error('重置失败')
  }
}

async function doRevokeSessions(u: AdminUser) {
  try {
    await ElMessageBox.confirm('将踢下线该用户的 PC/小程序/脚本会话，是否继续？', '强制下线')
    await revokeUserSessions(u.id)
    ElMessage.success('已强制下线')
  } catch {
  }
}

function onSizeChange(v: number) {
  size.value = v
  page.value = 0
  load()
}

function onCurrentChange(v: number) {
  page.value = v - 1
  load()
}

load()
</script>

<template>
  <el-card>
    <template #header>
      <div style="display:flex;gap:8px;align-items:center;justify-content:space-between;">
        <div>用户管理</div>
        <div style="display:flex;gap:8px;align-items:center;">
          <el-input v-model="q" placeholder="用户名/手机号/openid/unionid" clearable style="width:280px" @keyup.enter="load" />
          <el-button :loading="loading" @click="load">搜索</el-button>
          <el-button type="primary" @click="openCreate">新增用户</el-button>
        </div>
      </div>
    </template>

    <el-table :data="result.content" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="username" label="用户名" width="160" />
      <el-table-column prop="phone" label="手机号" width="150" />
      <el-table-column prop="status" label="状态" width="110" />
      <el-table-column label="角色" min-width="180">
        <template #default="{ row }">
          <el-tag v-for="r in row.roles" :key="r" style="margin-right:6px;">{{ r }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="wxOpenid" label="OpenId" min-width="200" />
      <el-table-column prop="wxUnionid" label="UnionId" min-width="200" />
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="warning" @click="doResetPassword(row)">重置密码</el-button>
          <el-button size="small" type="danger" @click="doRevokeSessions(row)">强制下线</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="display:flex;justify-content:flex-end;margin-top:12px;">
      <el-pagination
        :current-page="page + 1"
        :page-size="size"
        :page-sizes="[10,20,50,100,200]"
        layout="total, sizes, prev, pager, next"
        :total="result.totalElements"
        @size-change="onSizeChange"
        @current-change="onCurrentChange"
      />
    </div>
  </el-card>

  <el-dialog v-model="createDialog" title="新增用户" width="520px">
    <el-form label-width="110px">
      <el-form-item label="用户名" required>
        <el-input v-model="createForm.username" />
      </el-form-item>
      <el-form-item label="密码" required>
        <el-input v-model="createForm.password" type="password" show-password />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="createForm.phone" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="createForm.status" style="width:100%;">
          <el-option v-for="s in statusOptions" :key="s" :label="s" :value="s" />
        </el-select>
      </el-form-item>
      <el-form-item label="角色">
        <el-select v-model="createForm.roles" multiple style="width:100%;">
          <el-option v-for="r in roleOptions" :key="r" :label="r" :value="r" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createDialog = false">取消</el-button>
      <el-button type="primary" @click="submitCreate">创建</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="editDialog" title="编辑用户" width="560px">
    <el-form label-width="110px">
      <el-form-item label="ID">
        <el-input :model-value="String(editForm.id)" disabled />
      </el-form-item>
      <el-form-item label="用户名">
        <el-input v-model="editForm.username" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="editForm.phone" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="editForm.status" style="width:100%;">
          <el-option v-for="s in statusOptions" :key="s" :label="s" :value="s" />
        </el-select>
      </el-form-item>
      <el-form-item label="角色">
        <el-select v-model="editForm.roles" multiple style="width:100%;">
          <el-option v-for="r in roleOptions" :key="r" :label="r" :value="r" />
        </el-select>
      </el-form-item>
      <el-form-item label="提示">
        <div style="color:var(--el-text-color-secondary);font-size:12px;line-height:18px;">
          <div>1) 不允许把自己的账号降权为非管理员</div>
          <div>2) 不允许移除系统中最后一个管理员</div>
        </div>
      </el-form-item>
      <el-form-item v-if="selectedUser?.wxOpenid || selectedUser?.wxUnionid" label="微信标识">
        <div style="color:var(--el-text-color-secondary);font-size:12px;line-height:18px;">
          <div v-if="selectedUser?.wxOpenid">OpenId：{{ selectedUser.wxOpenid }}</div>
          <div v-if="selectedUser?.wxUnionid">UnionId：{{ selectedUser.wxUnionid }}</div>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="editDialog = false">取消</el-button>
      <el-button type="primary" @click="submitEdit">保存</el-button>
    </template>
  </el-dialog>
</template>
