<template>
  <div>
    <div class="stat-grid">
      <StatCard icon="fas fa-check-circle" iconBg="#d1fae5" iconColor="#059669"
        :value="'$' + approvedTotal.toLocaleString()" label="Approved" />
      <StatCard icon="fas fa-search-dollar" iconBg="#e0f2fe" iconColor="#0284c7"
        :value="'$' + reviewTotal.toLocaleString()" label="Under Review" />
      <StatCard icon="fas fa-hourglass" iconBg="#fef3c7" iconColor="#d97706"
        :value="'$' + pendingTotal.toLocaleString()" label="Pending Upload" />
      <StatCard icon="fas fa-times-circle" iconBg="#fee2e2" iconColor="#dc2626"
        :value="'$' + rejectedTotal.toLocaleString()" label="Rejected" />
    </div>

    <div class="card">
      <div class="ch">
        <div class="ct">Payment Requests</div>
        <div style="display:flex;gap:8px">
          <select v-model="filterStatus" class="fs" style="width:150px">
            <option value="">All Statuses</option>
            <option value="PENDING">Pending</option>
            <option value="UNDER_REVIEW">Under Review</option>
            <option value="APPROVED">Approved</option>
            <option value="REJECTED">Rejected</option>
          </select>
        </div>
      </div>

      <div v-if="loading" class="loading-box"><i class="fas fa-spinner fa-spin"></i> Loading...</div>
      <div v-else class="tw">
        <table>
          <thead>
            <tr>
              <th>Invoice</th><th>Event</th><th>Organizer</th>
              <th>Amount</th><th>Proof</th><th>Status</th>
              <th>Date</th><th>Verified</th><th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in filtered" :key="p.id">
              <td><code>{{ p.invoiceNumber }}</code></td>
              <td><strong>{{ p.eventName }}</strong></td>
              <td>{{ p.organizerName }}</td>
              <td><strong>${{ p.amount }}</strong></td>
              <td>
                <span v-if="p.hasProof" class="badge badge-ok">
                  <i class="fas fa-paperclip"></i> Uploaded
                </span>
                <span v-else class="badge badge-wa">Pending</span>
              </td>
              <td><StatusBadge :status="p.status" /></td>
              <td class="tx3c txs">{{ p.createdAt?.substring(0,10) || '—' }}</td>
              <td class="tx3c txs">{{ p.verifiedAt?.substring(0,10) || '—' }}</td>
              <td>
                <div style="display:flex;gap:6px">
                  <!-- View proof always available -->
                  <button class="btn btn-g btn-sm" @click="viewProof(p)" title="View Proof">
                    <i class="fas fa-eye"></i>
                  </button>
                  <!-- Approve/Reject only when UNDER_REVIEW -->
                  <template v-if="p.status === 'UNDER_REVIEW'">
                    <button class="btn btn-ok btn-sm" @click="verify(p, 'APPROVED')" title="Approve">
                      <i class="fas fa-check"></i>
                    </button>
                    <button class="btn btn-er btn-sm" @click="openReject(p)" title="Reject">
                      <i class="fas fa-times"></i>
                    </button>
                  </template>
                  <!-- Show remarks icon if any -->
                  <button v-if="p.adminRemarks" class="btn btn-g btn-sm"
                    @click="viewRemarks(p)" title="Admin Remarks">
                    <i class="fas fa-comment-alt"></i>
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!filtered.length">
              <td colspan="9" style="text-align:center;color:var(--tx3);padding:24px">
                No payment requests
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- ── Proof Modal ── -->
    <AppModal v-model="showProof" title="Payment Proof" width="560px">
      <div v-if="proofItem">

        <!-- Payment info -->
        <div class="proof-info-box">
          <div class="proof-info-row">
            <span class="pir-label">Event</span>
            <span class="pir-val fw6">{{ proofItem.eventName }}</span>
          </div>
          <div class="proof-info-row">
            <span class="pir-label">Invoice</span>
            <code class="pir-val">{{ proofItem.invoiceNumber }}</code>
          </div>
          <div class="proof-info-row">
            <span class="pir-label">Amount</span>
            <span class="pir-val fw6" style="color:var(--p)">${{ proofItem.amount }}</span>
          </div>
          <div class="proof-info-row">
            <span class="pir-label">Organizer</span>
            <span class="pir-val">{{ proofItem.organizerName }}</span>
          </div>
          <div class="proof-info-row">
            <span class="pir-label">Status</span>
            <StatusBadge :status="proofItem.status" />
          </div>
          <div v-if="proofItem.verifiedAt" class="proof-info-row">
            <span class="pir-label">Verified At</span>
            <span class="pir-val">{{ proofItem.verifiedAt?.substring(0,19).replace('T',' ') }}</span>
          </div>
          <div v-if="proofItem.adminRemarks" class="proof-info-row">
            <span class="pir-label">Remarks</span>
            <span class="pir-val" style="color:#dc2626">{{ proofItem.adminRemarks }}</span>
          </div>
        </div>

        <!-- Proof file viewer -->
        <div v-if="proofItem.hasProof" class="proof-file-box mt2">
          <div class="proof-file-header">
            <i class="fas fa-paperclip"></i>
            <span>{{ proofItem.proofFileName }}</span>
            <a :href="proofFileUrl(proofItem)" target="_blank" class="btn btn-p btn-sm" style="margin-left:auto">
              <i class="fas fa-external-link-alt"></i> Open File
            </a>
            <a :href="proofFileUrl(proofItem)" download class="btn btn-g btn-sm">
              <i class="fas fa-download"></i> Download
            </a>
          </div>

          <!-- Image preview -->
          <div v-if="isImage(proofItem.proofFileName)" class="proof-img-wrap">
            <img :src="proofFileUrl(proofItem)" :alt="proofItem.proofFileName"
              class="proof-img" @error="imgError = true" />
          </div>

          <!-- PDF preview -->
          <div v-else-if="isPdf(proofItem.proofFileName)" class="proof-pdf-wrap">
            <iframe :src="proofFileUrl(proofItem)" class="proof-pdf" />
          </div>

          <!-- Other file type -->
          <div v-else class="proof-other">
            <i class="fas fa-file-alt"></i>
            <span>File uploaded ({{ proofItem.proofFileName }}). Click "Open File" to view.</span>
          </div>
        </div>

        <div v-else class="al al-wa mt2">
          <i class="fas fa-exclamation-triangle"></i>
          No proof uploaded yet by the organizer.
        </div>

        <div class="al al-in mt2">
          <i class="fas fa-info-circle"></i>
          After approval, organizer is notified via Email and SMS automatically.
        </div>
      </div>

      <template #footer>
        <button class="btn btn-g" @click="showProof = false">Close</button>
        <template v-if="proofItem?.status === 'UNDER_REVIEW'">
          <button class="btn btn-er" @click="openReject(proofItem); showProof = false">
            <i class="fas fa-times"></i> Reject
          </button>
          <button class="btn btn-ok" @click="verify(proofItem, 'APPROVED'); showProof = false"
            :disabled="saving">
            <i v-if="saving" class="fas fa-spinner fa-spin"></i>
            <i v-else class="fas fa-check"></i>
            Approve & Notify
          </button>
        </template>
      </template>
    </AppModal>

    <!-- ── Reject Modal ── -->
    <AppModal v-model="showReject" title="Reject Payment">
      <div class="al al-er mb2">
        <i class="fas fa-exclamation-triangle"></i>
        Organizer will be notified by email and SMS with the reason.
      </div>
      <div class="fg">
        <label class="fl req">Reason for Rejection</label>
        <textarea v-model="rejectReason" class="fta"
          placeholder="Please specify why the payment proof is insufficient..."></textarea>
      </div>
      <template #footer>
        <button class="btn btn-g" @click="showReject = false">Cancel</button>
        <button class="btn btn-er" @click="submitReject" :disabled="!rejectReason || saving">
          <i v-if="saving" class="fas fa-spinner fa-spin"></i>
          <i v-else class="fas fa-times"></i>
          Reject & Notify
        </button>
      </template>
    </AppModal>

    <!-- ── Remarks Modal ── -->
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
import { ref, computed, onMounted } from 'vue'
import { adminApi } from '../../api'
import { useToast } from 'vue-toastification'
import StatCard    from '../../components/StatCard.vue'
import StatusBadge from '../../components/StatusBadge.vue'
import AppModal    from '../../components/AppModal.vue'

