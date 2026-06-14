<template>
  <div class="gr2">
    <div class="card">
      <div class="ch"><div class="ct">System Settings</div></div>
      <div class="fg"><label class="fl">System Name</label><input class="fi" value="EventSphere" /></div>
      <div class="fg"><label class="fl">Default Language</label>
        <select class="fs"><option>English</option><option>Kiswahili</option></select>
      </div>
      <div class="fg">
        <div style="display:flex;justify-content:space-between;align-items:center">
          <div><div class="fl">Email Notifications</div><div class="fh">Trigger on event/payment status changes</div></div>
          <label class="toggle"><input type="checkbox" checked /><span class="ts"></span></label>
        </div>
      </div>
      <div class="fg">
        <div style="display:flex;justify-content:space-between;align-items:center">
          <div><div class="fl">SMS Notifications</div><div class="fh">Africa's Talking API integration</div></div>
          <label class="toggle"><input type="checkbox" checked /><span class="ts"></span></label>
        </div>
      </div>
      <div class="fg">
        <div style="display:flex;justify-content:space-between;align-items:center">
          <div><div class="fl">Dark Mode</div></div>
          <label class="toggle"><input type="checkbox" @change="toggleTheme" :checked="isDark" /><span class="ts"></span></label>
        </div>
      </div>
      <button class="btn btn-p btn-full" @click="save"><i class="fas fa-save"></i> Save Settings</button>
    </div>

    <div class="card">
      <div class="ch"><div class="ct">Pricing Configuration</div></div>
      <div class="al al-in mb2"><i class="fas fa-calculator"></i> Formula: Total = Category Price + Capacity Charge</div>
      <div class="price-section">
        <div class="price-head">Category Base Prices</div>
        <div class="price-row" v-for="cat in categories" :key="cat.name">
          <span>{{ cat.name }}</span>
          <input class="fi" v-model="cat.price" style="width:100px;text-align:right;padding:5px 8px" />
        </div>
      </div>
      <div class="price-section mt2">
        <div class="price-head">Capacity Surcharges</div>
        <div class="price-row"><span>1–100 participants</span><span style="color:var(--ok);font-weight:600">Free</span></div>
        <div class="price-row"><span>101–500 participants</span><input class="fi" value="+$50" style="width:100px;text-align:right;padding:5px 8px" /></div>
        <div class="price-row"><span>500+ participants</span><input class="fi" value="+$120" style="width:100px;text-align:right;padding:5px 8px" /></div>
      </div>
      <button class="btn btn-ok btn-full mt2" @click="save"><i class="fas fa-save"></i> Save Pricing</button>
    </div>

    <div class="card">
      <div class="ch"><div class="ct"><i class="fas fa-envelope" style="color:var(--p)"></i> Email – Spring Mail</div></div>
      <div class="fg"><label class="fl">SMTP Host</label><input class="fi" value="smtp.gmail.com" /></div>
      <div class="g2"><div class="fg"><label class="fl">Port</label><input class="fi" value="587" /></div><div class="fg"><label class="fl">Security</label><select class="fs"><option>TLS</option><option>SSL</option></select></div></div>
      <div class="fg"><label class="fl">Email</label><input class="fi" value="noreply@eventsphere.com" /></div>
      <div class="fg"><label class="fl">App Password</label><input class="fi" type="password" value="app_password" /></div>
      <button class="btn btn-g btn-sm" @click="testEmail"><i class="fas fa-paper-plane"></i> Send Test Email</button>
    </div>

    <div class="card">
      <div class="ch"><div class="ct"><i class="fas fa-sms" style="color:var(--ok)"></i> SMS – Africa's Talking</div></div>
      <div class="fg"><label class="fl">API Key</label><input class="fi" type="password" value="atsk_xxxx" /></div>
      <div class="fg"><label class="fl">Username</label><input class="fi" value="eventsphere_prod" /></div>
      <div class="fg"><label class="fl">Sender ID</label><input class="fi" value="EventSphere" /></div>
      <div class="fg"><label class="fl">Country</label>
        <select class="fs"><option>Tanzania (+255)</option><option>Kenya (+254)</option><option>Uganda (+256)</option></select>
      </div>
      <button class="btn btn-g btn-sm" @click="testSMS"><i class="fas fa-sms"></i> Send Test SMS</button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useToast } from 'vue-toastification'

const toast = useToast()
const isDark = ref(document.documentElement.getAttribute('data-theme') === 'dark')

const categories = ref([
  { name: 'Seminar', price: '$50' },
  { name: 'Workshop', price: '$70' },
  { name: 'Conference', price: '$100' },
  { name: 'Wedding', price: '$150' },
  { name: 'Corporate Event', price: '$120' },
])

function toggleTheme() {
  isDark.value = !isDark.value
  document.documentElement.setAttribute('data-theme', isDark.value ? 'dark' : 'light')
  localStorage.setItem('es_theme', isDark.value ? 'dark' : 'light')
}

function save() { toast.success('Settings saved successfully!') }
function testEmail() { toast.info('Test email sent via Spring Mail. Check your inbox.') }
function testSMS() { toast.success("Test SMS sent via Africa's Talking.") }
</script>

<style scoped>
.gr2 { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }
.ch { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.ct { font-size: 15px; font-weight: 600; }
.price-section { }
.price-head { font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: .4px; color: var(--tx3); margin-bottom: 10px; }
.price-row { display: flex; align-items: center; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid var(--bd); font-size: 13px; }
.price-row:last-child { border-bottom: none; }
.toggle { position: relative; display: inline-block; width: 38px; height: 21px; }
.toggle input { opacity: 0; width: 0; height: 0; }
.ts { position: absolute; inset: 0; background: var(--bd2); border-radius: 11px; cursor: pointer; transition: .3s; }
.ts::before { content: ""; position: absolute; width: 15px; height: 15px; background: #fff; border-radius: 50%; left: 3px; top: 3px; transition: .3s; }
.toggle input:checked + .ts { background: var(--p); }
.toggle input:checked + .ts::before { transform: translateX(17px); }
@media (max-width: 900px) { .gr2 { grid-template-columns: 1fr; } }
</style>
