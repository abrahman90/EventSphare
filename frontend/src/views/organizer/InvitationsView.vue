<template>
  <div>
    <!-- Event banner -->
    <div v-if="event" class="event-banner" :class="'inv-' + event.selectedLayout?.toLowerCase()">
      <div>
        <div style="font-size:20px;font-weight:800;text-shadow:0 2px 4px rgba(0,0,0,.2)">{{ event.eventName }}</div>
        <div style="font-size:13px;opacity:.9;margin-top:4px">
          <i class="fas fa-map-marker-alt"></i> {{ event.venue }} &nbsp;|&nbsp;
          <i class="fas fa-calendar"></i> {{ event.eventDate }}
        </div>
      </div>
      <div style="text-align:right">
        <div style="font-size:11px;font-weight:700;background:rgba(255,255,255,.2);padding:3px 10px;border-radius:20px;display:inline-block;margin-bottom:8px">
          {{ event.selectedLayout }} LAYOUT
        </div>
        <div><StatusBadge :status="event.status" /></div>
      </div>
    </div>

    <!-- Tabs -->
    <div class="tabs mt2 mb2">
      <button v-for="tab in tabs" :key="tab" class="tab-btn"
        :class="{ active: activeTab === tab }" @click="activeTab = tab">
        {{ tab }}
      </button>
    </div>

    <!-- ── Cards Tab ── -->
    <div v-if="activeTab === 'Cards'">
      <div v-if="cards.length === 0" class="al al-wa">
        <i class="fas fa-exclamation-triangle"></i>
        No invitation cards have been generated yet. The admin will generate cards after payment is approved.
      </div>
      <template v-else>
        <div class="ch">
          <div style="display:flex;gap:10px;flex-wrap:wrap">
            <div class="search-box">
              <i class="fas fa-search"></i>
              <input v-model="search" placeholder="Search participants..." />
            </div>
          </div>
          <div style="display:flex;gap:8px;flex-wrap:wrap">
            <button class="btn btn-ok btn-sm" @click="bulkSend('email')" :disabled="sending">
              <i class="fas fa-envelope"></i> Email All
            </button>
            <button class="btn btn-ac btn-sm" @click="bulkSend('sms')" :disabled="sending">
              <i class="fas fa-sms"></i> SMS All
            </button>
            <button class="btn btn-p btn-sm" @click="bulkSend('both')" :disabled="sending">
              <i class="fas fa-paper-plane"></i> Send All
            </button>
          </div>
        </div>

        <div class="card">
          <div class="tw">
            <table>
              <thead>
                <tr>
                  <th><input type="checkbox" @change="toggleAll" /></th>
                  <th>ID</th>
                  <th>Participant</th>
                  <th>Email</th>
                  <th>Phone</th>
                  <th>Email Status</th>
                  <th>SMS Status</th>
                  <th>QR Scanned</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="c in filteredCards" :key="c.id">
                  <td><input type="checkbox" v-model="selected" :value="c.id" /></td>
                  <td><code style="font-size:10px">{{ c.invitationId }}</code></td>
                  <td>
                    <strong>{{ c.participantName }}</strong>
                    <span v-if="c.participantEmail || c.participantPhone"
                      class="badge badge-ok" style="margin-left:4px;font-size:9px">
                      <i class="fas fa-check"></i> Details Added
                    </span>
                  </td>
                  <td>{{ c.participantEmail || '—' }}</td>
                  <td>{{ c.participantPhone || '—' }}</td>
                  <td><StatusBadge :status="c.emailStatus" /></td>
                  <td><StatusBadge :status="c.smsStatus" /></td>
                  <td>
                    <span class="badge" :class="c.qrScanned ? 'badge-ok' : 'badge-gray'">
                      {{ c.qrScanned ? 'Scanned' : 'Not yet' }}
                    </span>
                  </td>
                  <td>
                    <div style="display:flex;gap:5px;flex-wrap:wrap">
                      <button class="btn btn-p btn-sm" @click="openEditParticipant(c)"
                        :title="c.participantEmail || c.participantPhone ? 'Edit participant details' : 'Add participant details'">
                        <i class="fas fa-user-edit"></i>
                        {{ c.participantEmail || c.participantPhone ? 'Edit' : 'Add' }}
                      </button>
                      <button class="btn btn-ok btn-sm"
                        :disabled="!c.participantEmail || sending"
                        @click="sendOne(c, true, false)"
                        :title="c.participantEmail ? 'Send Email' : 'Add email first'">
                        <i class="fas fa-envelope"></i>
                      </button>
                      <button class="btn btn-ac btn-sm"
                        :disabled="!c.participantPhone || sending"
                        @click="sendOne(c, false, true)"
                        :title="c.participantPhone ? 'Send SMS' : 'Add phone first'">
                        <i class="fas fa-sms"></i>
                      </button>
                      <a v-if="c.cardPdfPath"
                        :href="'http://localhost:8082/' + c.cardPdfPath"
                        target="_blank"
                        class="btn btn-g btn-sm"
                        title="Download PDF">
                        <i class="fas fa-download"></i>
                      </a>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </template>
    </div>

    <!-- ── Send Single Tab ── -->
    <div v-if="activeTab === 'Send Single'">
      <div class="gr2">
        <div class="card">
          <div class="ct mb2">Send Card to Single Participant</div>
          <div class="al al-in mb2">
            <i class="fas fa-info-circle"></i>
            Select a card, add participant details and send their invitation.
          </div>
          <div class="fg">
            <label class="fl req">Select Card</label>
            <select v-model="singleForm.cardId" class="fs">
              <option value="">-- Select a card --</option>
              <option v-for="c in cards" :key="c.id" :value="c.id">
                {{ c.participantName }} ({{ c.invitationId }})
                {{ c.participantEmail ? '✓ ' + c.participantEmail : '' }}
              </option>
            </select>
          </div>
          <div class="fg">
            <label class="fl">Send via</label>
            <div style="display:flex;gap:16px;margin-top:4px">
              <label class="fc g8"><input type="checkbox" v-model="singleForm.sendEmail" /> Email</label>
              <label class="fc g8"><input type="checkbox" v-model="singleForm.sendSms" /> SMS</label>
            </div>
          </div>
          <button class="btn btn-p btn-full" @click="submitSingle"
            :disabled="!singleForm.cardId || sending">
            <i v-if="sending" class="fas fa-spinner fa-spin"></i>
            <i v-else class="fas fa-paper-plane"></i>
            {{ sending ? 'Sending...' : 'Send Invitation' }}
          </button>
        </div>
        <div class="card">
          <div class="ct mb2">Recently Sent</div>
          <div class="tw" style="max-height:400px;overflow-y:auto">
            <table>
              <thead>
                <tr><th>Participant</th><th>Channel</th><th>Status</th><th>Time</th></tr>
              </thead>
              <tbody>
                <tr v-for="log in sendLog" :key="log.id">
                  <td>{{ log.name }}</td>
                  <td>
                    <span class="badge" :class="log.channel === 'email' ? 'badge-in' : 'badge-ok'">
                      {{ log.channel }}
                    </span>
                  </td>
                  <td><span class="badge badge-ok">Sent</span></td>
                  <td class="tx3c txs">{{ log.time }}</td>
                </tr>
                <tr v-if="!sendLog.length">
                  <td colspan="4" style="text-align:center;color:var(--tx3)">No sends yet</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- ── Excel Bulk Tab ── -->
    <div v-if="activeTab === 'Excel Bulk'">
      <div class="gr2">
        <div class="card">
          <div class="ct mb2">Bulk Send via Excel</div>

          <!-- Download Template -->
          <div class="al al-ok mb2" style="justify-content:space-between;align-items:center;flex-wrap:wrap;gap:10px">
            <div>
              <i class="fas fa-file-excel"></i>
              <strong> Step 1:</strong> Download the template, fill in participant details, then upload.
            </div>
            <button class="btn btn-ok btn-sm" @click="downloadTemplate"
              :disabled="downloadingTemplate" style="white-space:nowrap">
              <i v-if="downloadingTemplate" class="fas fa-spinner fa-spin"></i>
              <i v-else class="fas fa-file-download"></i>
              {{ downloadingTemplate ? 'Downloading...' : 'Download Template' }}
            </button>
          </div>

          <div class="al al-in mb2">
            <i class="fas fa-info-circle"></i>
            <div>
              <strong>Step 2:</strong> Upload your filled Excel file.<br><br>
              <strong>Excel format:</strong><br>
              Column A: Participant Name <span style="color:red">*</span><br>
              Column B: Email Address<br>
              Column C: Phone Number
            </div>
          </div>

          <div class="upz" @click="$refs.xlsxFile.click()"
            @dragover.prevent @drop.prevent="handleDrop">
            <i class="fas fa-file-excel"
              style="font-size:40px;color:#059669;margin-bottom:12px;display:block"></i>
            <div class="fw6 mb1">Drop Excel file here or click to browse</div>
            <div class="tx3c txs">Supports .xlsx, .xls</div>
            <div v-if="excelFile" class="badge badge-ok mt2">{{ excelFile.name }}</div>
            <input ref="xlsxFile" type="file" accept=".xlsx,.xls"
              style="display:none" @change="handleExcelSelect" />
          </div>

          <div class="fg mt2">
            <label class="fl">Send via</label>
            <div style="display:flex;gap:16px;margin-top:4px">
              <label class="fc g8"><input type="checkbox" v-model="bulkEmail" /> Email</label>
              <label class="fc g8"><input type="checkbox" v-model="bulkSMS" /> SMS</label>
            </div>
          </div>
          <button class="btn btn-p btn-full mt1" @click="submitExcel"
            :disabled="!excelFile || sendingExcel">
            <i v-if="sendingExcel" class="fas fa-spinner fa-spin"></i>
            <i v-else class="fas fa-paper-plane"></i>
            {{ sendingExcel ? 'Sending...' : 'Send All from Excel' }}
          </button>
        </div>

        <div class="card">
          <div class="ct mb2">How It Works</div>
          <ol class="how-list">
            <li>Click <strong>"Download Template"</strong> to get the Excel file</li>
            <li>Open and fill in participant names, emails and phone numbers</li>
            <li>Save and upload the file using the form on the left</li>
            <li>Select delivery channels (email and/or SMS)</li>
            <li>Click "Send All from Excel"</li>
            <li>The system matches each row to a card and sends automatically</li>
            <li>Track delivery status in the Cards tab</li>
          </ol>
          <div class="al al-ok mt2">
            <i class="fas fa-check-circle"></i>
            Each participant receives their <strong>unique QR-coded invitation card</strong> as a PDF attachment.
          </div>

          <!-- Template preview -->
          <div class="mt2">
            <div class="ct mb1" style="font-size:13px">Template Preview</div>
            <table class="template-preview">
              <thead>
                <tr>
                  <th>A: Participant Name *</th>
                  <th>B: Email Address</th>
                  <th>C: Phone Number</th>
                </tr>
              </thead>
              <tbody>
                <tr><td>Alice Mwangi</td><td>alice@example.com</td><td>+255700111001</td></tr>
                <tr><td>John Doe</td><td>john@example.com</td><td>+255700111002</td></tr>
                <tr style="color:var(--tx3);font-style:italic">
                  <td>Add more rows...</td><td></td><td></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- ── Statistics Tab ── -->
    <div v-if="activeTab === 'Statistics'">
      <div class="stat-grid">
        <StatCard icon="fas fa-layer-group" iconBg="#ede9fe" iconColor="#7c3aed"
          :value="stats.total" label="Generated" />
        <StatCard icon="fas fa-paper-plane" iconBg="#e0f2fe" iconColor="#0284c7"
          :value="stats.emailSent" label="Email Sent" />
        <StatCard icon="fas fa-sms" iconBg="#d1fae5" iconColor="#059669"
          :value="stats.smsSent" label="SMS Sent" />
        <StatCard icon="fas fa-eye" iconBg="#e8f0fe" iconColor="#1a56db"
          :value="stats.opened" label="Opened" />
        <StatCard icon="fas fa-qrcode" iconBg="#f0fdf4" iconColor="#166534"
          :value="stats.scanned" label="QR Scanned" />
        <StatCard icon="fas fa-clock" iconBg="#fef3c7" iconColor="#d97706"
          :value="stats.pending" label="Pending" />
      </div>
    </div>

    <!-- ── Edit/Add Participant Modal ── -->
    <AppModal v-model="showEditParticipant" title="Add Participant Details" width="480px">
      <div v-if="editCard">
        <div class="al al-in mb2">
          <i class="fas fa-info-circle"></i>
          Card ID: <strong>{{ editCard.invitationId }}</strong> —
          Fill in the participant's details to enable sending this card.
        </div>
        <div class="fg">
          <label class="fl req">Participant Name</label>
          <input v-model="editForm.name" class="fi" placeholder="e.g. Alice Mwangi" />
        </div>
        <div class="fg">
          <label class="fl">Email Address</label>
          <input v-model="editForm.email" class="fi" type="email" placeholder="alice@example.com" />
        </div>
        <div class="fg">
          <label class="fl">Phone Number</label>
          <input v-model="editForm.phone" class="fi" placeholder="+255 700 111 001" />
        </div>
        <div class="al al-wa mt1" v-if="!editForm.email && !editForm.phone">
          <i class="fas fa-exclamation-triangle"></i>
          Add at least an email or phone number to be able to send this card.
        </div>
        <div class="fg mt1">
          <label class="fl">Send card immediately after saving?</label>
          <div style="display:flex;gap:16px;margin-top:6px">
            <label class="fc g8">
              <input type="checkbox" v-model="editForm.sendEmail"
                :disabled="!editForm.email" /> Email
            </label>
            <label class="fc g8">
              <input type="checkbox" v-model="editForm.sendSms"
                :disabled="!editForm.phone" /> SMS
            </label>
          </div>
        </div>
      </div>
      <template #footer>
        <button class="btn btn-g" @click="showEditParticipant = false">Cancel</button>
        <button class="btn btn-p" @click="saveParticipant"
          :disabled="!editForm.name || savingParticipant">
          <i v-if="savingParticipant" class="fas fa-spinner fa-spin"></i>
          <i v-else class="fas fa-save"></i>
          Save & {{ editForm.sendEmail || editForm.sendSms ? 'Send' : 'Update' }}
        </button>
      </template>
    </AppModal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { orgApi } from '../../api'
