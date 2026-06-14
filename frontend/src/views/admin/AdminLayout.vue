<template>
  <div class="layout-wrap">
    <div class="sb-overlay" v-if="sidebarOpen" @click="sidebarOpen = false"></div>

    <AppSidebar :navSections="navSections" :mobileOpen="sidebarOpen" @close="sidebarOpen = false" />

    <div class="layout-main">
      <AppTopbar :title="currentTitle" @toggle-sidebar="sidebarOpen = !sidebarOpen" />
      <div class="layout-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import AppSidebar from '../../components/AppSidebar.vue'
import AppTopbar from '../../components/AppTopbar.vue'

const sidebarOpen = ref(false)
const route = useRoute()

const pageTitles = {
  '/admin/dashboard': 'Dashboard',
  '/admin/events': 'Event Management',
  '/admin/payments': 'Payment Management',
  '/admin/invitations': 'Invitation Management',
  '/admin/users': 'User Management',
  '/admin/notifications': 'Notification Log',
  '/admin/reports': 'Reports & Analytics',
  '/admin/settings': 'Settings',
}

const currentTitle = computed(() => {
  return pageTitles[route.path] || route.path.split('/').pop()?.replace(/-/g,' ')?.replace(/\b\w/g,l=>l.toUpperCase()) || 'EventSphere'
})

const navSections = [
  {
    label: 'Main Menu',
    items: [
      { to: '/admin/dashboard', icon: 'fas fa-tachometer-alt', label: 'Dashboard' },
      { to: '/admin/events', icon: 'fas fa-calendar-alt', label: 'Events' },
      { to: '/admin/payments', icon: 'fas fa-credit-card', label: 'Payments' },
      { to: '/admin/invitations', icon: 'fas fa-envelope-open-text', label: 'Invitations' },
    ]
  },
  {
    label: 'Management',
    items: [
      { to: '/admin/users', icon: 'fas fa-users', label: 'Users' },
      { to: '/admin/notifications', icon: 'fas fa-paper-plane', label: 'Notification Log' },
      { to: '/admin/reports', icon: 'fas fa-chart-bar', label: 'Reports' },
      { to: '/admin/settings', icon: 'fas fa-cog', label: 'Settings' },
    ]
  }
]
</script>

<style scoped>
.layout-wrap { display: flex; min-height: 100vh; }
.layout-main { flex: 1; margin-left: var(--sw); display: flex; flex-direction: column; min-height: 100vh; }
.layout-content { padding: 22px; flex: 1; }
.sb-overlay { display: none; position: fixed; inset: 0; background: rgba(0,0,0,.5); z-index: 90; }
@media (max-width: 900px) {
  .layout-main { margin-left: 0; }
  .sb-overlay { display: block; }
}
</style>
