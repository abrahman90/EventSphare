<template>
  <div>
    <div class="top-bar mb2">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input v-model="search" placeholder="Search my events..." />
      </div>
      <router-link to="/organizer/events/create" class="btn btn-p btn-sm">
        <i class="fas fa-plus"></i> New Event
      </router-link>
    </div>

    <div class="card">
      <div v-if="loading" class="loading-box"><i class="fas fa-spinner fa-spin"></i></div>
      <div v-else class="tw">
        <table>
          <thead>
            <tr>
              <th>#</th><th>Event</th><th>Category</th><th>Date</th>
              <th>Capacity</th><th>Status</th><th>Payment</th><th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="ev in filtered" :key="ev.id">
              <td><code>{{ String(ev.id).padStart(3,'0') }}</code></td>
              <td>
                <strong>{{ ev.eventName }}</strong>
                <div class="tx3c txs">{{ ev.venue }}</div>
              </td>
              <td>{{ ev.category?.replace('_',' ') }}</td>
              <td>{{ ev.eventDate }}</td>
              <td>{{ ev.capacity }}</td>
              <td><StatusBadge :status="ev.status" /></td>
              <td>
                <StatusBadge v-if="ev.payment" :status="ev.payment.status" />
                <button
                  v-if="ev.payment && ev.payment.status === 'PENDING'"
                  class="btn btn-wa btn-sm"
                  @click="openUploadProof(ev)">
                  <i class="fas fa-upload"></i> Upload
                </button>
              </td>
              <td>
                <div style="display:flex;gap:5px;flex-wrap:wrap">
                  <!-- View details -->
                  <button class="btn btn-g btn-sm" @click="viewEvent(ev)">
                    <i class="fas fa-eye"></i>
                  </button>

                  <!-- Manage invitations -->
                  <button
                    v-if="ev.status === 'APPROVED' && ev.payment?.status === 'APPROVED'"
                    class="btn btn-p btn-sm"
                    @click="$router.push(`/organizer/invitations/${ev.id}`)">
                    <i class="fas fa-envelope"></i> Cards
                  </button>

                  <!-- Download ZIP — shown when cards have been generated -->
                  <button
                    v-if="ev.status === 'APPROVED' && ev.payment?.status === 'APPROVED' && ev.generatedCards > 0"
                    class="btn btn-g btn-sm"
                    :disabled="downloading === ev.id"
                    @click="downloadMyCards(ev)"
                    title="Download invitation cards as ZIP">
                    <i v-if="downloading === ev.id" class="fas fa-spinner fa-spin"></i>
                    <i v-else class="fas fa-download"></i>
                    ZIP
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!filtered.length">
              <td colspan="8" style="text-align:center;color:var(--tx3);padding:30px">
                No events found.
                <router-link to="/organizer/events/create">Create your first event!</router-link>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Event Detail Modal -->
    <AppModal v-model="showDetail" :title="selectedEvent?.eventName">
      <div v-if="selectedEvent">
        <div class="detail-grid">
          <div><span class="dl">Category</span>{{ selectedEvent.category?.replace('_',' ') }}</div>
          <div><span class="dl">Date</span>{{ selectedEvent.eventDate }}</div>
          <div><span class="dl">Venue</span>{{ selectedEvent.venue }}</div>
          <div><span class="dl">Capacity</span>{{ selectedEvent.capacity }}</div>
          <div><span class="dl">Layout</span>{{ selectedEvent.selectedLayout }}</div>
          <div><span class="dl">Amount</span><strong style="color:var(--p)">${{ selectedEvent.totalAmount }}</strong></div>
          <div><span class="dl">Status</span><StatusBadge :status="selectedEvent.status" /></div>
          <div><span class="dl">Cards</span>{{ selectedEvent.generatedCards || 0 }}</div>
        </div>
        <div v-if="selectedEvent.adminRemarks" class="al al-in mt2">
          <i class="fas fa-info-circle"></i>
          <strong>Admin:</strong> {{ selectedEvent.adminRemarks }}
        </div>
        <div v-if="selectedEvent.description" class="desc-box mt2">
          {{ selectedEvent.description }}
        </div>

        <!-- Cards ready notice -->
        <div v-if="selectedEvent.generatedCards > 0" class="al al-ok mt2">
          <i class="fas fa-check-circle"></i>
          {{ selectedEvent.generatedCards }} invitation cards are ready for download.
        </div>
      </div>
      <template #footer>
        <button class="btn btn-g" @click="showDetail = false">Close</button>

        <button
          v-if="selectedEvent?.payment?.status === 'PENDING'"
          class="btn btn-wa"
          @click="openUploadProof(selectedEvent); showDetail = false">
          <i class="fas fa-upload"></i> Upload Proof
        </button>

        <!-- Download ZIP from modal -->
        <button
          v-if="selectedEvent?.status === 'APPROVED' && selectedEvent?.payment?.status === 'APPROVED' && selectedEvent?.generatedCards > 0"
          class="btn btn-g"
          :disabled="downloading === selectedEvent?.id"
          @click="downloadMyCards(selectedEvent)">
          <i v-if="downloading === selectedEvent?.id" class="fas fa-spinner fa-spin"></i>
          <i v-else class="fas fa-download"></i>
          Download ZIP
        </button>

        <button
          v-if="selectedEvent?.status === 'APPROVED' && selectedEvent?.payment?.status === 'APPROVED'"
          class="btn btn-p"
          @click="$router.push(`/organizer/invitations/${selectedEvent.id}`); showDetail = false">
          <i class="fas fa-envelope"></i> Manage Invitations
        </button>
      </template>
    </AppModal>

    <!-- Upload Proof Modal -->
    <AppModal v-model="showUpload" title="Upload Payment Proof">
      <div class="al al-in mb2">
        <i class="fas fa-info-circle"></i>
        Invoice: <strong>{{ selectedEvent?.payment?.invoiceNumber }}</strong> –
        Amount: <strong>${{ selectedEvent?.payment?.amount }}</strong>
      </div>
      <div class="upz" @click="$refs.proofFile.click()">
        <i class="fas fa-cloud-upload-alt"
          style="font-size:32px;color:var(--tx3);margin-bottom:10px;display:block"></i>
        <div class="fw6 mb1">Drop file here or click to browse</div>
        <div class="tx3c txs">JPG, PNG, PDF – max 10MB</div>
        <div v-if="proofFile" class="badge badge-ok mt2">{{ proofFile.name }}</div>
        <input
          ref="proofFile"
          type="file"
          accept=".jpg,.jpeg,.png,.pdf"
          style="display:none"
          @change="handleProofFile" />
      </div>
      <div class="al al-wa mt2">
        <i class="fas fa-exclamation-triangle"></i>
        Ensure the receipt shows the amount, date, and reference number clearly.
      </div>
      <template #footer>
        <button class="btn btn-g" @click="showUpload = false">Cancel</button>
        <button
          class="btn btn-p"
          @click="uploadProof"
          :disabled="!proofFile || uploading">
          <i v-if="uploading" class="fas fa-spinner fa-spin"></i>
          <i v-else class="fas fa-upload"></i>
          {{ uploading ? 'Uploading...' : 'Upload & Submit' }}
        </button>
      </template>
    </AppModal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { orgApi } from '../../api'