import { useToast } from 'vue-toastification'
import StatusBadge from '../../components/StatusBadge.vue'
import StatCard from '../../components/StatCard.vue'
import AppModal from '../../components/AppModal.vue'

const route = useRoute()
const toast = useToast()
const eventId = Number(route.params.eventId)

// ── State ──────────────────────────────────────────────────────────────────
const event = ref(null)
const cards = ref([])
const stats = ref({ total: 0, emailSent: 0, smsSent: 0, opened: 0, scanned: 0, pending: 0 })
const activeTab = ref('Cards')
const tabs = ['Cards', 'Send Single', 'Excel Bulk', 'Statistics']
const search = ref('')
const selected = ref([])
const sending = ref(false)
const sendingExcel = ref(false)
const excelFile = ref(null)
const bulkEmail = ref(true)
const bulkSMS = ref(false)
const sendLog = ref([])
const downloadingTemplate = ref(false)   // ← required for template download button

// ── Edit participant ───────────────────────────────────────────────────────
const showEditParticipant = ref(false)
const editCard = ref(null)
const savingParticipant = ref(false)
const editForm = ref({
  name: '', email: '', phone: '',
  sendEmail: false, sendSms: false,
})

// ── Single send form ───────────────────────────────────────────────────────
const singleForm = ref({
  name: '', email: '', phone: '',
  cardId: '', sendEmail: true, sendSms: false
})

