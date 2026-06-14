<template>
  <div>
    <div v-if="user?.accountStatus === 'PENDING_APPROVAL'" class="al al-wa mb2">
      <i class="fas fa-hourglass-half"></i>
      Your account is <strong>pending admin approval</strong>. You can explore but some features are locked.
    </div>

    <div class="stat-grid">
      <StatCard icon="fas fa-calendar-check" iconBg="#d1fae5" iconColor="#059669"
        :value="stats.totalEvents || 0" label="My Events" />
      <StatCard icon="fas fa-hourglass" iconBg="#fef3c7" iconColor="#d97706"
        :value="stats.pendingEvents || 0" label="Pending" />
      <StatCard icon="fas fa-check-circle" iconBg="#d1fae5" iconColor="#059669"
        :value="stats.approvedEvents || 0" label="Approved" />
      <StatCard icon="fas fa-envelope" iconBg="#e0f2fe" iconColor="#0284c7"
        value="—" label="Invitations Sent" />
    </div>

    <div class="gr2">
      <div class="card">
        <div class="ch">
          <div class="ct">My Recent Events</div>
          <router-link to="/organizer/events/create" class="btn btn-p btn-sm">
            <i class="fas fa-plus"></i> New Event
          </router-link>
        </div>
        <div v-if="loading" class="loading-box"><i class="fas fa-spinner fa-spin"></i></div>
        <div v-else class="tw">
          <table>
            <thead><tr><th>Event</th><th>Status</th><th>Payment</th><th>Date</th></tr></thead>
            <tbody>
              <tr v-for="ev in recentEvents" :key="ev.id">
                <td><strong>{{ ev.eventName }}</strong></td>
                <td><StatusBadge :status="ev.status" /></td>
                <td><StatusBadge v-if="ev.payment" :status="ev.payment.status" /></td>
                <td>{{ ev.eventDate }}</td>
              </tr>
              <tr v-if="!recentEvents.length">
                <td colspan="4" style="text-align:center;color:var(--tx3);padding:20px">
                  No events yet. <router-link to="/organizer/events/create">Create your first event!</router-link>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="card">
        <div class="ct mb2">Workflow Guide</div>
        <ul class="workflow-list">
          <li class="done"><i class="fas fa-check-circle"></i> Register & get admin approval</li>
          <li :class="{ done: stats.totalEvents > 0 }">
            <i :class="stats.totalEvents > 0 ? 'fas fa-check-circle' : 'fas fa-circle'"></i>
            Create event (select layout, auto-pricing)
          </li>
          <li>
            <i class="fas fa-circle"></i>
            Upload payment proof
          </li>
          <li>
            <i class="fas fa-circle"></i>
            Admin verifies payment & generates cards
          </li>
          <li>
            <i class="fas fa-circle"></i>
            Send invitation cards to participants
          </li>
          <li>
            <i class="fas fa-circle"></i>
            Scan QR codes at event entry
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { orgApi } from '../../api'
import { useAuthStore } from '../../stores/auth'
import StatCard from '../../components/StatCard.vue'
import StatusBadge from '../../components/StatusBadge.vue'

const auth = useAuthStore()
const user = computed(() => auth.user)
const stats = ref({})
const recentEvents = ref([])
const loading = ref(true)

onMounted(async () => {
  loading.value = true
  try {
    const [dashRes, evRes] = await Promise.all([orgApi.dashboard(), orgApi.getMyEvents()])
    stats.value = dashRes.data.data
    recentEvents.value = evRes.data.data.slice(0, 5)
  } catch (e) {} finally { loading.value = false }
})
</script>

<style scoped>
.stat-grid { display: grid; grid-template-columns: repeat(4,1fr); gap: 14px; margin-bottom: 22px; }
.gr2 { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }
.ch { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.ct { font-size: 15px; font-weight: 600; }
.loading-box { padding: 30px; text-align: center; color: var(--tx3); }
.workflow-list { list-style: none; display: flex; flex-direction: column; gap: 12px; }
.workflow-list li { display: flex; align-items: center; gap: 10px; font-size: 13px; color: var(--tx2); }
.workflow-list li i { font-size: 16px; color: var(--bd2); flex-shrink: 0; }
.workflow-list li.done { color: var(--ok); }
.workflow-list li.done i { color: var(--ok); }
@media (max-width: 900px) { .stat-grid { grid-template-columns: 1fr 1fr; } .gr2 { grid-template-columns: 1fr; } }
</style>
