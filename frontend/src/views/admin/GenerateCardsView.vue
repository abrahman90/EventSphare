<template>
  <div>
    <div class="back-row">
      <button class="btn btn-g btn-sm" @click="$router.push('/admin/invitations')">
        <i class="fas fa-arrow-left"></i> Back
      </button>
      <h2 v-if="event">{{ event.eventName }} – Generate Cards</h2>
    </div>

    <div v-if="loadingEvent" class="loading-box">
      <i class="fas fa-spinner fa-spin"></i> Loading event...
    </div>

    <template v-else-if="event">
      <!-- Event info banner -->
      <div class="event-banner" :class="'inv-' + event.selectedLayout?.toLowerCase()">
        <div>
          <div class="ev-name">{{ event.eventName }}</div>
          <div class="ev-info"><i class="fas fa-map-marker-alt"></i> {{ event.venue }}</div>
          <div class="ev-info"><i class="fas fa-calendar"></i> {{ event.eventDate }}</div>
          <div class="ev-info"><i class="fas fa-clock"></i> {{ event.startTime }} – {{ event.endTime }}</div>
        </div>
        <div style="text-align:right">
          <div class="ev-layout-tag">{{ event.selectedLayout }} LAYOUT</div>
          <StatusBadge :status="event.status" />
        </div>
      </div>

      <!-- Payment not approved warning -->
      <div v-if="event.payment?.status !== 'APPROVED'" class="al al-wa mt2">
        <i class="fas fa-exclamation-triangle"></i>
        Payment must be <strong>Approved</strong> before generating cards.
        Current payment status: <strong>{{ event.payment?.status || 'N/A' }}</strong>
      </div>

      <div v-else class="gr2 mt2">

        <!-- ── LEFT: Add Participants ── -->
        <div class="card">
          <div class="ch">
            <div class="ct">Add Participants</div>
            <button class="btn btn-g btn-sm" @click="showExcel = true">
              <i class="fas fa-file-excel"></i> Import Excel
            </button>
          </div>

          <div class="al al-in mb2">
            <i class="fas fa-info-circle"></i>
            Add participants one by one or import from Excel. Each gets a unique card with QR code.
          </div>

          <div class="g2 mb1">
            <div class="fg">
              <label class="fl req">Participant Name</label>
              <input v-model="newP.participantName" class="fi" placeholder="Alice Mwangi" />
            </div>
            <div class="fg">
              <label class="fl">Email</label>
              <input v-model="newP.email" class="fi" type="email" placeholder="alice@example.com" />
            </div>
          </div>
          <div class="fg mb2">
            <label class="fl">Phone Number</label>
            <input v-model="newP.phone" class="fi" placeholder="+255 700 111 001" />
          </div>

          <div style="display:flex;gap:8px">
            <button class="btn btn-p btn-sm" @click="addParticipant" :disabled="!newP.participantName">
              <i class="fas fa-plus"></i> Add Participant
            </button>
            <button class="btn btn-g btn-sm" @click="clearList" :disabled="!participants.length">
              <i class="fas fa-trash"></i> Clear All
            </button>
          </div>

          <!-- Participants list -->
          <div v-if="participants.length" class="part-list mt2">
            <div class="part-list-head">{{ participants.length }} participant(s) ready</div>
            <div class="tw" style="max-height:260px;overflow-y:auto">
              <table>
                <thead><tr><th>#</th><th>Name</th><th>Email</th><th>Phone</th><th></th></tr></thead>
                <tbody>
                  <tr v-for="(p, i) in participants" :key="i">
                    <td>{{ i + 1 }}</td>
                    <td>{{ p.participantName }}</td>
                    <td>{{ p.email || '—' }}</td>
                    <td>{{ p.phone || '—' }}</td>
                    <td>
                      <button class="btn btn-er btn-sm" @click="participants.splice(i,1)">
                        <i class="fas fa-trash"></i>
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <button class="btn btn-ok btn-full mt2 btn-lg"
            @click="generateCards"
            :disabled="!participants.length || generating">
            <i v-if="generating" class="fas fa-spinner fa-spin"></i>
            <i v-else class="fas fa-magic"></i>
            {{ generating ? 'Generating...' : `Generate ${participants.length} Cards` }}
          </button>
        </div>

        <!-- ── RIGHT: Preview + Generated Cards ── -->
        <div>

          <!-- Card Preview -->
          <div class="card mb2">
            <div class="ch"><div class="ct">Card Preview</div></div>

            <div class="inv-card-preview" :class="'inv-' + (event.selectedLayout?.toLowerCase() || 'classic')">
              <!-- Left section -->
              <div class="icp-left">
                <div class="icp-brand">✦ EventSphere</div>
                <div class="icp-title">{{ event.eventName }}</div>
                <div class="icp-divider"></div>
                <div class="icp-row"><i class="fas fa-map-marker-alt"></i> {{ event.venue }}</div>
                <div class="icp-row"><i class="fas fa-calendar-alt"></i> {{ event.eventDate }}</div>
                <div class="icp-row"><i class="fas fa-clock"></i> {{ event.startTime }}</div>
                <div v-if="event.contactInfo" class="icp-row"><i class="fas fa-phone"></i> {{ event.contactInfo }}</div>
                <div class="icp-divider" style="margin-top:auto"></div>
                <div class="icp-participant">{{ participants[0]?.participantName || '[Participant Name]' }}</div>
                <div class="icp-id">ID: {{ event.eventName?.substring(0,6).toUpperCase() }}-{{ event.id }}-XXXXXXXX</div>
              </div>

              <!-- Right section: QR placeholder -->
              <div class="icp-right">
                <div class="icp-inv-label">INVITATION</div>
                <div class="icp-qr-box">
                  <!-- SVG mock QR that looks like a real one -->
                  <svg viewBox="0 0 21 21" class="mock-qr" xmlns="http://www.w3.org/2000/svg">
                    <!-- top-left finder -->
                    <rect x="1" y="1" width="7" height="7" fill="none" stroke="currentColor" stroke-width="1"/>
                    <rect x="2.5" y="2.5" width="4" height="4" fill="currentColor"/>
                    <!-- top-right finder -->
                    <rect x="13" y="1" width="7" height="7" fill="none" stroke="currentColor" stroke-width="1"/>
                    <rect x="14.5" y="2.5" width="4" height="4" fill="currentColor"/>
                    <!-- bottom-left finder -->
                    <rect x="1" y="13" width="7" height="7" fill="none" stroke="currentColor" stroke-width="1"/>
                    <rect x="2.5" y="14.5" width="4" height="4" fill="currentColor"/>
                    <!-- data dots -->
                    <rect x="9" y="1" width="1" height="1" fill="currentColor"/>
                    <rect x="11" y="1" width="1" height="1" fill="currentColor"/>
                    <rect x="9" y="3" width="2" height="1" fill="currentColor"/>
                    <rect x="9" y="5" width="1" height="1" fill="currentColor"/>
                    <rect x="11" y="5" width="1" height="1" fill="currentColor"/>
                    <rect x="9" y="7" width="1" height="1" fill="currentColor"/>
                    <rect x="1" y="9" width="1" height="2" fill="currentColor"/>
                    <rect x="3" y="9" width="2" height="1" fill="currentColor"/>
                    <rect x="6" y="9" width="1" height="1" fill="currentColor"/>
                    <rect x="8" y="9" width="2" height="2" fill="currentColor"/>
                    <rect x="11" y="9" width="1" height="1" fill="currentColor"/>
                    <rect x="13" y="9" width="2" height="1" fill="currentColor"/>
                    <rect x="16" y="9" width="1" height="2" fill="currentColor"/>
                    <rect x="18" y="9" width="2" height="1" fill="currentColor"/>
                    <rect x="1" y="11" width="2" height="1" fill="currentColor"/>
                    <rect x="5" y="11" width="1" height="1" fill="currentColor"/>
                    <rect x="9" y="11" width="1" height="2" fill="currentColor"/>
                    <rect x="12" y="11" width="2" height="1" fill="currentColor"/>
                    <rect x="15" y="11" width="1" height="1" fill="currentColor"/>
                    <rect x="19" y="11" width="1" height="1" fill="currentColor"/>
                    <rect x="9" y="13" width="1" height="1" fill="currentColor"/>
                    <rect x="11" y="13" width="2" height="1" fill="currentColor"/>
                    <rect x="14" y="13" width="1" height="1" fill="currentColor"/>
                    <rect x="16" y="13" width="1" height="2" fill="currentColor"/>
                    <rect x="18" y="13" width="2" height="1" fill="currentColor"/>
                    <rect x="9" y="15" width="2" height="1" fill="currentColor"/>
                    <rect x="13" y="15" width="1" height="1" fill="currentColor"/>
                    <rect x="15" y="15" width="1" height="1" fill="currentColor"/>
                    <rect x="19" y="15" width="1" height="1" fill="currentColor"/>
                    <rect x="9" y="17" width="1" height="1" fill="currentColor"/>
                    <rect x="11" y="17" width="2" height="1" fill="currentColor"/>
                    <rect x="14" y="17" width="1" height="2" fill="currentColor"/>
                    <rect x="17" y="17" width="1" height="1" fill="currentColor"/>
                    <rect x="9" y="19" width="2" height="1" fill="currentColor"/>
                    <rect x="12" y="19" width="1" height="1" fill="currentColor"/>
                    <rect x="16" y="19" width="1" height="1" fill="currentColor"/>
                    <rect x="18" y="19" width="2" height="1" fill="currentColor"/>
                  </svg>
                </div>
                <div class="icp-name">{{ participants[0]?.participantName || 'Participant' }}</div>
                <div class="icp-layout-tag">{{ event.selectedLayout }} LAYOUT</div>
              </div>
            </div>

            <div class="al al-ok mt2" style="font-size:12px">
              <i class="fas fa-info-circle"></i>
              The actual PDF will contain a real scannable QR code unique to each participant.
            </div>
          </div>

          <!-- Generated Cards List -->
          <div v-if="generatedCards.length" class="card">
            <div class="ch">
              <div class="ct">Generated Cards <span class="badge badge-ok">{{ generatedCards.length }}</span></div>
            </div>
            <p class="tx3c txs mb2">Cards are ready on the organizer's dashboard to send to participants.</p>

            <!-- QR grid -->
            <div class="qr-grid">
              <div v-for="c in generatedCards.slice(0, 6)" :key="c.id" class="qr-tile">
                <div class="qr-tile-inner">
                  <img v-if="c.qrCodePath" :src="getQrUrl(c)" :alt="c.invitationId" class="qr-tile-img" />
                  <div v-else class="qr-tile-placeholder"><i class="fas fa-spinner fa-spin"></i></div>
                </div>
                <div class="qr-tile-name">{{ c.participantName }}</div>
                <div class="qr-tile-id">{{ c.invitationId?.split('-').slice(-1)[0] }}</div>
                <span v-if="c.cardPdfPath" class="badge badge-ok" style="font-size:10px">
                  <i class="fas fa-file-pdf"></i> PDF Ready
                </span>
                <span v-else class="badge badge-wa" style="font-size:10px">Generating…</span>
              </div>
            </div>

            <div v-if="generatedCards.length > 6" class="tx3c txs mt2 text-center">
              + {{ generatedCards.length - 6 }} more cards generated
            </div>
          </div>

        </div>
      </div>
    </template>

    <!-- Excel Import Modal -->
    <AppModal v-model="showExcel" title="Import Participants from Excel">
      <div class="al al-in mb2">
        <i class="fas fa-info-circle"></i>
        <div>
          Excel columns: <strong>Name</strong> (A), <strong>Email</strong> (B), <strong>Phone</strong> (C).
          Header row is skipped automatically.
        </div>
      </div>
      <div class="upz" @click="$refs.excelFile.click()" @dragover.prevent @drop.prevent="handleDrop">
        <i class="fas fa-file-excel" style="font-size:36px;color:#059669;margin-bottom:10px;display:block"></i>
        <div class="fw6 mb1">Drop Excel file here or click to browse</div>
        <div class="tx3c txs">Supports .xlsx, .xls</div>
        <div v-if="excelFile" class="badge badge-ok mt2">{{ excelFile.name }}</div>
        <input ref="excelFile" type="file" accept=".xlsx,.xls" style="display:none" @change="handleExcelSelect" />
      </div>
      <template #footer>
        <button class="btn btn-g" @click="showExcel = false">Cancel</button>
        <button class="btn btn-p" @click="importExcel" :disabled="!excelFile || importing">
          <i v-if="importing" class="fas fa-spinner fa-spin"></i>
          <i v-else class="fas fa-file-import"></i>
          {{ importing ? 'Importing...' : 'Import' }}
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

