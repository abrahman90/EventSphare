<template>
  <div class="layout-wrap">
    <div class="sb-overlay" v-if="sidebarOpen" @click="sidebarOpen = false"></div>
    <AppSidebar :navSections="navSections" :mobileOpen="sidebarOpen" @close="sidebarOpen = false" />
    <div class="layout-main">
      <AppTopbar :title="currentTitle" @toggle-sidebar="sidebarOpen = !sidebarOpen" />
      <div class="layout-content"><router-view /></div>
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
  '/organizer/dashboard': 'My Dashboard',
  '/organizer/events/create': 'Create New Event',
  '/organizer/events': 'My Events',
  '/organizer/payments': 'My Payments',
  '/organizer/scanner': 'QR Scanner',
}

const currentTitle = computed(() => {
  const exact = pageTitles[route.path]
  if (exact) return exact
  if (route.path.includes('/invitations')) return 'My Invitations'
  if (route.path.includes('/events/')) return 'Event Details'
  return 'EventSphere'
})

const navSections = [
  {
    label: 'Main Menu',
    items: [
      { to: '/organizer/dashboard', icon: 'fas fa-tachometer-alt', label: 'Dashboard' },
      { to: '/organizer/events/create', icon: 'fas fa-plus-circle', label: 'Create Event' },
      { to: '/organizer/events', icon: 'fas fa-calendar-check', label: 'My Events' },
      { to: '/organizer/payments', icon: 'fas fa-receipt', label: 'My Payments' },
      { to: '/organizer/scanner', icon: 'fas fa-qrcode', label: 'QR Scanner' },
    ]
  }
]
</script>

<style scoped>
.layout-wrap { display: flex; min-height: 100vh; }
.layout-main { flex: 1; margin-left: var(--sw); display: flex; flex-direction: column; }
.layout-content { padding: 22px; flex: 1; }
.sb-overlay { display: none; position: fixed; inset: 0; background: rgba(0,0,0,.5); z-index: 90; }
@media (max-width: 900px) { .layout-main { margin-left: 0; } .sb-overlay { display: block; } }
</style>
