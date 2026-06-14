<template>
  <aside class="sidebar" :class="{ open: mobileOpen }">
    <div class="sb-head">
      <div class="logo">
        <div class="logo-ic"><i class="fas fa-calendar-star"></i></div>
        <div>
          <div class="logo-tx">EventSphere</div>
          <div class="logo-sub">Smart Event Mgmt</div>
        </div>
      </div>
    </div>

    <nav class="sb-nav">
      <template v-for="section in navSections" :key="section.label">
        <div class="nav-lbl">{{ section.label }}</div>
        <router-link
          v-for="item in section.items"
          :key="item.to"
          :to="item.to"
          class="nav-it"
          active-class="on"
          @click="$emit('close')"
        >
          <span class="ic"><i :class="item.icon"></i></span>
          <span class="lb">{{ item.label }}</span>
          <span v-if="item.badge" class="bx">{{ item.badge }}</span>
        </router-link>
      </template>
    </nav>

    <div class="sb-foot">
      <div class="user-row">
        <div class="av" :style="{ background: avatarColor }">{{ initials }}</div>
        <div style="flex:1;min-width:0">
          <div class="user-name">{{ user?.fullName }}</div>
          <div class="user-role">{{ user?.role }}</div>
        </div>
        <button class="logout-btn" @click="handleLogout" title="Logout">
          <i class="fas fa-sign-out-alt"></i>
        </button>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const props = defineProps({
  navSections: Array,
  mobileOpen: Boolean,
})
const emit = defineEmits(['close'])

const auth = useAuthStore()
const router = useRouter()
const user = computed(() => auth.user)

const initials = computed(() => {
  const name = user.value?.fullName || ''
  return name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase()
})

const avatarColor = computed(() => {
  const colors = ['#1a56db', '#059669', '#7c3aed', '#d97706', '#0284c7']
  const idx = (user.value?.id || 0) % colors.length
  return colors[idx]
})

async function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.sidebar {
  width: var(--sw); background: var(--sb); position: fixed;
  left: 0; top: 0; bottom: 0; z-index: 100; display: flex;
  flex-direction: column; overflow-y: auto; overflow-x: hidden;
  transition: transform .3s;
}
.sb-head { padding: 18px 16px; border-bottom: 1px solid rgba(255,255,255,.06); }
.logo { display: flex; align-items: center; gap: 10px; }
.logo-ic {
  width: 38px; height: 38px; background: var(--p); border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; color: #fff; flex-shrink: 0;
}
.logo-tx { font-size: 17px; font-weight: 700; color: #fff; }
.logo-sub { font-size: 10px; color: #64748b; text-transform: uppercase; letter-spacing: .5px; }
.sb-nav { flex: 1; padding: 10px 10px 4px; }
.nav-lbl { font-size: 10px; font-weight: 600; text-transform: uppercase; letter-spacing: .8px; color: #475569; padding: 12px 8px 6px; }
.nav-it {
  display: flex; align-items: center; gap: 9px; padding: 8px 11px;
  border-radius: 8px; color: #94a3b8; transition: all .2s; cursor: pointer;
  margin-bottom: 2px; text-decoration: none;
}
.nav-it:hover { background: rgba(255,255,255,.06); color: #fff; }
.nav-it.on { background: var(--p); color: #fff; }
.ic { width: 18px; text-align: center; font-size: 14px; flex-shrink: 0; }
.lb { font-size: 13px; font-weight: 500; flex: 1; }
.bx { background: var(--er); color: #fff; font-size: 10px; padding: 1px 6px; border-radius: 10px; font-weight: 700; }
.sb-foot { padding: 14px 10px; border-top: 1px solid rgba(255,255,255,.06); }
.user-row { display: flex; align-items: center; gap: 10px; }
.av {
  width: 32px; height: 32px; border-radius: 8px; display: flex;
  align-items: center; justify-content: center; font-weight: 700;
  font-size: 13px; color: #fff; flex-shrink: 0;
}
.user-name { font-size: 13px; font-weight: 600; color: #e2e8f0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.user-role { font-size: 11px; color: #64748b; }
.logout-btn { background: none; border: none; color: #64748b; cursor: pointer; padding: 4px; }
.logout-btn:hover { color: #ef4444; }

@media (max-width: 900px) {
  .sidebar { transform: translateX(-100%); }
  .sidebar.open { transform: translateX(0); }
}
</style>