const route   = useRoute()
const toast   = useToast()
const eventId = Number(route.params.eventId)

const event          = ref(null)
const loadingEvent   = ref(true)
const participants   = ref([])
const generatedCards = ref([])
const generating     = ref(false)
const showExcel      = ref(false)
const excelFile      = ref(null)
const importing      = ref(false)
const newP           = ref({ participantName: '', email: '', phone: '' })

async function loadEvent() {
  loadingEvent.value = true
  try {
    const { data }      = await adminApi.getEvent(eventId)
    event.value         = data.data
    const { data: cd }  = await adminApi.getCards(eventId)
    generatedCards.value = cd.data
  } catch { toast.error('Failed to load event') }
  finally { loadingEvent.value = false }
}

function addParticipant() {
  if (!newP.value.participantName.trim()) return
  participants.value.push({ ...newP.value })
  newP.value = { participantName: '', email: '', phone: '' }
}

function clearList() {
  if (confirm('Clear all participants?')) participants.value = []
}

async function generateCards() {
  if (!participants.value.length) return
  generating.value = true
  try {
    const { data } = await adminApi.generateCards({ eventId, participants: participants.value })
    generatedCards.value = [...generatedCards.value, ...data.data]
    toast.success(`${data.data.length} cards generated!`)
    participants.value = []
  } catch (e) {
    toast.error(e.response?.data?.message || 'Failed to generate cards')
  } finally { generating.value = false }
}

