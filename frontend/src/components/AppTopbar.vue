<template>
  <header class="topbar">
    <div class="tb-left">
      <button class="hamburger" @click="$emit('toggle-sidebar')">
        <i class="fas fa-bars"></i>
      </button>
      <div class="page-title">{{ title }}</div>
    </div>
    <div class="tb-right">
      <div class="search-bar">
        <i class="fas fa-search"></i>
        <input v-model="searchQ" type="text" placeholder="Search..." />
      </div>

      <!-- Language -->
      <button class="tb-btn" :class="{ active: lang === 'en' }" @click="setLang('en')">🇬🇧 EN</button>
      <button class="tb-btn" :class="{ active: lang === 'sw' }" @click="setLang('sw')">🇹🇿 SW</button>

      <!-- Theme toggle -->
      <button class="tb-icon" @click="toggleTheme" :title="isDark ? 'Light mode' : 'Dark mode'">
        <i :class="isDark ? 'fas fa-sun' : 'fas fa-moon'"></i>
      </button>

      <!-- Notifications -->
      <div class="notif-wrap" ref="notifRef">
        <button class="tb-icon" @click="notifOpen = !notifOpen">
          <i class="fas fa-bell"></i>
          <span v-if="unreadCount > 0" class="bell-dot">{{ unreadCount }}</span>
        </button>
        <div class="notif-dropdown" v-if="notifOpen">
          <div class="nd-head">
            <span>Notifications</span>
            <button @click="markAllRead" class="nd-mark">Mark all read</button>
          </div>
          <div class="nd-list">
            <div v-if="notifications.length === 0" class="nd-empty">No notifications</div>
            <div v-for="n in notifications" :key="n.id"
                 class="nd-item" :class="{ unread: !n.read }">
              <div class="nd-title">{{ n.title }}</div>
              <div class="nd-msg">{{ n.message }}</div>
              <div class="nd-time">{{ n.time }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- User avatar -->
      <div class="user-av" :style="{ background: avatarColor }">
        {{ initials }}
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '../stores/auth'

const props = defineProps({ title: String })
const emit = defineEmits(['toggle-sidebar'])

const auth = useAuthStore()
const user = computed(() => auth.user)

const searchQ = ref('')
const notifOpen = ref(false)
const notifRef = ref(null)
const lang = ref(localStorage.getItem('es_lang') || 'en')
const isDark = ref(document.documentElement.getAttribute('data-theme') === 'dark')

const notifications = ref([
  { id: 1, title: 'Welcome!', message: 'You are now logged in to EventSphere.', time: 'Just now', read: false },
])

const unreadCount = computed(() => notifications.value.filter(n => !n.read).length)

const initials = computed(() => {
  const name = user.value?.fullName || ''
  return name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase()
})

const avatarColor = computed(() => {
  const colors = ['#1a56db', '#059669', '#7c3aed', '#d97706', '#0284c7']
  return colors[(user.value?.id || 0) % colors.length]
})

function toggleTheme() {
  isDark.value = !isDark.value
  const theme = isDark.value ? 'dark' : 'light'
  document.documentElement.setAttribute('data-theme', theme)
  localStorage.setItem('es_theme', theme)
}

function setLang(l) {
  lang.value = l
  localStorage.setItem('es_lang', l)
}

function markAllRead() {
  notifications.value.forEach(n => n.read = true)
}

function handleOutsideClick(e) {
  if (notifRef.value && !notifRef.value.contains(e.target)) {
    notifOpen.value = false
  }
}

onMounted(() => document.addEventListener('click', handleOutsideClick))
onUnmounted(() => document.removeEventListener('click', handleOutsideClick))
</script>

<style scoped>
.topbar {
  background: var(--bg2); border-bottom: 1px solid var(--bd);
  height: var(--hh); display: flex; align-items: center;
  justify-content: space-between; padding: 0 22px;
  position: sticky; top: 0; z-index: 50;
}
.tb-left { display: flex; align-items: center; gap: 14px; }
.hamburger {
  display: none; width: 34px; height: 34px; border-radius: 8px;
  background: var(--bg3); border: 1px solid var(--bd);
  align-items: center; justify-content: center; cursor: pointer; color: var(--tx2);
}
.page-title { font-size: 17px; font-weight: 600; color: var(--tx); }
.tb-right { display: flex; align-items: center; gap: 10px; }
.search-bar {
  display: flex; align-items: center; gap: 7px; background: var(--bg3);
  border: 1px solid var(--bd); border-radius: 8px; padding: 5px 11px; width: 200px;
}
.search-bar input { border: none; background: transparent; outline: none; font-size: 13px; color: var(--tx); width: 100%; }
.search-bar input::placeholder { color: var(--tx3); }
.tb-btn {
  padding: 4px 10px; border-radius: 6px; font-size: 11px; font-weight: 600;
  border: 1px solid var(--bd); background: var(--bg3); color: var(--tx2); cursor: pointer;
}
.tb-btn.active { background: var(--p); color: #fff; border-color: var(--p); }
.tb-icon {
  width: 34px; height: 34px; border-radius: 8px; background: var(--bg3);
  border: 1px solid var(--bd); display: flex; align-items: center; justify-content: center;
  cursor: pointer; color: var(--tx2); font-size: 14px; position: relative;
}
.tb-icon:hover { background: var(--pl); color: var(--p); }
.bell-dot {
  position: absolute; top: -3px; right: -3px; background: var(--er);
  color: #fff; font-size: 9px; font-weight: 700; padding: 1px 4px;
  border-radius: 8px; border: 2px solid var(--bg2);
}
.notif-wrap { position: relative; }
.notif-dropdown {
  position: absolute; top: calc(100% + 8px); right: 0; width: 300px;
  background: var(--bg2); border: 1px solid var(--bd); border-radius: 12px;
  box-shadow: 0 12px 32px rgba(0,0,0,.15); z-index: 200;
}
.nd-head {
  padding: 11px 14px; border-bottom: 1px solid var(--bd);
  display: flex; justify-content: space-between; align-items: center;
  font-weight: 600; font-size: 14px;
}
.nd-mark { font-size: 12px; color: var(--p); background: none; border: none; cursor: pointer; }
.nd-list { max-height: 300px; overflow-y: auto; }
.nd-empty { padding: 20px; text-align: center; color: var(--tx3); font-size: 13px; }
.nd-item { padding: 11px 14px; border-bottom: 1px solid var(--bd); cursor: pointer; }
.nd-item:last-child { border-bottom: none; }
.nd-item.unread { background: var(--pl); }
.nd-item:hover { background: var(--bg3); }
.nd-title { font-size: 13px; font-weight: 600; color: var(--tx); margin-bottom: 2px; }
.nd-msg { font-size: 12px; color: var(--tx2); }
.nd-time { font-size: 10px; color: var(--tx3); margin-top: 3px; }
.user-av {
  width: 34px; height: 34px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  font-weight: 700; font-size: 13px; color: #fff;
}
@media (max-width: 900px) {
  .hamburger { display: flex; }
  .search-bar { display: none; }
  .tb-btn { display: none; }
}
</style>