import { useToast } from 'vue-toastification'
import StatusBadge from '../../components/StatusBadge.vue'
import AppModal from '../../components/AppModal.vue'

const toast = useToast()
const events = ref([])
const loading = ref(true)
const search = ref('')
const showDetail = ref(false)
const showUpload = ref(false)
const selectedEvent = ref(null)
const proofFile = ref(null)
const uploading = ref(false)
const downloading = ref(null)

const filtered = computed(() => {
  const q = search.value.toLowerCase()
  return events.value.filter(e => !q || e.eventName?.toLowerCase().includes(q))
})

async function loadEvents() {
  loading.value = true
  try {
    const { data } = await orgApi.getMyEvents()
    events.value = data.data
  } catch {
    toast.error('Failed to load events')
  } finally {
    loading.value = false
  }
}

function viewEvent(ev) {
  selectedEvent.value = ev
  showDetail.value = true
}

function openUploadProof(ev) {
  selectedEvent.value = ev
  proofFile.value = null
  showUpload.value = true
}

function handleProofFile(e) {
  proofFile.value = e.target.files[0]
}

async function uploadProof() {
  if (!proofFile.value || !selectedEvent.value?.payment?.id) return
  uploading.value = true
  try {
    await orgApi.uploadProof(selectedEvent.value.payment.id, proofFile.value)
    const ev = events.value.find(e => e.id === selectedEvent.value.id)
    if (ev?.payment) ev.payment.status = 'UNDER_REVIEW'
    showUpload.value = false
    toast.success('Payment proof uploaded! Admin will review shortly.')
  } catch (e) {
    toast.error(e.response?.data?.message || 'Upload failed')
  } finally {
    uploading.value = false
  }
}

async function downloadMyCards(ev) {
  downloading.value = ev.id
  try {
    const response = await orgApi.downloadMyCards(ev.id)
    const url = window.URL.createObjectURL(
      new Blob([response.data], { type: 'application/zip' })
    )
    const a = document.createElement('a')
    a.href = url
    a.download = `invitations-${ev.eventName}.zip`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    toast.success('Cards ZIP downloaded!')
  } catch {
    toast.error('Download failed')
  } finally {
    downloading.value = null
  }
}

onMounted(loadEvents)
</script>

<style scoped>
.top-bar { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px; }
.search-box { display: flex; align-items: center; gap: 7px; background: var(--bg3); border: 1px solid var(--bd); border-radius: 8px; padding: 6px 12px; }
.search-box input { border: none; background: transparent; outline: none; font-size: 13px; color: var(--tx); width: 200px; }
.loading-box { padding: 40px; text-align: center; color: var(--tx3); }
.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; font-size: 13px; background: var(--bg3); border-radius: 8px; padding: 14px; }
.dl { display: block; font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: .4px; color: var(--tx3); margin-bottom: 3px; }
.desc-box { background: var(--bg3); border-radius: 8px; padding: 14px; font-size: 13px; color: var(--tx2); }
</style>