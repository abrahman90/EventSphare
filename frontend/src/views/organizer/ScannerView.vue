<template>
  <div>
    <!-- Event selector -->
    <div class="card mb2">
      <div class="ch">
        <div class="ct">Select Event to Scan</div>
      </div>
      <div class="fg">
        <label class="fl">Event</label>
        <select v-model="selectedEventId" class="fs" @change="loadExpected">
          <option value="">-- Select an event --</option>
          <option v-for="ev in myEvents" :key="ev.id" :value="ev.id">
            {{ ev.eventName }} ({{ ev.eventDate }})
          </option>
        </select>
      </div>
    </div>

    <div class="gr2">
      <!-- ── Scanner Panel ── -->
      <div class="card">
        <div class="ct mb2">QR Code Scanner</div>

        <div class="al al-in mb2">
          <i class="fas fa-info-circle"></i>
          Scan participant QR codes at the event entrance. Each ticket can only be scanned once.
        </div>

        <!-- Camera zone -->
        <div class="scan-zone" :class="{ scanning: scanning }" @click="triggerCameraScan">
          <div class="scan-frame">
            <div class="scan-corners">
              <span class="corner tl"></span>
              <span class="corner tr"></span>
              <span class="corner bl"></span>
              <span class="corner br"></span>
            </div>
            <div class="scan-line"></div>
            <div class="scan-hint">
              <i class="fas fa-camera" style="font-size:32px;margin-bottom:10px;display:block"></i>
              <span v-if="!scanning">Tap to activate camera</span>
              <span v-else>Scanning...</span>
              <div style="font-size:11px;opacity:.6;margin-top:4px">
                Or enter the invitation ID below
              </div>
            </div>
          </div>
        </div>

        <!-- Manual input -->
        <div class="mt2">
          <label class="fl">Enter Invitation ID manually</label>
          <div class="input-row">
            <input
              v-model="manualCode"
              class="fi"
              placeholder="e.g. SDGGD-4-0001"
              @keyup.enter="scan(manualCode)"
              :disabled="scanning"
            />
            <button
              class="btn btn-p"
              @click="scan(manualCode)"
              :disabled="!manualCode.trim() || scanning">
              <i v-if="scanning" class="fas fa-spinner fa-spin"></i>
              <i v-else class="fas fa-search"></i>
              Verify
            </button>
          </div>
        </div>

        <!-- Scan Result -->
        <transition name="result-fade">
          <div v-if="result" class="scan-result mt2" :class="resultClass">
            <div class="result-icon">{{ resultIcon }}</div>
            <div class="result-title">{{ resultTitle }}</div>
            <div v-if="result.participantName" class="result-detail">
              <i class="fas fa-user"></i>
              <strong>{{ result.participantName }}</strong>
            </div>
            <div v-if="result.eventName" class="result-detail">
              <i class="fas fa-calendar-alt"></i> {{ result.eventName }}
            </div>
            <div v-if="result.scannedAt" class="result-time">
              <i class="fas fa-clock"></i>
              Scanned at: {{ formatTime(result.scannedAt) }}
            </div>
            <div v-if="result.message" class="result-msg">{{ result.message }}</div>
          </div>
        </transition>
      </div>

      <!-- ── Attendance Log ── -->
      <div class="card">
        <div class="ch">
          <div class="ct">Attendance Log</div>
          <div style="display:flex;gap:8px;align-items:center">
            <span class="badge badge-ok">{{ checkedIn }} checked in</span>
            <button class="btn btn-g btn-sm" @click="clearLog" title="Clear log">
              <i class="fas fa-trash"></i>
            </button>
          </div>
        </div>

        <!-- Stats -->
        <div class="att-stats">
          <div class="att-stat">
            <div class="att-num" style="color:var(--ok)">{{ checkedIn }}</div>
            <div class="att-lbl">Checked In</div>
          </div>
          <div class="att-stat">
            <div class="att-num">{{ totalExpected }}</div>
            <div class="att-lbl">Expected</div>
          </div>
          <div class="att-stat">
            <div class="att-num" style="color:var(--er)">
              {{ Math.max(0, totalExpected - checkedIn) }}
            </div>
            <div class="att-lbl">Remaining</div>
          </div>
        </div>

        <div class="progress-bar mb1">
          <div class="progress-fill" :style="{ width: progressPct + '%' }"></div>
        </div>
        <div class="tx3c txs mb2">{{ progressPct.toFixed(1) }}% attendance rate</div>

        <!-- Log entries -->
        <div class="log-list">
          <div v-if="!scanLog.length" class="log-empty">
            <i class="fas fa-qrcode"></i>
            <p>No scans yet. Start scanning to see attendance log.</p>
          </div>
          <div
            v-for="entry in scanLog"
            :key="entry.id"
            class="log-entry"
            :class="entry.type">
            <div class="log-av" :class="'av-' + entry.type">
              {{ entry.name?.[0]?.toUpperCase() || '?' }}
            </div>
            <div class="log-info">
              <div class="log-name">{{ entry.name }}</div>
              <div class="log-sub">{{ entry.event || entry.code }}</div>
            </div>
            <div class="log-right">
              <span class="badge" :class="badgeClass(entry.type)">
                {{ entry.type === 'valid'
                  ? '✅ Entry'
                  : entry.type === 'used'
                  ? '⚠️ Used'
                  : '❌ Invalid' }}
              </span>
              <div class="log-time">{{ entry.time }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { orgApi } from '../../api'
import { useToast } from 'vue-toastification'

const toast = useToast()

const manualCode      = ref('')
const scanning        = ref(false)
const result          = ref(null)
const scanLog         = ref([])
const checkedIn       = ref(0)
const totalExpected   = ref(0)
const myEvents        = ref([])
const selectedEventId = ref('')

const progressPct = computed(() =>
  totalExpected.value > 0
    ? Math.min((checkedIn.value / totalExpected.value) * 100, 100)
    : 0
)

const resultClass = computed(() => {
  if (!result.value) return ''
  if (result.value.status === 'VALID')           return 'result-valid'
  if (result.value.status === 'ALREADY_SCANNED') return 'result-used'
  return 'result-invalid'
})

const resultIcon = computed(() => {
  if (!result.value) return ''
  if (result.value.status === 'VALID')           return '✅'
  if (result.value.status === 'ALREADY_SCANNED') return '⚠️'
  return '❌'
})

const resultTitle = computed(() => {
  if (!result.value) return ''
  if (result.value.status === 'VALID')           return 'Valid Ticket – Entry Granted!'
  if (result.value.status === 'ALREADY_SCANNED') return 'Ticket Already Used'
  return 'Invalid Ticket'
})

function badgeClass(type) {
  if (type === 'valid') return 'badge-ok'
  if (type === 'used')  return 'badge-wa'
  return 'badge-er'
}

function formatTime(ts) {
  if (!ts) return ''
  return ts.substring(0, 19).replace('T', ' ')
}

// ── Load organizer's events ──────────────────────────────────────────────────
async function loadEvents() {
  try {
    const { data } = await orgApi.getMyEvents()
    myEvents.value = data.data || []
  } catch (e) {
    console.error('Failed to load events:', e)
  }
}

// ── Load expected attendance count ───────────────────────────────────────────
async function loadExpected() {
  if (!selectedEventId.value) {
    totalExpected.value = 0
    return
  }
  try {
    const { data } = await orgApi.getStats(selectedEventId.value)  // ← fixed: was getInvitationStats
    totalExpected.value = data.data?.total || 0
  } catch (e) {
    console.error('Failed to load stats:', e)
    totalExpected.value = 0
  }
}

// ── Camera scan ──────────────────────────────────────────────────────────────
function triggerCameraScan() {
  toast.info('Use the manual input field to enter the Invitation ID from the QR code.')
}

// ── Main scan function ───────────────────────────────────────────────────────
async function scan(rawInput) {
  if (!rawInput?.trim()) return
  scanning.value = true
  result.value   = null

  // Extract invitation ID — QR data may be pipe-separated: "INVID|name|event|date|venue"
  const invitationId = rawInput.includes('|')
    ? rawInput.split('|')[0].trim()
    : rawInput.trim()

  try {
    const { data } = await orgApi.scan(invitationId)
    const res = data.data

    // Handle case where backend returns null data (500 was swallowed)
    if (!res) {
      result.value = {
        status: 'INVALID',
        message: 'Server error — could not verify ticket. Check backend logs.'
      }
      addToLog(result.value, invitationId)
      toast.error('Server error during scan')
      return
    }

    result.value = res
    addToLog(res, invitationId)

    if (res.status === 'VALID') {
      checkedIn.value++
      toast.success(`✅ ${res.participantName || 'Participant'} – Entry Granted!`)
    } else if (res.status === 'ALREADY_SCANNED') {
      toast.warning('⚠️ This ticket has already been scanned!')
    } else {
      toast.error('❌ Invalid ticket – not found in system.')
    }
  } catch (e) {
    // Log full error details for debugging
    console.error('Scan failed - status:', e.response?.status)
    console.error('Scan failed - response:', e.response?.data)
    console.error('Scan failed - error:', e.message)

    const errorMsg = e.response?.data?.message
      || e.response?.data?.error
      || e.message
      || 'Could not verify ticket. Check connection.'

    result.value = {
      status: 'INVALID',
      message: errorMsg
    }
    addToLog(result.value, invitationId)
    toast.error(`Scan failed: ${errorMsg}`)
  } finally {
    scanning.value   = false
    manualCode.value = ''
  }
}

function addToLog(res, code) {
  const type = res.status === 'VALID'
    ? 'valid'
    : res.status === 'ALREADY_SCANNED'
    ? 'used'
    : 'invalid'

  scanLog.value.unshift({
    id:    Date.now(),
    name:  res.participantName || 'Unknown',
    event: res.eventName || '',
    code:  code?.substring(0, 24) || '',
    type,
    time:  new Date().toLocaleTimeString(),
  })

  if (scanLog.value.length > 100) scanLog.value.pop()
}

function clearLog() {
  if (confirm('Clear the attendance log for this session?')) {
    scanLog.value   = []
    checkedIn.value = 0
    result.value    = null
  }
}

onMounted(loadEvents)
</script>

<style scoped>
.gr2 { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }
.ct  { font-size: 15px; font-weight: 600; }
.ch  { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }

/* ── Scan zone ── */
.scan-zone {
  background: var(--bg3);
  border: 2px dashed var(--bd2);
  border-radius: var(--r2);
  padding: 32px 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all .2s;
  min-height: 240px;
}
.scan-zone:hover,
.scan-zone.scanning { border-color: var(--p); background: var(--pl); }

.scan-frame {
  width: 200px; height: 200px; position: relative;
  display: flex; align-items: center; justify-content: center;
}

.scan-corners { position: absolute; inset: 0; }
.corner { position: absolute; width: 24px; height: 24px; border-color: var(--p); border-style: solid; }
.corner.tl { top:0;    left:0;  border-width: 3px 0 0 3px; border-radius: 4px 0 0 0; }
.corner.tr { top:0;    right:0; border-width: 3px 3px 0 0; border-radius: 0 4px 0 0; }
.corner.bl { bottom:0; left:0;  border-width: 0 0 3px 3px; border-radius: 0 0 0 4px; }
.corner.br { bottom:0; right:0; border-width: 0 3px 3px 0; border-radius: 0 0 4px 0; }

.scan-line {
  position: absolute; top: 0; left: 0; right: 0; height: 2px;
  background: linear-gradient(90deg, transparent, var(--p), transparent);
  animation: scanLine 1.8s linear infinite;
}
@keyframes scanLine { 0% { top: 0; } 100% { top: 100%; } }
.scan-hint { text-align: center; color: var(--tx3); font-size: 13px; font-weight: 500; }

/* ── Input row ── */
.input-row { display: flex; gap: 8px; }

/* ── Scan result ── */
.scan-result {
  border-radius: var(--r2); padding: 22px 18px; text-align: center;
}
.result-valid   { background: #d1fae5; border: 2px solid #059669; }
.result-used    { background: #fef3c7; border: 2px solid #d97706; }
.result-invalid { background: #fee2e2; border: 2px solid #dc2626; }
.result-icon  { font-size: 44px; margin-bottom: 8px; }
.result-title { font-size: 18px; font-weight: 700; margin-bottom: 10px; }
.result-detail {
  font-size: 14px; margin-bottom: 5px;
  display: flex; align-items: center; justify-content: center; gap: 6px;
}
.result-time {
  font-size: 12px; opacity: .7; margin-top: 6px;
  display: flex; align-items: center; justify-content: center; gap: 5px;
}
.result-msg { font-size: 12px; opacity: .8; margin-top: 5px; }

.result-fade-enter-active { transition: all .3s ease; }
.result-fade-enter-from   { opacity: 0; transform: translateY(-10px); }

/* ── Stats ── */
.att-stats {
  display: grid; grid-template-columns: repeat(3,1fr);
  gap: 8px; margin-bottom: 12px;
}
.att-stat  { text-align: center; background: var(--bg3); border-radius: 8px; padding: 10px; }
.att-num   { font-size: 22px; font-weight: 700; }
.att-lbl   { font-size: 11px; color: var(--tx3); margin-top: 2px; }

.progress-bar  { height: 8px; background: var(--bd); border-radius: 4px; overflow: hidden; }
.progress-fill { height: 100%; background: var(--ok); border-radius: 4px; transition: width .5s ease; }

/* ── Log ── */
.log-list  { max-height: 360px; overflow-y: auto; }
.log-empty { text-align: center; padding: 30px; color: var(--tx3); }
.log-empty i { font-size: 32px; margin-bottom: 8px; display: block; }

.log-entry {
  display: flex; align-items: center; gap: 10px;
  padding: 9px 0; border-bottom: 1px solid var(--bd);
}
.log-entry:last-child { border-bottom: none; }

.log-av {
  width: 34px; height: 34px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  font-weight: 700; font-size: 14px; flex-shrink: 0;
}
.av-valid   { background: #d1fae5; color: #059669; }
.av-used    { background: #fef3c7; color: #d97706; }
.av-invalid { background: #fee2e2; color: #dc2626; }

.log-info { flex: 1; min-width: 0; }
.log-name {
  font-size: 13px; font-weight: 600;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.log-sub {
  font-size: 10px; color: var(--tx3); font-family: monospace;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

.log-right { text-align: right; flex-shrink: 0; }
.log-time  { font-size: 10px; color: var(--tx3); margin-top: 3px; }

@media (max-width: 900px) { .gr2 { grid-template-columns: 1fr; } }
</style>