// ── Computed ───────────────────────────────────────────────────────────────
const filteredCards = computed(() => {
  const q = search.value.toLowerCase()
  return cards.value.filter(c =>
    !q ||
    c.participantName?.toLowerCase().includes(q) ||
    c.invitationId?.toLowerCase().includes(q) ||
    c.participantEmail?.toLowerCase().includes(q)
  )
})

// ── Helpers ────────────────────────────────────────────────────────────────
function toggleAll(e) {
  selected.value = e.target.checked ? cards.value.map(c => c.id) : []
}

// ── Load data ──────────────────────────────────────────────────────────────
async function loadData() {
  try {
    const [evRes, cardsRes, statsRes] = await Promise.all([
      orgApi.getEvent(eventId),
      orgApi.getMyCards(eventId),
      orgApi.getStats(eventId),
    ])
    event.value = evRes.data.data
    cards.value = cardsRes.data.data
    stats.value = statsRes.data.data
  } catch {
    toast.error('Failed to load invitation data')
  }
}

// ── Download Excel template ────────────────────────────────────────────────
async function downloadTemplate() {
  downloadingTemplate.value = true
  try {
    const response = await orgApi.downloadExcelTemplate()
    const url = window.URL.createObjectURL(
      new Blob([response.data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      })
    )
    const a = document.createElement('a')
    a.href = url
    a.download = 'participants-template.xlsx'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    toast.success('Template downloaded!')
  } catch {
    toast.error('Failed to download template')
  } finally {
    downloadingTemplate.value = false
  }
}

