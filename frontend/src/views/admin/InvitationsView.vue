<template>
  <div>
    <div class="al al-in mb2">
      <i class="fas fa-info-circle"></i>
      Cards are auto-generated based on the event's <strong>capacity</strong> set by the organizer.
      Only events with <strong>Approved</strong> status and <strong>Approved</strong> payment are eligible.
    </div>

    <div class="card">
      <div class="ch">
        <div class="ct">Events – Generate Invitation Cards</div>
        <div class="search-box">
          <i class="fas fa-search"></i>
          <input v-model="search" placeholder="Search events..." />
        </div>
      </div>

      <div v-if="loading" class="loading-box">
        <i class="fas fa-spinner fa-spin"></i> Loading...
      </div>

      <div v-else class="tw">
        <table>
          <thead>
            <tr>
              <th>Event</th>
              <th>Organizer</th>
              <th>Layout</th>
              <th>Capacity</th>
              <th>Status</th>
              <th>Payment</th>
              <th>Cards</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="ev in filtered" :key="ev.id">
              <td>
                <strong>{{ ev.eventName }}</strong>
                <div class="tx3c txs">{{ ev.eventDate }}</div>
              </td>
              <td>
                <div>{{ ev.organizerName }}</div>
                <div class="tx3c txs">{{ ev.organizerEmail }}</div>
              </td>
              <td>
                <span class="layout-badge" :class="'lb-' + ev.selectedLayout?.toLowerCase()">
                  {{ ev.selectedLayout }}
                </span>
              </td>
              <td>
                <span class="badge badge-gray">
                  <i class="fas fa-users"></i> {{ ev.capacity }}
                </span>
              </td>
              <td><StatusBadge :status="ev.status" /></td>
              <td>
                <StatusBadge v-if="ev.payment" :status="ev.payment.status" />
                <span v-else class="badge badge-gray">N/A</span>
              </td>
              <td>
                <span class="badge" :class="ev.generatedCards > 0 ? 'badge-ok' : 'badge-gray'">
                  {{ ev.generatedCards || 0 }} / {{ ev.capacity }}
                </span>
              </td>
              <td>
                <div style="display:flex;gap:6px;flex-wrap:wrap">

                  <!-- Generate button — only when approved+paid -->
                  <button
                    class="btn btn-p btn-sm"
                    :disabled="ev.status !== 'APPROVED' || ev.payment?.status !== 'APPROVED' || generating === ev.id"
                    @click="generateAndDownload(ev)"
                    :title="canGenerate(ev) ? (ev.generatedCards > 0 ? 'Regenerate all cards & download ZIP' : 'Generate ' + ev.capacity + ' cards & download ZIP') : 'Event or payment not approved'">
                    <i v-if="generating === ev.id" class="fas fa-spinner fa-spin"></i>
                    <i v-else class="fas fa-magic"></i>
                    {{ ev.generatedCards > 0 ? 'Regenerate' : 'Generate' }}
                  </button>

                  <!-- Download ZIP — only if cards already exist -->
                  <button
                    v-if="ev.generatedCards > 0"
                    class="btn btn-g btn-sm"
                    :disabled="downloading === ev.id"
                    @click="downloadZip(ev)"
                    title="Download ZIP of all cards">
                    <i v-if="downloading === ev.id" class="fas fa-spinner fa-spin"></i>
                    <i v-else class="fas fa-download"></i>
                    ZIP
                  </button>

                </div>
              </td>
            </tr>
            <tr v-if="!filtered.length">
              <td colspan="8" style="text-align:center;color:var(--tx3);padding:30px">
                No events found
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Confirm Regenerate Modal -->
    <AppModal v-model="showConfirm" title="Regenerate Cards?" width="440px">
      <div class="al al-wa mb2">
        <i class="fas fa-exclamation-triangle"></i>
        This will <strong>delete all existing {{ confirmEvent?.generatedCards }} cards</strong>
        for "{{ confirmEvent?.eventName }}" and create {{ confirmEvent?.capacity }} new ones.
      </div>
      <div class="al al-in">
        <i class="fas fa-info-circle"></i>
        A new ZIP will download automatically and the organizer will be notified.
      </div>
      <template #footer>
        <button class="btn btn-g" @click="showConfirm = false">Cancel</button>
        <button class="btn btn-er" @click="confirmRegenerate" :disabled="generating !== null">
          <i v-if="generating !== null" class="fas fa-spinner fa-spin"></i>
          <i v-else class="fas fa-redo"></i>
          Yes, Regenerate
        </button>
      </template>
    </AppModal>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { adminApi } from '../../api'
