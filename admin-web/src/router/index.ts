import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '../views/DashboardView.vue'
import VehiclesView from '../views/VehiclesView.vue'
import MaterialPricesView from '../views/MaterialPricesView.vue'
import MaterialTemplatesView from '../views/MaterialTemplatesView.vue'
import LoginView from '../views/LoginView.vue'
import { useAuthStore } from '../stores/auth'
import MaterialFetchRunsView from '../views/MaterialFetchRunsView.vue'
import VehicleIngestRunsView from '../views/VehicleIngestRunsView.vue'
import UsersView from '../views/UsersView.vue'
import VehicleMappingsView from '../views/VehicleMappingsView.vue'
import MiitCpJobsView from '../views/MiitCpJobsView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView },
    { path: '/', name: 'dashboard', component: DashboardView },
    { path: '/vehicles', name: 'vehicles', component: VehiclesView },
    { path: '/material-prices', name: 'material-prices', component: MaterialPricesView },
    { path: '/material-templates', name: 'material-templates', component: MaterialTemplatesView },
    { path: '/jobs/material-price', name: 'jobs-material-price', component: MaterialFetchRunsView },
    { path: '/jobs/vehicle', name: 'jobs-vehicle', component: VehicleIngestRunsView },
    { path: '/jobs/miit-cp', name: 'jobs-miit-cp', component: MiitCpJobsView },
    { path: '/users', name: 'users', component: UsersView },
    { path: '/vehicle-mappings', name: 'vehicle-mappings', component: VehicleMappingsView },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.path === '/login') {
    if (auth.token) return { path: '/' }
    return true
  }
  if (!auth.token) {
    return { path: '/login' }
  }
  if (!auth.me) {
    try {
      await auth.loadMe()
    } catch {
      auth.logout()
      return { path: '/login' }
    }
  }
  return true
})

export default router