function getQrUrl(card) {
  // Serve QR image via backend static endpoint
  return `/api/files/qr/${encodeURIComponent(card.invitationId)}`
}

function handleExcelSelect(e) { excelFile.value = e.target.files[0] }
function handleDrop(e)        { excelFile.value = e.dataTransfer.files[0] }

async function importExcel() {
  if (!excelFile.value) return
  importing.value = true
  try {
    const { data } = await adminApi.generateFromExcel(eventId, excelFile.value)
    generatedCards.value = [...generatedCards.value, ...data.data]
    toast.success(`${data.data.length} cards generated from Excel!`)
    showExcel.value = false
    excelFile.value = null
  } catch (e) {
    toast.error(e.response?.data?.message || 'Import failed')
  } finally { importing.value = false }
}

onMounted(loadEvent)
</script>

<style scoped>
/* ── layout ── */
.back-row { display:flex; align-items:center; gap:14px; margin-bottom:18px; }
.back-row h2 { font-size:18px; font-weight:700; }
.loading-box { padding:60px; text-align:center; color:var(--tx3); font-size:15px; }
.gr2 { display:grid; grid-template-columns:1fr 1fr; gap:18px; }
.ch  { display:flex; align-items:center; justify-content:space-between; margin-bottom:16px; }
.ct  { font-size:15px; font-weight:600; }
.text-center { text-align:center; }
.part-list-head { font-size:12px; font-weight:600; color:var(--ok); margin-bottom:8px; }

