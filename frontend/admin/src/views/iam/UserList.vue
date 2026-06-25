<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { iamApi, type RoleRecord, type UserRecord, type UserRequest } from '../../api/iam'

const users = ref<UserRecord[]>([])
const roles = ref<RoleRecord[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive<UserRequest>({
  username: '',
  displayName: '',
  reportToUserId: undefined,
  wecomUserId: '',
  departmentName: '',
  roleCodes: [],
  enabled: true,
})

const dialogTitle = computed(() => (editingId.value ? '编辑用户' : '新建用户'))
const supervisorOptions = computed(() => users.value.filter((user) => user.id !== editingId.value))

async function load() {
  loading.value = true
  try {
    users.value = await iamApi.users()
    roles.value = await iamApi.roles()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { username: '', displayName: '', reportToUserId: undefined, wecomUserId: '', departmentName: '', roleCodes: [], enabled: true })
  dialogVisible.value = true
}

function openEdit(user: UserRecord) {
  editingId.value = user.id
  Object.assign(form, {
    username: user.username,
    displayName: user.displayName,
    reportToUserId: user.reportToUserId,
    wecomUserId: user.wecomUserId ?? '',
    departmentName: user.departmentName ?? '',
    roleCodes: [...user.roleCodes],
    enabled: user.enabled,
  })
  dialogVisible.value = true
}

async function submit() {
  if (!form.username || !form.displayName) {
    ElMessage.warning('请填写登录名和姓名')
    return
  }
  if (editingId.value) {
    await iamApi.updateUser(editingId.value, form)
    ElMessage.success('用户已更新')
  } else {
    await iamApi.createUser(form)
    ElMessage.success('用户已创建，初始密码 qwer1234')
  }
  dialogVisible.value = false
  await load()
}

async function resetPassword(user: UserRecord) {
  await iamApi.resetPassword(user.id)
  ElMessage.success(`已重置 ${user.username} 密码为 qwer1234`)
}

async function toggleStatus(user: UserRecord) {
  await iamApi.updateUserStatus(user.id, !user.enabled)
  ElMessage.success(user.enabled ? '用户已停用' : '用户已启用')
  await load()
}

function supervisorName(id?: number) {
  if (!id) return '-'
  const user = users.value.find((item) => item.id === id)
  return user ? `${user.displayName}（${user.username}）` : String(id)
}

onMounted(load)
</script>

<template>
  <section class="page-section">
    <header class="section-head">
      <h1>用户管理</h1>
      <el-button type="primary" @click="openCreate">新建用户</el-button>
    </header>
    <el-table :data="users" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="登录名" width="120" />
      <el-table-column prop="displayName" label="姓名" width="120" />
      <el-table-column prop="departmentName" label="部门" />
      <el-table-column label="上级用户" width="160">
        <template #default="scope">{{ supervisorName(scope.row.reportToUserId) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.enabled ? 'success' : 'info'">{{ scope.row.enabled ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="角色">
        <template #default="scope">{{ scope.row.roleCodes?.join(', ') }}</template>
      </el-table-column>
      <el-table-column label="操作" width="260">
        <template #default="scope">
          <el-button link type="primary" @click="openEdit(scope.row)">编辑</el-button>
          <el-button link @click="resetPassword(scope.row)">重置密码</el-button>
          <el-button link @click="toggleStatus(scope.row)">{{ scope.row.enabled ? '停用' : '启用' }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px">
      <el-form label-width="100px">
        <el-form-item label="登录名"><el-input v-model="form.username" :disabled="!!editingId" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.displayName" /></el-form-item>
        <el-form-item label="部门"><el-input v-model="form.departmentName" /></el-form-item>
        <el-form-item label="上级用户">
          <el-select v-model="form.reportToUserId" class="form-control" filterable clearable placeholder="请选择上级用户">
            <el-option v-for="user in supervisorOptions" :key="user.id" :label="`${user.displayName}（${user.username}）`" :value="user.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="企业微信 ID"><el-input v-model="form.wecomUserId" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleCodes" multiple class="form-control">
            <el-option v-for="role in roles" :key="role.code" :label="`${role.name} (${role.code})`" :value="role.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.enabled" /></el-form-item>
        <el-alert v-if="!editingId" type="info" show-icon :closable="false" title="新建用户默认密码为 qwer1234，系统将保存密码哈希。" />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>