// ── Edit participant ───────────────────────────────────────────────────────
function openEditParticipant(card) {
  editCard.value = card
  editForm.value = {
    name: card.participantName || '',
    email: card.participantEmail || '',
    phone: card.participantPhone || '',
    sendEmail: false,
    sendSms: false,
  }
  showEditParticipant.value = true
}

async function saveParticipant() {
  if (!editForm.value.name) return
  savingParticipant.value = true
  try {
    await orgApi.updateCardParticipant(editCard.value.id, {
      participantName: editForm.value.name,
      participantEmail: editForm.value.email || null,
      participantPhone: editForm.value.phone || null,
    })
    const idx = cards.value.findIndex(c => c.id === editCard.value.id)
    if (idx >= 0) {
      cards.value[idx].participantName = editForm.value.name
      cards.value[idx].participantEmail = editForm.value.email || null
      cards.value[idx].participantPhone = editForm.value.phone || null
    }
    toast.success('Participant details saved!')
    if (editForm.value.sendEmail || editForm.value.sendSms) {
      await orgApi.sendSingle({
        cardId: editCard.value.id,
        sendEmail: editForm.value.sendEmail,
        sendSms: editForm.value.sendSms,
      })
      if (idx >= 0) {
        if (editForm.value.sendEmail) cards.value[idx].emailStatus = 'SENT'
        if (editForm.value.sendSms) cards.value[idx].smsStatus = 'SENT'
      }
      sendLog.value.unshift({
        id: Date.now(), name: editForm.value.name,
        channel: editForm.value.sendEmail ? 'email' : 'sms',
        time: new Date().toLocaleTimeString()
      })
      toast.success(`Card sent to ${editForm.value.name}!`)
    }
    showEditParticipant.value = false
  } catch (e) {
    toast.error(e.response?.data?.message || 'Failed to save participant details')
  } finally {
    savingParticipant.value = false
  }
}

