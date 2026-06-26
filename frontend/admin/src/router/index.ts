import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '../views/Dashboard.vue'
import UserList from '../views/iam/UserList.vue'
import RoleList from '../views/iam/RoleList.vue'
import PermissionMatrix from '../views/iam/PermissionMatrix.vue'
import MenuList from '../views/system/MenuList.vue'
import ProcessDefinitionList from '../views/oa/ProcessDefinitionList.vue'
import ProcessDesigner from '../views/oa/ProcessDesigner.vue'
import MyStartedInstances from '../views/oa/MyStartedInstances.vue'
import OaStartCenter from '../views/oa/OaStartCenter.vue'
import InboundApply from '../views/oa/InboundApply.vue'
import OutboundApply from '../views/oa/OutboundApply.vue'
import ReimbursementApply from '../views/oa/ReimbursementApply.vue'
import WarehouseList from '../views/inventory/WarehouseList.vue'
import ItemList from '../views/inventory/ItemList.vue'
import PriceList from '../views/inventory/PriceList.vue'
import StockList from '../views/inventory/StockList.vue'
import StockTxnList from '../views/inventory/StockTxnList.vue'
import InboundOrderList from '../views/inventory/InboundOrderList.vue'
import OutboundOrderList from '../views/inventory/OutboundOrderList.vue'
import Login from '../views/Login.vue'
import MobileLayout from '../mobile/MobileLayout.vue'
import MobileOaHome from '../mobile/pages/MobileOaHome.vue'
import MobileOaLogin from '../mobile/pages/MobileOaLogin.vue'
import MobileStartCenter from '../mobile/pages/MobileStartCenter.vue'
import MobileInboundApply from '../mobile/pages/MobileInboundApply.vue'
import MobileOutboundApply from '../mobile/pages/MobileOutboundApply.vue'
import MobileReimbursementApply from '../mobile/pages/MobileReimbursementApply.vue'
import MobileTodoTasks from '../mobile/pages/MobileTodoTasks.vue'
import MobileHandledTasks from '../mobile/pages/MobileHandledTasks.vue'
import MobileStartedInstances from '../mobile/pages/MobileStartedInstances.vue'
import MobileTaskDetail from '../mobile/pages/MobileTaskDetail.vue'
import { isLoggedIn, isMobileLoggedIn } from '../api/http'

function currentRoles(): string[] {
  const raw = localStorage.getItem('his.currentUser')
  if (!raw) return []
  try {
    return (JSON.parse(raw) as { roleCodes?: string[] }).roleCodes ?? []
  } catch {
    return []
  }
}

function hasAnyRole(...roles: string[]) {
  const userRoles = currentRoles()
  return roles.some((role) => userRoles.includes(role))
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: Login, meta: { public: true } },
    { path: '/m/oa/login', component: MobileOaLogin, meta: { public: true, mobile: true } },
    {
      path: '/m/oa',
      component: MobileLayout,
      meta: { mobile: true },
      children: [
        { path: '', component: MobileOaHome },
        { path: 'start', component: MobileStartCenter },
        { path: 'start/inbound', component: MobileInboundApply },
        { path: 'start/outbound', component: MobileOutboundApply },
        { path: 'start/reimbursement', component: MobileReimbursementApply },
        { path: 'todo', component: MobileTodoTasks },
        { path: 'handled', component: MobileHandledTasks },
        { path: 'mine', component: MobileStartedInstances },
        { path: 'tasks/:taskId', component: MobileTaskDetail },
      ],
    },
    { path: '/', component: Dashboard },
    { path: '/iam/users', component: UserList, meta: { roles: ['SYSTEM_ADMIN'] } },
    { path: '/iam/roles', component: RoleList, meta: { roles: ['SYSTEM_ADMIN'] } },
    { path: '/iam/permissions', component: PermissionMatrix, meta: { roles: ['SYSTEM_ADMIN'] } },
    { path: '/system/menus', component: MenuList, meta: { roles: ['SYSTEM_ADMIN'] } },
    { path: '/oa/start', component: OaStartCenter },
    { path: '/oa/processes', component: ProcessDefinitionList, meta: { roles: ['SYSTEM_ADMIN', 'OA_ADMIN'] } },
    { path: '/oa/designer', component: ProcessDesigner, meta: { roles: ['SYSTEM_ADMIN', 'OA_ADMIN'] } },
    { path: '/oa/started', component: MyStartedInstances },
    { path: '/oa/inbound', component: InboundApply },
    { path: '/oa/outbound', component: OutboundApply },
    { path: '/oa/reimbursement', component: ReimbursementApply },
    { path: '/inventory/warehouses', component: WarehouseList },
    { path: '/inventory/items', component: ItemList },
    { path: '/inventory/prices', component: PriceList },
    { path: '/inventory/stocks', component: StockList },
    { path: '/inventory/stock-transactions', component: StockTxnList },
    { path: '/inventory/inbound-orders', component: InboundOrderList },
    { path: '/inventory/outbound-orders', component: OutboundOrderList },
  ],
})

router.beforeEach((to) => {
  if (to.meta.mobile && to.path !== '/m/oa/login' && !isMobileLoggedIn()) {
    return { path: '/m/oa/login', query: { redirect: to.fullPath } }
  }
  if (!to.meta.public && !isLoggedIn()) {
    if (to.meta.mobile) return true
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && isLoggedIn()) {
    return '/'
  }
  const requiredRoles = to.meta.roles as string[] | undefined
  if (requiredRoles && !hasAnyRole(...requiredRoles)) {
    return '/'
  }
  return true
})

export default router
