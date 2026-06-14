<template>
  <div>
    <!-- Filters -->
    <div class="top-bar">
      <div class="filters">
        <div class="search-box">
          <i class="fas fa-search"></i>
          <input v-model="search" placeholder="Search events..." />
        </div>
        <select v-model="filterStatus" class="fs" style="width:140px">
          <option value="">All Statuses</option>
          <option v-for="s in statuses" :key="s" :value="s">{{ s.replace('_',' ') }}</option>
        </select>
        <select v-model="filterCat" class="fs" style="width:140px">
          <option value="">All Categories</option>
          <option v-for="c in categories" :key="c" :value="c">{{ c.replace('_',' ') }}</option>
        </select>
      </div>
    </div>

    <div class="card mt2">
      <div v-if="loading" class="loading-box"><i class="fas fa-spinner fa-spin"></i> Loading events...</div>
      <div v-else class="tw">
        <table>
          <thead>
            <tr>
              <th>#</th><th>Event</th><th>Organizer</th><th>Category</th>
              <th>Date</th><th>Capacity</th><th>Payment</th><th>Status</th><th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="ev in filtered" :key="ev.id">
              <td><code>{{ String(ev.id).padStart(3,'0') }}</code></td>
              <td>
                <strong>{{ ev.eventName }}</strong>
                <div class="tx3c txs">{{ ev.venue }}</div>
              </td>
              <td>{{ ev.organizerName }}</td>
              <td>{{ ev.category?.replace('_',' ') }}</td>
              <td>{{ ev.eventDate }}</td>
              <td>{{ ev.capacity }}</td>
              <td>
                <StatusBadge v-if="ev.payment" :status="ev.payment.status" />
                <span v-else class="badge badge-gray">N/A</span>
              </td>
              <td><StatusBadge :status="ev.status" /></td>
              <td>
                <div style="display:flex;gap:6px;flex-wrap:wrap">
                  <button class="btn btn-g btn-sm" @click="viewEvent(ev)" title="View">
                    <i class="fas fa-eye"></i>
                  </button>
                  <button class="btn btn-p btn-sm" @click="openStatusModal(ev)" title="Update Status">
                    <i class="fas fa-edit"></i>
                  </button>
                  <button v-if="ev.status === 'APPROVED' && ev.payment?.status === 'APPROVED'"
                    class="btn btn-ok btn-sm" @click="goGenerate(ev)" title="Generate Cards">
                    <i class="fas fa-envelope"></i> Cards
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!filtered.length">
              <td colspan="9" style="text-align:center;color:var(--tx3);padding:30px">No events found</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Event Detail Modal -->
    <AppModal v-model="showDetail" :title="selected?.eventName" width="700px">
      <div v-if="selected" class="event-detail">
        <div class="detail-grid">
          <div><span class="dl">Category</span><span>{{ selected.category?.replace('_',' ') }}</span></div>
          <div><span class="dl">Organizer</span><span>{{ selected.organizerName }}</span></div>
          <div><span class="dl">Date</span><span>{{ selected.eventDate }}</span></div>
          <div><span class="dl">Time</span><span>{{ selected.startTime }} – {{ selected.endTime }}</span></div>
          <div><span class="dl">Venue</span><span>{{ selected.venue }}</span></div>
          <div><span class="dl">Capacity</span><span>{{ selected.capacity }}</span></div>
          <div><span class="dl">Layout</span><span>{{ selected.selectedLayout }}</span></div>
          <div><span class="dl">Total Amount</span><span style="color:var(--p);font-weight:700">${{ selected.totalAmount }}</span></div>
          <div><span class="dl">Status</span><StatusBadge :status="selected.status" /></div>
          <div><span class="dl">Cards Generated</span><span>{{ selected.generatedCards || 0 }}</span></div>
        </div>
        <div v-if="selected.description" class="desc-box">
          <div class="dl mb1">Description</div>
          <p>{{ selected.description }}</p>
        </div>
        <div v-if="selected.adminRemarks" class="al al-in mt2">
          <i class="fas fa-info-circle"></i>
          <div><strong>Admin Remarks:</strong> {{ selected.adminRemarks }}</div>
        </div>
      </div>
      <template #footer>
        <button class="btn btn-g" @click="showDetail = false">Close</button>
        <button v-if="selected?.status === 'APPROVED' && selected?.payment?.status === 'APPROVED'"
          class="btn btn-ok" @click="goGenerate(selected); showDetail = false">
          <i class="fas fa-magic"></i> Generate Cards
        </button>
        <button class="btn btn-p" @click="openStatusModal(selected); showDetail = false">
          <i class="fas fa-edit"></i> Update Status
        </button>
      </template>
    </AppModal>

    <!-- Status Update Modal -->
    <AppModal v-model="showStatus" title="Update Event Status">
      <div class="fg">
        <label class="fl req">New Status</label>
        <select v-model="statusForm.status" class="fs">
          <option v-for="s in statuses" :key="s" :value="s">{{ s.replace('_',' ') }}</option>
        </select>
      </div>
      <div class="fg">
        <label class="fl">Admin Remarks</label>
        <textarea v-model="statusForm.adminRemarks" class="fta" placeholder="Optional remarks for organizer..."></textarea>
      </div>
      <div class="al al-in">
        <i class="fas fa-info-circle"></i>
        Organizer will be notified by <strong>email</strong> and <strong>SMS</strong> automatically.
      </div>
      <template #footer>
        <button class="btn btn-g" @click="showStatus = false">Cancel</button>
        <button class="btn btn-p" @click="submitStatus" :disabled="saving">
          <i v-if="saving" class="fas fa-spinner fa-spin"></i>
          <i v-else class="fas fa-save"></i>
          {{ saving ? 'Saving...' : 'Save & Notify' }}
        </button>
      </template>
    </AppModal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { adminApi } from '../../api'
