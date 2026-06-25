<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { iamApi, type RoleRecord } from '../../api/iam'
import { systemApi, type MenuRecord } from '../../api/system'

const menus = ref<MenuRecord[]>([])
const roles = ref<RoleRecord[]>([])
const selectedRole = ref('SYSTEM_ADMIN')
const checkedMenuIds = ref<number[]>([])
const treeRef = ref()
const dialogVisible = ref(false)
const form = reactive({ code: '', name: '', parentId: undefined as number | undefined, path: '', icon: '', sortOrder: 100 })

const menuTree = computed(() => buildMenuTree(menus.value))
const parentOptions = computed(() => menus.value.map((menu) => ({ label: `${menu.name}（${menu.code}）`, value: menu.id })))

function buildMenuTree(source: MenuRecord[]) {
  const map = new Map<number, MenuRecord & { children: MenuRecord[] }>()
  source.forEach((menu) => map.set(menu.id, { ...menu, children: [] }))
  const roots: Array<MenuRecord & { children: MenuRecord[] }> = []
  map.forEach((node) => {
    if (node.parentId && map.has(node.parentId)) {
      map.get(node.parentId)?.children.push(node)
    } else {
      roots.push(node)
    }
  })
  const sort = (nodes: Array<MenuRecord & { children: MenuRecord[] }>) => {
    nodes.sort((a, b) => a.sortOrder - b.sortOrder)
    nodes.forEach((node) => sort(node.children as Array<MenuRecord & { children: MenuRecord[] }>))
  }
  sort(roots)
  return roots
}

async function load() {
  menus.value = await systemApi.menus()
  roles.value = await iamApi.roles()
  await loadRoleMenus()
}

async function loadRoleMenus() {
  checkedMenuIds.value = await systemApi.roleMenus(selectedRole.value)
  await nextTick()
  treeRef.value?.setCheckedKeys(checkedMenuIds.value)
}

async function saveRoleMenus() {
  const checked = treeRef.value?.getCheckedKeys(false) ?? checkedMenuIds.value
  const halfChecked = treeRef.value?.getHalfCheckedKeys?.() ?? []
  await systemApi.saveRoleMenus(selectedRole.value, [...new Set([...checked, ...halfChecked])])
  ElMessage.success('角色菜单已保存')
}

async function createMenu() {
  if (!form.code || !form.name) {
    ElMessage.warning('请填写菜单编码和名称')
    return
  }
  await systemApi.createMenu(form)
  ElMessage.success('菜单已创建')
  dialogVisible.value = false
  await load()
}

onMounted(load)
</script>

<template>
  <section class="page-section">
    <header class="section-head">
      <h1>菜单管理</h1>
      <el-button type="primary" @click="dialogVisible = true">新增菜单</el-button>
    </header>

    <el-row :gutter="16">
      <el-col :span="12">
        <h3>菜单列表</h3>
        <el-table :data="menuTree" row-key="id" border :tree-props="{ children: 'children' }">
          <el-table-column prop="code" label="编码" width="160" />
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="path" label="路径" />
          <el-table-column prop="parentId" label="上级" width="90" />
          <el-table-column prop="sortOrder" label="排序" width="80" />
        </el-table>
      </el-col>
      <el-col :span="12">
        <h3>角色菜单配置</h3>
        <el-select v-model="selectedRole" class="form-control" @change="loadRoleMenus">
          <el-option v-for="role in roles" :key="role.code" :label="role.name" :value="role.code" />
        </el-select>
        <el-tree
          ref="treeRef"
          style="margin-top: 12px"
          :data="menuTree"
          node-key="id"
          show-checkbox
          default-expand-all
          :props="{ label: 'name', children: 'children' }"
        />
        <el-button type="primary" style="margin-top: 12px" @click="saveRoleMenus">保存角色菜单</el-button>
      </el-col>
    </el-row>

    <el-dialog v-model="dialogVisible" title="新增菜单" width="520px">
      <el-form label-width="90px">
        <el-form-item label="编码"><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="上级目录">
          <el-select v-model="form.parentId" clearable filterable class="form-control">
            <el-option v-for="menu in parentOptions" :key="menu.value" :label="menu.label" :value="menu.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="路径"><el-input v-model="form.path" /></el-form-item>
        <el-form-item label="图标"><el-input v-model="form.icon" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="1" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="createMenu">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>