const toast = useToast()

const payments       = ref([])
const loading        = ref(true)
const filterStatus   = ref('')
const showProof      = ref(false)
const showReject     = ref(false)
const showRemarks    = ref(false)
const proofItem      = ref(null)
const rejectReason   = ref('')
const selectedRemarks = ref('')
const saving         = ref(false)
const imgError       = ref(false)

const filtered = computed(() =>
  payments.value.filter(p => !filterStatus.value || p.status === filterStatus.value)
)

const approvedTotal = computed(() => sum('APPROVED'))
const reviewTotal   = computed(() => sum('UNDER_REVIEW'))
const pendingTotal  = computed(() => sum('PENDING'))
const rejectedTotal = computed(() => sum('REJECTED'))

function sum(status) {
  return payments.value.filter(p => p.status === status).reduce((a, p) => a + (p.amount || 0), 0)
}

async function loadPayments() {
  loading.value = true
  try {
    const { data } = await adminApi.getPayments()
    payments.value = data.data
  } catch { toast.error('Failed to load payments') }
  finally { loading.value = false }
}

function viewProof(p) {
  proofItem.value = p
  imgError.value  = false
  showProof.value = true
}

function openReject(p) {
  proofItem.value  = p
  rejectReason.value = ''
  showReject.value = true
}

