import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  { path: '/', redirect: '/login' },
  {
    path: '/login',
    component: () => import('../views/auth/LoginView.vue'),
    meta: { guest: true }
  },
  {
    path: '/register',
    component: () => import('../views/auth/RegisterView.vue'),
    meta: { guest: true }
  },

  // ── Admin routes ────────────────────────────────────────────────────────────
  {
    path: '/admin',
    component: () => import('../views/admin/AdminLayout.vue'),
    meta: { requiresAuth: true, role: 'ADMIN' },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      {
        path: 'dashboard',
        component: () => import('../views/admin/DashboardView.vue')
      },
      {
        path: 'events',
        component: () => import('../views/admin/EventsView.vue')
      },
      {
        path: 'events/:id',
        component: () => import('../views/admin/EventDetailView.vue')
      },
      {
        path: 'payments',
        component: () => import('../views/admin/PaymentsView.vue')
      },
      {
        // Invitations list — admin generates cards from here, no detail page needed
        path: 'invitations',
        component: () => import('../views/admin/InvitationsView.vue')
      },
      {
        path: 'users',
        component: () => import('../views/admin/UsersView.vue')
      },
      {
        path: 'notifications',
        component: () => import('../views/admin/NotificationLogView.vue')
      },
      {
        path: 'reports',
        component: () => import('../views/admin/ReportsView.vue')
      },
      {
        path: 'settings',
        component: () => import('../views/admin/SettingsView.vue')
      },
    ]
  },

  // ── Organizer routes ────────────────────────────────────────────────────────
  {
    path: '/organizer',
    component: () => import('../views/organizer/OrganizerLayout.vue'),
    meta: { requiresAuth: true, role: 'ORGANIZER' },
    children: [
      { path: '', redirect: '/organizer/dashboard' },
      {
        path: 'dashboard',
        component: () => import('../views/organizer/DashboardView.vue')
      },
      {
        path: 'events/create',
        component: () => import('../views/organizer/CreateEventView.vue')
      },
      {
        path: 'events',
        component: () => import('../views/organizer/MyEventsView.vue')
      },
      {
        path: 'events/:id',
        component: () => import('../views/organizer/EventDetailView.vue')
      },
      {
        path: 'payments',
        component: () => import('../views/organizer/MyPaymentsView.vue')
      },
      {
        path: 'invitations/:eventId',
        component: () => import('../views/organizer/InvitationsView.vue')
      },
      {
        path: 'scanner',
        component: () => import('../views/organizer/ScannerView.vue')
      },
    ]
  },

  // Catch-all
  { path: '/:pathMatch(.*)*', redirect: '/login' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const auth = useAuthStore()

  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return next('/login')
  }
  if (to.meta.guest && auth.isLoggedIn) {
    return next(auth.isAdmin ? '/admin/dashboard' : '/organizer/dashboard')
  }
  if (to.meta.role && auth.user?.role !== to.meta.role) {
    return next(auth.isAdmin ? '/admin/dashboard' : '/organizer/dashboard')
  }
  next()
})

export default router