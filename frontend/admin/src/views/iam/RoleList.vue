<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { iamApi, type PermissionRecord, type RoleRecord, type RoleRequest } from '../../api/iam'

const roles = ref<RoleRecord[]>([])
const permissions = ref<PermissionRecord[]>([])
const dialogVisible = ref(false)
const editingCode = ref<string | null>(null)
const form = reactive<RoleRequest>({ code: '', name: '', description: '', permissionCodes: [], enabled: true, sortOrder: 100 })

const groupedPermissions = computed(() => {
  const groups = new Map<string, PermissionRecord[]>()
  permissions.value.forEach((p) => {
    const list = groups.get(p.domain) ?? []
    list.push(p)
    groups.set(p.domain, list)
  })
  return [...groups.entries()]
})

const dialogTitle = computed(() => (editingCode.value ? '编辑角色' : '新增角色'))

async function load() {
  roles.value = await iamApi.roles()
  permissions.value = await iamApi.permissions()
}

function openCreate() {
  editingCode.value = null
  Object.assign(form, { code: '', name: '', description: '', permissionCodes: [], enabled: true, sortOrder: 100 })
  dialogVisible.value = true
}

function openEdit(role: RoleRecord) {
  editingCode.value = role.code
  Object.assign(form, {
    code: role.code,
    name: role.name,
    description: role.description,
    permissionCodes: [...role.permissionCodes],
    enabled: role.enabled,
    sortOrder: role.sortOrder,
  })
  dialogVisible.value = true
}

async function submit() {
  if (!form.code || !form.name) {
    ElMessage.warning('请填写角色编码和名称')
    return
  }
  if (editingCode.value) {
    await iamApi.updateRole(editingCode.value, form)
    ElMessage.success('角色已更新')
  } else {
    await iamApi.createRole(form)
    ElMessage.success('角色已创建')
  }
  dialogVisible.value = false
  await load()
}

async function removeRole(role: RoleRecord) {
  await ElMessageBox.confirm(`确认删除角色 ${role.name}？`, '提示', { type: 'warning' })
  await iamApi.deleteRole(role.code)
  ElMessage.success('角色已删除')
  await load()
}

onMounted(load)
</script>

<template>
  <section class="page-section">
    <header class="section-head">
      <h1>角色权限</h1>
      <el-button type="primary" @click="openCreate">新增角色</el-button>
    </header>
    <el-table :data="roles" border>
      <el-table-column prop="code" label="角色编码" width="180" />
      <el-table-column prop="name" label="角色名称" width="140" />
      <el-table-column prop="description" label="说明" />
      <el-table-column label="系统内置" width="100">
        <template #default="scope"><el-tag>{{ scope.row.systemBuiltIn ? '是' : '否' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="权限数" width="90">
        <template #default="scope">{{ scope.row.permissionCodes.length }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="scope">
          <el-button link type="primary" @click="openEdit(scope.row)">编辑</el-button>
          <el-button link type="danger" :disabled="scope.row.systemBuiltIn" @click="removeRole(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="760px">
      <el-form label-width="90px">
        <el-form-item label="角色编码"><el-input v-model="form.code" :disabled="!!editingCode" /></el-form-item>
        <el-form-item label="角色名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="form.description" type="textarea" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="1" /></el-form-item>
        <el-form-item label="启用"><el-switch v-model="form.enabled" /></el-form-item>
        <el-form-item label="权限点">
          <div class="permission-groups">
            <div v-for="[domain, items] in groupedPermissions" :key="domain" class="permission-group">
              <h4>{{ domain }}</h4>
              <el-checkbox-group v-model="form.permissionCodes">
                <div v-for="perm in items" :key="perm.code" class="permission-item">
                  <el-checkbox :label="perm.code" :value="perm.code">
                    {{ perm.name }}（{{ perm.code }}）
                  </el-checkbox>
                  <p>{{ perm.description }}</p>
                </div>
              </el-checkbox-group>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.permission-groups { max-height: 360px; overflow: auto; width: 100%; }
.permission-group { margin-bottom: 16px; }
.permission-group h4 { margin: 0 0 8px; }
.permission-item { margin-bottom: 8px; }
.permission-item p { margin: 2px 0 0 24px; color: #64748b; font-size: 12px; }
</style>