function viewRemarks(p) {
  selectedRemarks.value = p.adminRemarks
  showRemarks.value = true
}

// Build URL to the uploaded proof file served by FileController
function proofFileUrl(p) {
  if (!p?.proofFileName) return ''
  const backendBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
  return `${backendBase}/uploads/payments/${encodeURIComponent(p.proofFileName)}`
}

function isImage(name) {
  return name && /\.(jpg|jpeg|png|gif|webp)$/i.test(name)
}

function isPdf(name) {
  return name && /\.pdf$/i.test(name)
}

async function verify(p, status, remarks = '') {
  saving.value = true
  try {
    const { data } = await adminApi.verifyPayment(p.id, { status, adminRemarks: remarks || null })
    // Replace full object so verifiedAt and all fields update
    const idx = payments.value.findIndex(x => x.id === p.id)
    if (idx >= 0) payments.value[idx] = { ...payments.value[idx], ...data.data }
    if (proofItem.value?.id === p.id) proofItem.value = payments.value[idx]
    toast.success(`Payment ${status === 'APPROVED' ? 'approved' : 'updated'}! Organizer notified.`)
    showProof.value = false
  } catch { toast.error('Failed to update payment') }
  finally { saving.value = false }
}

async function submitReject() {
  if (!rejectReason.value.trim()) return
  saving.value = true
  try {
    const { data } = await adminApi.verifyPayment(proofItem.value.id, {
      status: 'REJECTED',
      adminRemarks: rejectReason.value
    })
    const idx = payments.value.findIndex(x => x.id === proofItem.value.id)
    if (idx >= 0) payments.value[idx] = { ...payments.value[idx], ...data.data }
    showReject.value = false
    toast.info('Payment rejected. Organizer notified.')
  } catch { toast.error('Failed to reject payment') }
  finally { saving.value = false }
}

onMounted(loadPayments)
</script>

<style scoped>
.stat-grid { display:grid; grid-template-columns:repeat(4,1fr); gap:14px; margin-bottom:22px; }
.ch  { display:flex; align-items:center; justify-content:space-between; margin-bottom:16px; }
.ct  { font-size:15px; font-weight:600; }
.fw6 { font-weight:600; }
.loading-box { padding:40px; text-align:center; color:var(--tx3); }

/* proof info table */
.proof-info-box {
  background: var(--bg2, #f8fafc);
  border-radius: 10px;
  border: 1px solid var(--br, #e2e8f0);
  overflow: hidden;
}
.proof-info-row {
  display: flex;
  align-items: center;
  padding: 9px 16px;
  border-bottom: 1px solid var(--br, #e2e8f0);
  gap: 12px;
}
.proof-info-row:last-child { border-bottom: none; }
.pir-label { font-size:12px; font-weight:600; color:var(--tx3); width:90px; flex-shrink:0; }
.pir-val   { font-size:13px; color:var(--tx1); }

/* proof file area */
.proof-file-box {
  border: 1px solid var(--br, #e2e8f0);
  border-radius: 10px;
  overflow: hidden;
}
.proof-file-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: var(--bg2, #f8fafc);
  border-bottom: 1px solid var(--br, #e2e8f0);
  font-size: 13px;
  font-weight: 600;
}
.proof-img-wrap { padding: 12px; background:#fff; text-align:center; }
.proof-img {
  max-width: 100%;
  max-height: 340px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0,0,0,.1);
  object-fit: contain;
}
.proof-pdf-wrap { background:#fff; }
.proof-pdf { width:100%; height:340px; border:none; display:block; }
.proof-other {
  display:flex; align-items:center; gap:10px;
  padding:20px; color:var(--tx3); font-size:13px;
}
.proof-other i { font-size:24px; }

@media (max-width:900px) { .stat-grid { grid-template-columns:1fr 1fr; } }
</style>