/* ── event banner ── */
.event-banner {
  border-radius:var(--r2); padding:20px 24px; color:#fff;
  display:flex; align-items:flex-start; justify-content:space-between;
}
.ev-name { font-size:20px; font-weight:800; margin-bottom:8px; }
.ev-info { font-size:13px; opacity:.9; margin-bottom:4px; display:flex; align-items:center; gap:7px; }
.ev-layout-tag {
  font-size:11px; font-weight:700; background:rgba(255,255,255,.2);
  padding:3px 10px; border-radius:20px; margin-bottom:8px; display:inline-block;
}

/* ── invitation card preview ── */
.inv-card-preview {
  display: flex;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0,0,0,.18);
  min-height: 210px;
}

.icp-left {
  flex: 1.6;
  padding: 22px 24px;
  display: flex;
  flex-direction: column;
  gap: 0;
  color: #fff;
}

.icp-brand   { font-size:10px; opacity:.7; margin-bottom:8px; letter-spacing:.5px; }
.icp-title   { font-size:17px; font-weight:800; margin-bottom:10px; line-height:1.3; }
.icp-divider { height:1px; background:rgba(255,255,255,.25); margin:8px 0; }
.icp-row     { font-size:11px; opacity:.85; margin-bottom:5px; display:flex; align-items:center; gap:7px; }
.icp-participant { font-size:14px; font-weight:700; margin-top:10px; }
.icp-id      { font-size:9px; opacity:.6; font-family:monospace; margin-top:3px; }

.icp-right {
  flex: 1;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: rgba(0,0,0,.18);
}

.icp-inv-label {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 1.5px;
  color: rgba(255,255,255,.8);
}

/* QR box — white card with border */
.icp-qr-box {
  background: #fff;
  border-radius: 12px;
  padding: 12px;
  box-shadow: 0 4px 16px rgba(0,0,0,.25);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 110px;
  height: 110px;
}

.mock-qr {
  width: 86px;
  height: 86px;
  color: #1a1a2e;   /* dark dots */
  display: block;
}

.icp-name       { font-size:12px; font-weight:700; color:#fff; text-align:center; }
.icp-layout-tag { font-size:9px; color:rgba(255,255,255,.55); letter-spacing:.5px; }

/* ── QR grid (generated cards) ── */
.qr-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin-top: 6px;
}

.qr-tile {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 10px;
  background: var(--bg2, #f8fafc);
  border-radius: 12px;
  border: 1px solid var(--br, #e2e8f0);
}

.qr-tile-inner {
  background: #fff;
  border-radius: 10px;
  border: 2px solid #1a56db;
  padding: 8px;
  width: 90px;
  height: 90px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 10px rgba(26,86,219,.15);
}

.qr-tile-img {
  width: 74px;
  height: 74px;
  display: block;
  image-rendering: pixelated;  /* keeps QR sharp */
}

.qr-tile-placeholder {
  color: #94a3b8;
  font-size: 22px;
}

.qr-tile-name {
  font-size: 11px;
  font-weight: 600;
  text-align: center;
  color: var(--tx1, #0f172a);
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qr-tile-id {
  font-size: 9px;
  font-family: monospace;
  color: var(--tx3, #94a3b8);
}

@media (max-width:900px) {
  .gr2 { grid-template-columns:1fr; }
  .qr-grid { grid-template-columns: repeat(2,1fr); }
}
</style>