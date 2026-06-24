import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '../views/Dashboard.vue'
import UserList from '../views/iam/UserList.vue'
import RoleList from '../views/iam/RoleList.vue'
import PermissionMatrix from '../views/iam/PermissionMatrix.vue'
import ProcessDefinitionList from '../views/oa/ProcessDefinitionList.vue'
import ProcessDesigner from '../views/oa/ProcessDesigner.vue'
import MyStartedInstances from '../views/oa/MyStartedInstances.vue'
import MyTodoTasks from '../views/oa/MyTodoTasks.vue'
import InboundApply from '../views/oa/InboundApply.vue'
import OutboundApply from '../views/oa/OutboundApply.vue'
import ReimbursementApply from '../views/oa/ReimbursementApply.vue'
import WarehouseList from '../views/inventory/WarehouseList.vue'
import ItemList from '../views/inventory/ItemList.vue'
import PriceList from '../views/inventory/PriceList.vue'
import StockList from '../views/inventory/StockList.vue'
import InboundOrderList from '../views/inventory/InboundOrderList.vue'
import OutboundOrderList from '../views/inventory/OutboundOrderList.vue'
import Login from '../views/Login.vue'
import { isLoggedIn } from '../api/http'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: Login, meta: { public: true } },
    { path: '/', component: Dashboard },
    { path: '/iam/users', component: UserList },
    { path: '/iam/roles', component: RoleList },
    { path: '/iam/permissions', component: PermissionMatrix },
    { path: '/oa/processes', component: ProcessDefinitionList },
    { path: '/oa/designer', component: ProcessDesigner },
    { path: '/oa/started', component: MyStartedInstances },
    { path: '/oa/todo', component: MyTodoTasks },
    { path: '/oa/inbound', component: InboundApply },
    { path: '/oa/outbound', component: OutboundApply },
    { path: '/oa/reimbursement', component: ReimbursementApply },
    { path: '/inventory/warehouses', component: WarehouseList },
    { path: '/inventory/items', component: ItemList },
    { path: '/inventory/prices', component: PriceList },
    { path: '/inventory/stocks', component: StockList },
    { path: '/inventory/inbound-orders', component: InboundOrderList },
    { path: '/inventory/outbound-orders', component: OutboundOrderList },
  ],
})

router.beforeEach((to) => {
  if (!to.meta.public && !isLoggedIn()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && isLoggedIn()) {
    return '/'
  }
  return true
})

export default router
