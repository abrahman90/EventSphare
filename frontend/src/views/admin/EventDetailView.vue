<template>
  <div>
    <button class="btn btn-g btn-sm mb2" @click="$router.back()">
      <i class="fas fa-arrow-left"></i> Back to Events
    </button>
    <div v-if="loading" class="loading-box"><i class="fas fa-spinner fa-spin"></i> Loading...</div>
    <div v-else-if="event">
      <div class="event-header">
        <div>
          <h2>{{ event.eventName }}</h2>
          <div class="meta">
            <span><i class="fas fa-user"></i> {{ event.organizerName }}</span>
            <span><i class="fas fa-calendar"></i> {{ event.eventDate }}</span>
            <span><i class="fas fa-map-marker-alt"></i> {{ event.venue }}</span>
          </div>
        </div>
        <div style="display:flex;gap:8px;flex-wrap:wrap;align-items:center">
          <StatusBadge :status="event.status" />
          <button class="btn btn-p btn-sm" @click="showStatus = true">
            <i class="fas fa-edit"></i> Update Status
          </button>
          <button v-if="event.status === 'APPROVED' && event.payment?.status === 'APPROVED'"
            class="btn btn-ok btn-sm" @click="$router.push(`/admin/invitations/${event.id}`)">
            <i class="fas fa-magic"></i> Generate Cards
          </button>
        </div>
      </div>

      <div class="gr2 mt2">
        <div class="card">
          <div class="ct mb2">Event Information</div>
          <div class="info-grid">
            <div><span class="lbl">Category</span> {{ event.category?.replace('_',' ') }}</div>
            <div><span class="lbl">Capacity</span> {{ event.capacity }}</div>
            <div><span class="lbl">Start Time</span> {{ event.startTime }}</div>
            <div><span class="lbl">End Time</span> {{ event.endTime }}</div>
            <div><span class="lbl">Layout</span> {{ event.selectedLayout }}</div>
            <div><span class="lbl">Total Amount</span> <strong style="color:var(--p)">${{ event.totalAmount }}</strong></div>
            <div><span class="lbl">Contact</span> {{ event.contactInfo }}</div>
            <div><span class="lbl">Cards Generated</span> {{ event.generatedCards || 0 }}</div>
          </div>
          <div v-if="event.description" class="desc-box mt2">
            <div class="lbl mb1">Description</div>
            {{ event.description }}
          </div>
        </div>
        <div class="card">
          <div class="ct mb2">Payment Status</div>
          <div v-if="event.payment" class="pay-info">
            <div class="pay-row"><span>Invoice</span><code>{{ event.payment.invoiceNumber }}</code></div>
            <div class="pay-row"><span>Amount</span><strong>${{ event.payment.amount }}</strong></div>
            <div class="pay-row"><span>Status</span><StatusBadge :status="event.payment.status" /></div>
            <div class="pay-row"><span>Proof</span>
              <span v-if="event.payment.hasProof" class="badge badge-ok"><i class="fas fa-paperclip"></i> Uploaded</span>
              <span v-else class="badge badge-wa">Pending</span>
            </div>
          </div>
          <div v-else class="tx3c txs">No payment info</div>

          <div class="ct mt2 mb2">Invitation Card Preview</div>
          <div class="gc" :class="'inv-' + event.selectedLayout?.toLowerCase()">
            <div class="gc-ev">{{ event.eventName }}</div>
            <div class="gc-row"><i class="fas fa-map-marker-alt"></i> {{ event.venue }}</div>
            <div class="gc-row"><i class="fas fa-calendar"></i> {{ event.eventDate }}</div>
            <div class="gc-row"><i class="fas fa-clock"></i> {{ event.startTime }}</div>
            <div class="gc-hr"></div>
            <div class="gc-nm">[Participant Name]</div>
            <div class="gc-id">ID: {{ event.eventName?.substring(0,4).toUpperCase() }}-{{ event.id }}-XXXXXXXX</div>
          </div>
        </div>
      </div>
    </div>

    <AppModal v-model="showStatus" title="Update Event Status">
      <div class="fg">
        <label class="fl req">New Status</label>
        <select v-model="statusForm.status" class="fs">
          <option value="PENDING">Pending</option>
          <option value="APPROVED">Approved</option>
          <option value="REJECTED">Rejected</option>
          <option value="IN_PROGRESS">In Progress</option>
          <option value="COMPLETED">Completed</option>
        </select>
      </div>
      <div class="fg">
        <label class="fl">Admin Remarks</label>
        <textarea v-model="statusForm.adminRemarks" class="fta" placeholder="Optional remarks..."></textarea>
      </div>
      <div class="al al-in">
        <i class="fas fa-bell"></i> Organizer notified by Email & SMS automatically.
      </div>
      <template #footer>
        <button class="btn btn-g" @click="showStatus = false">Cancel</button>
        <button class="btn btn-p" @click="saveStatus" :disabled="saving">
          <i v-if="saving" class="fas fa-spinner fa-spin"></i>
          <i v-else class="fas fa-save"></i>
          {{ saving ? 'Saving...' : 'Save & Notify' }}
        </button>
      </template>
    </AppModal>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { adminApi } from '../../api'
import { useToast } from 'vue-toastification'
import StatusBadge from '../../components/StatusBadge.vue'
import AppModal from '../../components/AppModal.vue'

const route = useRoute()
const toast = useToast()
const event = ref(null)
const loading = ref(true)
const showStatus = ref(false)
const saving = ref(false)
const statusForm = ref({ status: '', adminRemarks: '' })

async function load() {
  loading.value = true
  try {
    const { data } = await adminApi.getEvent(route.params.id)
    event.value = data.data
    statusForm.value.status = data.data.status
    statusForm.value.adminRemarks = data.data.adminRemarks || ''
  } catch (e) {
    toast.error('Failed to load event')
  } finally {
    loading.value = false
  }
}

async function saveStatus() {
  saving.value = true
  try {
    const { data } = await adminApi.updateEventStatus(event.value.id, statusForm.value)
    event.value = data.data
    showStatus.value = false
    toast.success('Status updated! Organizer notified.')
  } catch (e) {
    toast.error('Failed')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.loading-box { padding: 60px; text-align: center; color: var(--tx3); }
.event-header { display: flex; align-items: flex-start; justify-content: space-between; flex-wrap: wrap; gap: 14px; margin-bottom: 4px; }
.event-header h2 { font-size: 22px; font-weight: 800; margin-bottom: 6px; }
.meta { display: flex; gap: 16px; flex-wrap: wrap; font-size: 13px; color: var(--tx2); }
.meta span { display: flex; align-items: center; gap: 6px; }
.gr2 { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }
.ct { font-size: 15px; font-weight: 600; }
.lbl { font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: .4px; color: var(--tx3); display: block; margin-bottom: 2px; }
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; font-size: 13px; }
.desc-box { background: var(--bg3); border-radius: 8px; padding: 14px; font-size: 13px; color: var(--tx2); }
.pay-row { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; border-bottom: 1px solid var(--bd); font-size: 13px; }
.pay-row:last-child { border-bottom: none; }
@media (max-width: 900px) { .gr2 { grid-template-columns: 1fr; } }
</style>