// ── Send functions ─────────────────────────────────────────────────────────
async function sendOne(card, email, sms) {
  sending.value = true
  try {
    await orgApi.sendSingle({ cardId: card.id, sendEmail: email, sendSms: sms })
    if (email) card.emailStatus = 'SENT'
    if (sms) card.smsStatus = 'SENT'
    sendLog.value.unshift({
      id: Date.now(), name: card.participantName,
      channel: email ? 'email' : 'sms',
      time: new Date().toLocaleTimeString()
    })
    toast.success(`${email ? 'Email' : 'SMS'} sent to ${card.participantName}!`)
  } catch { toast.error('Send failed') } finally { sending.value = false }
}

async function bulkSend(mode) {
  sending.value = true
  const ids = selected.value.length > 0 ? selected.value : undefined
  try {
    const { data } = await orgApi.sendBulk({
      eventId, cardIds: ids,
      sendEmail: mode === 'email' || mode === 'both',
      sendSms: mode === 'sms' || mode === 'both',
    })
    toast.success(`Bulk send complete! Email: ${data.data.emailSent}, SMS: ${data.data.smsSent}`)
    await loadData()
  } catch { toast.error('Bulk send failed') } finally { sending.value = false }
}

async function submitSingle() {
  if (!singleForm.value.cardId) return
  sending.value = true
  try {
    await orgApi.sendSingle({
      cardId: singleForm.value.cardId,
      sendEmail: singleForm.value.sendEmail,
      sendSms: singleForm.value.sendSms,
    })
    sendLog.value.unshift({
      id: Date.now(),
      name: singleForm.value.name || 'Participant',
      channel: singleForm.value.sendEmail ? 'email' : 'sms',
      time: new Date().toLocaleTimeString()
    })
    toast.success('Invitation sent successfully!')
    singleForm.value = { name: '', email: '', phone: '', cardId: '', sendEmail: true, sendSms: false }
  } catch { toast.error('Send failed') } finally { sending.value = false }
}