import { useToast } from 'vue-toastification'
import StatusBadge from '../../components/StatusBadge.vue'
import AppModal from '../../components/AppModal.vue'

const toast = useToast()
const events = ref([])
const loading = ref(true)
const search = ref('')
const generating = ref(null)
const downloading = ref(null)
const showConfirm = ref(false)
const confirmEvent = ref(null)

const filtered = computed(() => {
  const q = search.value.toLowerCase()
  return events.value.filter(e =>
    !q ||
    e.eventName?.toLowerCase().includes(q) ||
    e.organizerName?.toLowerCase().includes(q)
  )
})

function canGenerate(ev) {
  return ev.status === 'APPROVED' && ev.payment?.status === 'APPROVED'
}

async function loadEvents() {
  loading.value = true
  try {
    const { data } = await adminApi.getEvents()
    events.value = data.data
  } catch {
    toast.error('Failed to load events')
  } finally {
    loading.value = false
  }
}

function generateAndDownload(ev) {
  // If cards already exist, confirm before regenerating
  if (ev.generatedCards > 0) {
    confirmEvent.value = ev
    showConfirm.value = true
    return
  }
  doGenerate(ev)
}

async function confirmRegenerate() {
  showConfirm.value = false
  await doGenerate(confirmEvent.value)
}

async function doGenerate(ev) {
  generating.value = ev.id
  try {
    toast.info(`Generating ${ev.capacity} cards for "${ev.eventName}"...`)
    const response = await adminApi.generateCards(ev.id)
    triggerZipDownload(response.data, `invitations-${ev.eventName}-${ev.id}.zip`)
    // Update count in table reactively
    const idx = events.value.findIndex(e => e.id === ev.id)
    if (idx >= 0) events.value[idx].generatedCards = ev.capacity
    toast.success(`✅ ${ev.capacity} cards generated! ZIP downloaded. Organizer notified.`)
  } catch (e) {
    toast.error(e.response?.data?.message || 'Generation failed. Check backend logs.')
  } finally {
    generating.value = null
  }
}

async function downloadZip(ev) {
  downloading.value = ev.id
  try {
    const response = await adminApi.downloadCards(ev.id)
    triggerZipDownload(response.data, `invitations-${ev.eventName}-${ev.id}.zip`)
    toast.success('ZIP downloaded!')
  } catch {
    toast.error('Download failed')
  } finally {
    downloading.value = null
  }
}

function triggerZipDownload(blob, filename) {
  const url = window.URL.createObjectURL(new Blob([blob], { type: 'application/zip' }))
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  window.URL.revokeObjectURL(url)
}

onMounted(loadEvents)
</script>

<style scoped>
.ch { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; flex-wrap: wrap; gap: 10px; }
.ct { font-size: 15px; font-weight: 600; }
.search-box { display: flex; align-items: center; gap: 7px; background: var(--bg3); border: 1px solid var(--bd); border-radius: 8px; padding: 6px 12px; }
.search-box input { border: none; background: transparent; outline: none; font-size: 13px; color: var(--tx); width: 180px; }
.loading-box { padding: 40px; text-align: center; color: var(--tx3); }
.layout-badge { padding: 3px 8px; border-radius: 20px; font-size: 11px; font-weight: 600; }
.lb-classic    { background: #e8eaf6; color: #3949ab; }
.lb-modern     { background: #e0f2f1; color: #00695c; }
.lb-corporate  { background: #eceff1; color: #546e7a; }
.lb-wedding    { background: #fce4ec; color: #ad1457; }
.lb-conference { background: #fbe9e7; color: #d84315; }
.lb-premium    { background: #f3e5f5; color: #7b1fa2; }
</style>