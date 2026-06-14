<template>
  <div>
    <!-- Stats -->
    <div class="stat-grid">
      <StatCard icon="fas fa-users" iconBg="#ede9fe" iconColor="#7c3aed"
        :value="stats.totalUsers || 0" label="Total Users" change="12% this month" changeType="up" />
      <StatCard icon="fas fa-calendar-check" iconBg="#d1fae5" iconColor="#059669"
        :value="stats.totalEvents || 0" label="Total Events" change="8% this month" changeType="up" />
      <StatCard icon="fas fa-dollar-sign" iconBg="#fee2e2" iconColor="#dc2626"
        :value="'$' + (stats.totalRevenue || 0).toLocaleString()" label="Total Revenue" change="23% this month" changeType="up" />
      <StatCard icon="fas fa-hourglass-half" iconBg="#fef3c7" iconColor="#d97706"
        :value="stats.pendingEvents || 0" label="Pending Events" />
      <StatCard icon="fas fa-hourglass" iconBg="#e0f2fe" iconColor="#0284c7"
        :value="stats.pendingApprovals || 0" label="Pending Approvals" />
    </div>

    <div class="gr2 mt2">
      <!-- Recent Events -->
      <div class="card">
        <div class="ch">
          <div class="ct">Recent Events</div>
          <router-link to="/admin/events" class="btn btn-g btn-sm">View All</router-link>
        </div>
        <div v-if="loading" class="loading-box">
          <i class="fas fa-spinner fa-spin"></i> Loading...
        </div>
        <div v-else class="tw">
          <table>
            <thead><tr><th>Event</th><th>Organizer</th><th>Status</th><th>Date</th></tr></thead>
            <tbody>
              <tr v-for="ev in recentEvents" :key="ev.id">
                <td>
                  <strong>{{ ev.eventName }}</strong>
                  <div class="tx3c txs">{{ ev.category }}</div>
                </td>
                <td>{{ ev.organizerName }}</td>
                <td><StatusBadge :status="ev.status" /></td>
                <td>{{ ev.eventDate }}</td>
              </tr>
              <tr v-if="!recentEvents.length">
                <td colspan="4" style="text-align:center;color:var(--tx3)">No events yet</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Pending Approvals (Users) -->
      <div class="card">
        <div class="ch">
          <div class="ct">Pending Organizer Approvals</div>
          <span class="badge badge-wa">{{ pendingUsers.length }}</span>
        </div>
        <div v-if="pendingUsers.length === 0" class="empty-state">
          <i class="fas fa-check-circle" style="color:var(--ok)"></i>
          <p>All caught up! No pending approvals.</p>
        </div>
        <div v-for="u in pendingUsers.slice(0,5)" :key="u.id" class="approval-item">
          <div class="av-sm" :style="{ background: '#1a56db' }">{{ u.fullName?.[0] }}</div>
          <div style="flex:1">
            <div class="fw6 txs">{{ u.fullName }}</div>
            <div class="tx3c txs">{{ u.email }}</div>
          </div>
          <div class="action-btns">
            <button class="btn btn-ok btn-sm" @click="approve(u.id)" :disabled="approving === u.id">
              <i class="fas fa-check"></i>
            </button>
            <button class="btn btn-er btn-sm" @click="reject(u.id)">
              <i class="fas fa-times"></i>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '../../api'
import { useToast } from 'vue-toastification'
import StatCard from '../../components/StatCard.vue'
import StatusBadge from '../../components/StatusBadge.vue'

const toast = useToast()
const stats = ref({})
const recentEvents = ref([])
const pendingUsers = ref([])
const loading = ref(true)
const approving = ref(null)

async function loadData() {
  loading.value = true
  try {
    const [dashRes, evRes, pendRes] = await Promise.all([
      adminApi.dashboard(),
      adminApi.getEvents(),
      adminApi.getPending(),
    ])
    stats.value = dashRes.data.data
    recentEvents.value = evRes.data.data.slice(0, 5)
    pendingUsers.value = pendRes.data.data
  } catch (e) {
    toast.error('Failed to load dashboard data')
  } finally {
    loading.value = false
  }
}

async function approve(id) {
  approving.value = id
  try {
    await adminApi.approveUser(id)
    toast.success('Organizer approved! Email & SMS sent.')
    pendingUsers.value = pendingUsers.value.filter(u => u.id !== id)
    stats.value.pendingApprovals = Math.max(0, (stats.value.pendingApprovals || 1) - 1)
  } catch (e) {
    toast.error('Failed to approve')
  } finally {
    approving.value = null
  }
}

async function reject(id) {
  if (!confirm('Reject this organizer?')) return
  try {
    await adminApi.deactivateUser(id)
    toast.info('Organizer rejected')
    pendingUsers.value = pendingUsers.value.filter(u => u.id !== id)
  } catch (e) {
    toast.error('Failed to reject')
  }
}

onMounted(loadData)
</script>

<style scoped>
.stat-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(175px,1fr)); gap: 14px; margin-bottom: 22px; }
.gr2 { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }
.ch { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.ct { font-size: 15px; font-weight: 600; }
.loading-box { padding: 30px; text-align: center; color: var(--tx3); }
.approval-item { display: flex; align-items: center; gap: 10px; padding: 10px 0; border-bottom: 1px solid var(--bd); }
.approval-item:last-child { border-bottom: none; }
.av-sm { width: 32px; height: 32px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 13px; color: #fff; flex-shrink: 0; }
.action-btns { display: flex; gap: 6px; }
.empty-state { text-align: center; padding: 30px; color: var(--tx3); }
.empty-state i { font-size: 32px; margin-bottom: 8px; display: block; }
@media (max-width: 900px) { .gr2 { grid-template-columns: 1fr; } }
</style>