import { useToast } from 'vue-toastification'
import StatusBadge from '../../components/StatusBadge.vue'
import AppModal from '../../components/AppModal.vue'

const router = useRouter()
const toast = useToast()

const events = ref([])
const loading = ref(true)
const search = ref('')
const filterStatus = ref('')
const filterCat = ref('')
const selected = ref(null)
const showDetail = ref(false)
const showStatus = ref(false)
const saving = ref(false)
const statusForm = ref({ status: '', adminRemarks: '' })

const statuses = ['PENDING','APPROVED','REJECTED','IN_PROGRESS','COMPLETED']
const categories = ['SEMINAR','WORKSHOP','CONFERENCE','WEDDING','CORPORATE_EVENT']

const filtered = computed(() => {
  return events.value.filter(ev => {
    const q = search.value.toLowerCase()
    const matchQ = !q || ev.eventName?.toLowerCase().includes(q) || ev.organizerName?.toLowerCase().includes(q)
    const matchS = !filterStatus.value || ev.status === filterStatus.value
    const matchC = !filterCat.value || ev.category === filterCat.value
    return matchQ && matchS && matchC
  })
})

async function loadEvents() {
  loading.value = true
  try {
    const { data } = await adminApi.getEvents()
    events.value = data.data
  } catch (e) {
    toast.error('Failed to load events')
  } finally {
    loading.value = false
  }
}

function viewEvent(ev) {
  selected.value = ev
  showDetail.value = true
}

function openStatusModal(ev) {
  selected.value = ev
  statusForm.value = { status: ev.status, adminRemarks: ev.adminRemarks || '' }
  showStatus.value = true
}

async function submitStatus() {
  saving.value = true
  try {
    const { data } = await adminApi.updateEventStatus(selected.value.id, statusForm.value)
    const idx = events.value.findIndex(e => e.id === selected.value.id)
    if (idx >= 0) events.value[idx] = data.data
    showStatus.value = false
    toast.success('Status updated! Organizer notified via Email & SMS.')
  } catch (e) {
    toast.error(e.response?.data?.message || 'Failed to update status')
  } finally {
    saving.value = false
  }
}

function goGenerate(ev) {
  router.push(`/admin/invitations/${ev.id}`)
}

onMounted(loadEvents)
</script>

<style scoped>
.top-bar { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px; }
.filters { display: flex; gap: 10px; flex-wrap: wrap; }
.search-box {
  display: flex; align-items: center; gap: 7px; background: var(--bg3);
  border: 1px solid var(--bd); border-radius: 8px; padding: 6px 12px;
}
.search-box input { border: none; background: transparent; outline: none; font-size: 13px; color: var(--tx); width: 200px; }
.loading-box { padding: 40px; text-align: center; color: var(--tx3); font-size: 15px; }
.event-detail .detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; font-size: 13px; margin-bottom: 14px; }
.dl { display: block; font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: .4px; color: var(--tx3); margin-bottom: 3px; }
.desc-box { background: var(--bg3); border-radius: 8px; padding: 14px; font-size: 13px; color: var(--tx2); }
</style>