// ── Excel ──────────────────────────────────────────────────────────────────
function handleExcelSelect(e) { excelFile.value = e.target.files[0] }
function handleDrop(e) { excelFile.value = e.dataTransfer.files[0] }

async function submitExcel() {
  if (!excelFile.value) return
  sendingExcel.value = true
  try {
    const { data } = await orgApi.sendFromExcel(
      eventId, excelFile.value, bulkEmail.value, bulkSMS.value)
    toast.success(`Excel bulk send complete! Sent to ${data.data.emailSent + data.data.smsSent} participants.`)
    excelFile.value = null
    await loadData()
  } catch (e) {
    toast.error(e.response?.data?.message || 'Excel send failed')
  } finally { sendingExcel.value = false }
}

onMounted(loadData)
</script>

<style scoped>
.event-banner { border-radius: var(--r2); padding: 20px 24px; color: #fff; display: flex; align-items: flex-start; justify-content: space-between; }
.tabs { display: flex; gap: 4px; background: var(--bg3); border-radius: 10px; padding: 4px; width: fit-content; flex-wrap: wrap; }
.tab-btn { padding: 7px 16px; border-radius: 7px; font-size: 13px; font-weight: 500; color: var(--tx2); cursor: pointer; border: none; background: transparent; }
.tab-btn.active { background: var(--bg2); color: var(--p); box-shadow: var(--sh); }
.tab-btn:hover:not(.active) { color: var(--tx); }
.ch { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; flex-wrap: wrap; gap: 10px; }
.ct { font-size: 15px; font-weight: 600; }
.search-box { display: flex; align-items: center; gap: 7px; background: var(--bg3); border: 1px solid var(--bd); border-radius: 8px; padding: 6px 12px; }
.search-box input { border: none; background: transparent; outline: none; font-size: 13px; color: var(--tx); width: 180px; }
.gr2 { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }
.stat-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px,1fr)); gap: 14px; }
.how-list { padding-left: 18px; display: flex; flex-direction: column; gap: 10px; }
.how-list li { font-size: 13px; color: var(--tx2); }
.fc { display: flex; align-items: center; }
.g8 { gap: 8px; }
.template-preview { width: 100%; border-collapse: collapse; font-size: 12px; margin-top: 8px; }
.template-preview th { background: #1e3a5f; color: #fff; padding: 6px 10px; text-align: left; font-weight: 600; }
.template-preview td { padding: 5px 10px; border-bottom: 1px solid var(--bd); color: var(--tx2); }
.template-preview tr:nth-child(even) td { background: var(--bg3); }
@media (max-width: 900px) { .gr2 { grid-template-columns: 1fr; } }
</style>