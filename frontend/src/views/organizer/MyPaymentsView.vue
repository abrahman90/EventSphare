<template>
  <div>
    <div class="card">
      <div class="ch">
        <div class="ct">My Payment History</div>
      </div>
      <div v-if="loading" class="loading-box"><i class="fas fa-spinner fa-spin"></i> Loading...</div>
      <div v-else>
        <div v-if="!payments.length" class="empty-state">
          <i class="fas fa-receipt"></i>
          <p>No payments yet. Create an event to get started.</p>
          <router-link to="/organizer/events/create" class="btn btn-p mt2">
            <i class="fas fa-plus"></i> Create Event
          </router-link>
        </div>
        <div v-else class="tw">
          <table>
            <thead>
              <tr><th>Invoice</th><th>Event</th><th>Amount</th><th>Status</th><th>Proof</th><th>Date</th><th>Actions</th></tr>
            </thead>
            <tbody>
              <tr v-for="p in payments" :key="p.id">
                <td><code>{{ p.invoiceNumber }}</code></td>
                <td><strong>{{ p.eventName }}</strong></td>
                <td><strong style="color:var(--p)">${{ p.amount }}</strong></td>
                <td><StatusBadge :status="p.status" /></td>
                <td>
                  <span v-if="p.hasProof" class="badge badge-ok">
                    <i class="fas fa-paperclip"></i> {{ p.proofFileName || 'Uploaded' }}
                  </span>
                  <span v-else class="badge badge-wa">Not uploaded</span>
                </td>
                <td class="tx3c txs">{{ p.createdAt?.substring(0, 10) }}</td>
                <td>
                  <div style="display:flex;gap:6px">
                    <button v-if="p.status === 'PENDING' || p.status === 'REJECTED'"
                      class="btn btn-wa btn-sm" @click="openUpload(p)">
                      <i class="fas fa-upload"></i> Upload Proof
                    </button>
                    <button v-if="p.adminRemarks" class="btn btn-g btn-sm" @click="viewRemarks(p)">
                      <i class="fas fa-comment"></i>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Upload Proof Modal -->
    <AppModal v-model="showUpload" title="Upload Payment Proof">
      <div v-if="selected" class="al al-in mb2">
        <i class="fas fa-info-circle"></i>
        <div>
          <strong>Invoice:</strong> {{ selected.invoiceNumber }}<br>
          <strong>Amount Due:</strong> ${{ selected.amount }}<br>
          <strong>Event:</strong> {{ selected.eventName }}
        </div>
      </div>
      <div class="upz" @click="$refs.proofInput.click()" @dragover.prevent @drop.prevent="onDrop">
        <i class="fas fa-cloud-upload-alt" style="font-size:36px;color:var(--tx3);margin-bottom:10px;display:block"></i>
        <div class="fw6 mb1">Drop payment receipt here or click to browse</div>
        <div class="tx3c txs">JPG, PNG, PDF – max 10MB</div>
        <div v-if="proofFile" class="badge badge-ok mt2">
          <i class="fas fa-check"></i> {{ proofFile.name }}
        </div>
        <input ref="proofInput" type="file" style="display:none" accept=".jpg,.jpeg,.png,.pdf" @change="onFileChange" />
      </div>
      <div class="al al-wa mt2">
        <i class="fas fa-exclamation-triangle"></i>
        Ensure the receipt clearly shows: amount, date, bank reference, and transaction number.
      </div>
      <template #footer>
        <button class="btn btn-g" @click="showUpload = false">Cancel</button>
        <button class="btn btn-p" @click="submitProof" :disabled="!proofFile || uploading">
          <i v-if="uploading" class="fas fa-spinner fa-spin"></i>
          <i v-else class="fas fa-upload"></i>
          {{ uploading ? 'Uploading...' : 'Upload & Submit for Review' }}
        </button>
      </template>
    </AppModal>

    <!-- Remarks Modal -->
    <AppModal v-model="showRemarks" title="Admin Remarks">
      <div class="al al-in">
        <i class="fas fa-comment-alt"></i>
        <div>{{ selectedRemarks }}</div>
      </div>
      <template #footer>
        <button class="btn btn-g" @click="showRemarks = false">Close</button>
      </template>
    </AppModal>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { orgApi } from '../../api'
import { useToast } from 'vue-toastification'
import StatusBadge from '../../components/StatusBadge.vue'
import AppModal from '../../components/AppModal.vue'

const toast = useToast()
const payments = ref([])
const loading = ref(true)
const showUpload = ref(false)
const showRemarks = ref(false)
const selected = ref(null)
const selectedRemarks = ref('')
const proofFile = ref(null)
const uploading = ref(false)

async function loadPayments() {
  loading.value = true
  try {
    const { data } = await orgApi.getMyEvents()
    const events = data.data
    const payList = []
    for (const ev of events) {
      if (ev.payment) {
        payList.push({ ...ev.payment, eventName: ev.eventName })
      }
    }
    payments.value = payList
  } catch (e) {
    toast.error('Failed to load payments')
  } finally {
    loading.value = false
  }
}

function openUpload(p) {
  selected.value = p
  proofFile.value = null
  showUpload.value = true
}

function viewRemarks(p) {
  selectedRemarks.value = p.adminRemarks
  showRemarks.value = true
}

function onFileChange(e) {
  proofFile.value = e.target.files[0]
}

function onDrop(e) {
  proofFile.value = e.dataTransfer.files[0]
}

async function submitProof() {
  if (!proofFile.value || !selected.value?.id) return
  uploading.value = true
  try {
    await orgApi.uploadProof(selected.value.id, proofFile.value)
    const p = payments.value.find(x => x.id === selected.value.id)
    if (p) {
      p.status = 'UNDER_REVIEW'
      p.hasProof = true
      p.proofFileName = proofFile.value.name
    }
    showUpload.value = false
    toast.success('Payment proof uploaded! Admin will review and notify you via email & SMS.')
  } catch (e) {
    toast.error(e.response?.data?.message || 'Upload failed')
  } finally {
    uploading.value = false
  }
}

onMounted(loadPayments)
</script>

<style scoped>
.ch { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.ct { font-size: 15px; font-weight: 600; }
.loading-box { padding: 40px; text-align: center; color: var(--tx3); }
.empty-state { text-align: center; padding: 50px; color: var(--tx3); }
.empty-state i { font-size: 48px; margin-bottom: 12px; display: block; }
.fw6 { font-weight: 600; }
</style>
