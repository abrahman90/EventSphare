<template>
  <div>
    <!-- Steps -->
    <div class="steps-bar">
      <div v-for="(s, i) in steps" :key="i" class="step" :class="{ active: currentStep === i+1, done: currentStep > i+1 }">
        <div class="step-num">
          <i v-if="currentStep > i+1" class="fas fa-check"></i>
          <span v-else>{{ i+1 }}</span>
        </div>
        <div class="step-lbl">{{ s }}</div>
        <div v-if="i < steps.length-1" class="step-line"></div>
      </div>
    </div>

    <!-- Step 1: Event Details -->
    <div v-if="currentStep === 1" class="card">
      <div class="step-title">Step 1: Event Details</div>
      <div class="g2">
        <div class="fg"><label class="fl req">Event Name</label>
          <input v-model="form.eventName" class="fi" placeholder="e.g. Annual Tech Summit 2026" required /></div>
        <div class="fg"><label class="fl req">Category</label>
          <select v-model="form.category" class="fs" @change="calcPrice">
            <option value="">Select category</option>
            <option value="SEMINAR">Seminar (tsh500)</option>
            <option value="WORKSHOP">Workshop (tsh70o)</option>
            <option value="CONFERENCE">Conference (tsh1000)</option>
            <option value="WEDDING">Wedding (tsh1500)</option>
            <option value="CORPORATE_EVENT">Corporate Event (tsh1200)</option>
          </select></div>
      </div>
      <div class="fg"><label class="fl req">Description</label>
        <textarea v-model="form.description" class="fta" rows="4"
          placeholder="Describe your event in detail. This will appear on the invitation card."></textarea>
        <div class="fh">This description will be printed on each invitation card</div>
      </div>
      <div class="g2">
        <div class="fg"><label class="fl req">Venue</label>
          <input v-model="form.venue" class="fi" placeholder="e.g. Julius Nyerere Convention Centre" /></div>
        <div class="fg"><label class="fl req">Date</label>
          <input v-model="form.eventDate" class="fi" type="date" :min="today" /></div>
      </div>
      <div class="g3">
        <div class="fg"><label class="fl req">Start Time</label>
          <input v-model="form.startTime" class="fi" type="time" value="09:00" /></div>
        <div class="fg"><label class="fl req">End Time</label>
          <input v-model="form.endTime" class="fi" type="time" value="18:00" /></div>
        <div class="fg"><label class="fl req">Capacity</label>
          <input v-model.number="form.capacity" class="fi" type="number" min="1" placeholder="200" @input="calcPrice" /></div>
      </div>
      <div class="fg"><label class="fl req">Contact Information</label>
        <input v-model="form.contactInfo" class="fi" placeholder="+255 712 345 678 | event@example.com" /></div>
      <div class="step-footer">
        <div></div>
        <button class="btn btn-p" @click="nextStep" :disabled="!step1Valid">
          Next <i class="fas fa-arrow-right"></i>
        </button>
      </div>
    </div>

    <!-- Step 2: Layout Selection -->
    <div v-if="currentStep === 2" class="card">
      <div class="step-title">Step 2: Select Invitation Layout</div>
      <p class="tx2c txs mb2">The selected layout will be applied to ALL invitation cards generated for this event.</p>
      <div class="layout-grid">
        <div v-for="layout in layouts" :key="layout.key"
          class="layout-card" :class="{ selected: form.selectedLayout === layout.key }"
          @click="form.selectedLayout = layout.key">
          <div class="lc-preview" :class="'inv-' + layout.key.toLowerCase()">
            <div class="lc-check"><i class="fas fa-check"></i></div>
            <div class="lc-name-preview">Event</div>
            <div class="lc-tag">{{ layout.label }}</div>
            <div class="lc-qr"><i class="fas fa-qrcode"></i></div>
          </div>
          <div class="lc-label">{{ layout.label }}</div>
        </div>
      </div>

      <!-- Live Preview -->
      <div v-if="form.selectedLayout" class="preview-wrap mt2">
        <div class="al al-ok mb2">
          <i class="fas fa-check-circle"></i>
          <strong>{{ form.selectedLayout }}</strong> layout selected! This is how your cards will look:
        </div>
        <div class="gc" :class="'inv-' + form.selectedLayout.toLowerCase()">
          <div style="position:absolute;top:-20px;right:-20px;width:80px;height:80px;border-radius:50%;background:rgba(255,255,255,.06)"></div>
          <div style="display:flex;justify-content:space-between;align-items:flex-start">
            <div style="flex:1">
              <div class="gc-ev">{{ form.eventName || 'Your Event Name' }}</div>
              <div class="gc-row"><i class="fas fa-map-marker-alt"></i> {{ form.venue || 'Event Venue' }}</div>
              <div class="gc-row"><i class="fas fa-calendar"></i> {{ form.eventDate || 'Event Date' }}</div>
              <div class="gc-row"><i class="fas fa-clock"></i> {{ form.startTime }} – {{ form.endTime }}</div>
              <div class="gc-row"><i class="fas fa-users"></i> Capacity: {{ form.capacity || '—' }}</div>

            </div>
            <div style="background:#fff;padding:6px;border-radius:8px;width:64px;height:64px;display:flex;align-items:center;justify-content:center;flex-shrink:0">
              <i class="fas fa-qrcode" style="font-size:26px;color:#1a56db"></i>
            </div>
          </div>
          <div class="gc-hr"></div>
          <div class="gc-nm"></div>
          <div class="gc-id">ID: {{ form.eventName?.substring(0,4).toUpperCase() || 'EVT' }}  -{{ new Date().getFullYear() }}-XXXXXXXX ·  <br> {{ form.selectedLayout }} LAYOUT</div>
        </div>
      </div>

      <div class="step-footer">
        <button class="btn btn-g" @click="currentStep--"><i class="fas fa-arrow-left"></i> Back</button>
        <button class="btn btn-p" @click="nextStep" :disabled="!form.selectedLayout">
          Next <i class="fas fa-arrow-right"></i>
        </button>
      </div>
    </div>

    <!-- Step 3: Pricing -->
    <div v-if="currentStep === 3" class="card">
      <div class="step-title">Step 3: Pricing Calculation</div>
      <div class="al al-in mb2">
        <i class="fas fa-calculator"></i>
        Pricing is automatically calculated. No manual entry required.
      </div>
      <div class="g2">
        <div>
          <div class="fg"><label class="fl">Event Category</label>
            <input class="fi" :value="form.category?.replace('_',' ')" disabled /></div>
          <div class="fg"><label class="fl">Participants</label>
            <input class="fi" :value="form.capacity" disabled /></div>
          <div class="fg"><label class="fl">Selected Layout</label>
            <input class="fi" :value="form.selectedLayout" disabled /></div>
        </div>
        <div class="price-box">
          <div class="price-title">Price Breakdown</div>
          <div class="price-row"><span>Base Price (Category)</span><strong>${{ pricing.base }}</strong></div>
          <div class="price-row"><span>Capacity Surcharge</span><strong>${{ pricing.capacity }}</strong></div>
          <div class="price-total"><span>Total Amount Due</span><span>${{ pricing.total }}</span></div>
          <div class="al al-wa mt2" style="font-size:12px">
            <i class="fas fa-exclamation-triangle"></i>
            You will need to upload payment proof after submitting the request.
          </div>
        </div>
      </div>
      <div class="step-footer">
        <button class="btn btn-g" @click="currentStep--"><i class="fas fa-arrow-left"></i> Back</button>
        <button class="btn btn-p" @click="nextStep">Next <i class="fas fa-arrow-right"></i></button>
      </div>
    </div>

    <!-- Step 4: Review & Submit -->
    <div v-if="currentStep === 4" class="card">
      <div class="step-title">Step 4: Review & Submit</div>
      <div class="al al-in mb2">
        <i class="fas fa-info-circle"></i>
        After submission, the admin will review your request and notify you by <strong>email</strong> and <strong>SMS</strong>.
      </div>
      <div class="review-grid">
        <div><span class="rl">Event Name</span><span class="rv">{{ form.eventName }}</span></div>
        <div><span class="rl">Category</span><span class="rv">{{ form.category?.replace('_',' ') }}</span></div>
        <div><span class="rl">Venue</span><span class="rv">{{ form.venue }}</span></div>
        <div><span class="rl">Date</span><span class="rv">{{ form.eventDate }}</span></div>
        <div><span class="rl">Time</span><span class="rv">{{ form.startTime }} – {{ form.endTime }}</span></div>
        <div><span class="rl">Capacity</span><span class="rv">{{ form.capacity }}</span></div>
        <div><span class="rl">Layout</span><span class="rv" style="color:var(--ac);font-weight:600">{{ form.selectedLayout }}</span></div>
        <div><span class="rl">Total Due</span><span class="rv" style="color:var(--p);font-size:18px;font-weight:700">${{ pricing.total }}</span></div>
      </div>
      <div class="fg mt2"><label class="fl">Additional Notes (optional)</label>
        <textarea v-model="form.notes" class="fta" placeholder="Special requirements or notes for admin..."></textarea>
      </div>
      <div class="step-footer">
        <button class="btn btn-g" @click="currentStep--"><i class="fas fa-arrow-left"></i> Back</button>
        <button class="btn btn-ok btn-lg" @click="submitEvent" :disabled="submitting">
          <i v-if="submitting" class="fas fa-spinner fa-spin"></i>
          <i v-else class="fas fa-paper-plane"></i>
          {{ submitting ? 'Submitting...' : 'Submit Request' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { orgApi } from '../../api'
import { useToast } from 'vue-toastification'

const router = useRouter()
const toast = useToast()

const currentStep = ref(1)
const submitting = ref(false)
const steps = ['Event Details', 'Layout', 'Pricing', 'Submit']
const today = new Date().toISOString().split('T')[0]

const form = ref({
  eventName: '', category: '', description: '', venue: '',
  eventDate: '', startTime: '09:00', endTime: '18:00',
  capacity: null, contactInfo: '', selectedLayout: '', notes: ''
})

const layouts = [
  { key: 'CLASSIC', label: 'Classic' },
  { key: 'MODERN', label: 'Modern' },
  { key: 'CORPORATE', label: 'Corporate' },
  { key: 'WEDDING', label: 'Wedding' },
  { key: 'CONFERENCE', label: 'Conference' },
  { key: 'PREMIUM', label: 'Premium' },
]

const categoryPrices = { SEMINAR: 50, WORKSHOP: 70, CONFERENCE: 100, WEDDING: 150, CORPORATE_EVENT: 120 }

const pricing = computed(() => {
  const base = categoryPrices[form.value.category] || 0
  const cap = form.value.capacity > 500 ? 120 : form.value.capacity > 100 ? 50 : 0
  return { base, capacity: cap, total: base + cap }
})

const step1Valid = computed(() => {
  return form.value.eventName && form.value.category && form.value.description &&
         form.value.venue && form.value.eventDate && form.value.capacity > 0
})

function calcPrice() {} // triggered on input, pricing is computed

function nextStep() {
  if (currentStep.value < 4) currentStep.value++
}

async function submitEvent() {
  submitting.value = true
  try {
    const payload = {
      eventName: form.value.eventName,
      category: form.value.category,
      description: form.value.description,
      venue: form.value.venue,
      eventDate: form.value.eventDate,
      startTime: form.value.startTime + ':00',
      endTime: form.value.endTime + ':00',
      capacity: form.value.capacity,
      contactInfo: form.value.contactInfo,
      selectedLayout: form.value.selectedLayout,
    }
    await orgApi.createEvent(payload)
    toast.success('Event submitted! You will be notified by Email & SMS when reviewed.')
    router.push('/organizer/events')
  } catch (e) {
    toast.error(e.response?.data?.message || 'Failed to submit event')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.steps-bar { display: flex; align-items: center; background: var(--bg3); border-radius: 12px; padding: 12px 16px; margin-bottom: 22px; }
.step { display: flex; align-items: center; gap: 8px; flex: 1; }
.step-num { width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700; border: 2px solid var(--bd2); color: var(--tx3); flex-shrink: 0; transition: all .3s; }
.step.active .step-num { background: var(--p); color: #fff; border-color: var(--p); }
.step.done .step-num { background: var(--ok); color: #fff; border-color: var(--ok); }
.step-lbl { font-size: 12px; font-weight: 500; color: var(--tx3); }
.step.active .step-lbl { color: var(--p); }
.step.done .step-lbl { color: var(--ok); }
.step-line { flex: 1; height: 1px; background: var(--bd2); margin: 0 8px; }
.step-title { font-size: 17px; font-weight: 700; margin-bottom: 20px; color: var(--tx); }
.step-footer { display: flex; justify-content: space-between; margin-top: 20px; padding-top: 16px; border-top: 1px solid var(--bd); }
.layout-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px,1fr)); gap: 12px; margin-bottom: 16px; }
.layout-card { border: 2px solid var(--bd); border-radius: var(--r2); cursor: pointer; transition: all .2s; overflow: hidden; background: var(--bg2); }
.layout-card:hover { border-color: var(--p); transform: translateY(-2px); }
.layout-card.selected { border-color: var(--p); box-shadow: 0 0 0 3px rgba(26,86,219,.15); }
.lc-preview { height: 115px; padding: 10px; color: #fff; position: relative; }
.lc-check { position: absolute; top: 7px; right: 7px; width: 20px; height: 20px; background: var(--p); border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 10px; opacity: 0; transition: opacity .2s; }
.layout-card.selected .lc-check { opacity: 1; }
.lc-name-preview { font-size: 15px; font-weight: 800; margin-bottom: 3px; }
.lc-tag { font-size: 9px; opacity: .8; }
.lc-qr { margin-top: 8px; width: 24px; height: 24px; background: rgba(255,255,255,.2); border-radius: 4px; display: flex; align-items: center; justify-content: center; font-size: 11px; }
.lc-label { padding: 8px; text-align: center; font-size: 12px; font-weight: 600; color: var(--tx); border-top: 1px solid var(--bd); }
.preview-wrap { }
.price-box { background: var(--bg3); border-radius: var(--r); padding: 16px; }
.price-title { font-size: 13px; font-weight: 600; margin-bottom: 12px; }
.price-row { display: flex; justify-content: space-between; font-size: 13px; padding: 7px 0; border-bottom: 1px solid var(--bd); }
.price-total { display: flex; justify-content: space-between; font-size: 16px; font-weight: 700; padding: 10px 0; color: var(--p); background: var(--pl); padding: 10px 12px; border-radius: 8px; margin-top: 8px; }
.review-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; background: var(--bg3); border-radius: var(--r); padding: 16px; }
.rl { display: block; font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: .4px; color: var(--tx3); margin-bottom: 3px; }
.rv { font-size: 14px; color: var(--tx); }
@media (max-width: 700px) { .steps-bar { flex-direction: column; align-items: flex-start; gap: 8px; } .step-line { display: none; } }
</style>
