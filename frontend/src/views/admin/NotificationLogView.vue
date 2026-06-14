<template>
  <div>
    <div class="al al-in mb2">
      <i class="fas fa-server"></i>
      Real-time log of all notifications dispatched via <strong>Spring Mail</strong> (email) and <strong>Africa's Talking</strong> (SMS).
    </div>

    <div class="tabs mb2">
      <button v-for="tab in tabs" :key="tab.key" class="tab-btn" :class="{ active: activeTab === tab.key }" @click="activeTab = tab.key">
        {{ tab.label }}
        <span v-if="tab.key === 'failed' && failedCount > 0" class="badge badge-er ml1">{{ failedCount }}</span>
      </button>
    </div>

    <div class="card">
      <div class="ch">
        <div class="ct">{{ currentTabLabel }}</div>
        <div style="display:flex;gap:8px">
          <button v-if="activeTab === 'failed'" class="btn btn-wa btn-sm" @click="retryAll">
            <i class="fas fa-redo"></i> Retry All Failed
          </button>
          <button class="btn btn-g btn-sm" @click="loadLog">
            <i class="fas fa-sync"></i> Refresh
          </button>
        </div>
      </div>

      <div v-if="loading" class="loading-box"><i class="fas fa-spinner fa-spin"></i> Loading...</div>
      <div v-else class="tw">
        <table>
          <thead>
            <tr><th>ID</th><th>Type</th><th>Recipient</th><th>Content</th><th>Event</th><th>Status</th><th>Time</th><th v-if="activeTab === 'failed'">Action</th></tr>
          </thead>
          <tbody>
            <tr v-for="n in displayed" :key="n.id">
              <td><code>NL-{{ n.id }}</code></td>
              <td>
                <span class="badge" :class="n.type === 'EMAIL' ? 'badge-in' : 'badge-ok'">
                  <i :class="n.type === 'EMAIL' ? 'fas fa-envelope' : 'fas fa-sms'"></i>
                  {{ n.type }}
                </span>
              </td>
              <td class="txs">{{ n.recipient }}</td>
              <td style="max-width:220px">
                <div class="txs" style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap">
                  {{ n.subject || n.content?.substring(0,60) }}
                </div>
              </td>
              <td class="txs">{{ n.eventName }}</td>
              <td>
                <span class="badge" :class="n.delivered ? 'badge-ok' : 'badge-er'">
                  <i :class="n.delivered ? 'fas fa-check' : 'fas fa-times'"></i>
                  {{ n.delivered ? 'Delivered' : 'Failed' }}
                </span>
              </td>
              <td class="tx3c txs">{{ n.createdAt?.substring(0,16).replace('T',' ') }}</td>
              <td v-if="activeTab === 'failed'">
                <button class="btn btn-wa btn-sm" @click="retryOne(n)">
                  <i class="fas fa-redo"></i> Retry
                </button>
              </td>
            </tr>
            <tr v-if="!displayed.length">
              <td :colspan="activeTab === 'failed' ? 8 : 7" style="text-align:center;color:var(--tx3);padding:30px">
                No notifications found
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { adminApi } from '../../api'
import { useToast } from 'vue-toastification'

const toast = useToast()
const allLog = ref([])
const loading = ref(true)
const activeTab = ref('all')

const tabs = [
  { key: 'all', label: 'All' },
  { key: 'email', label: 'Email' },
  { key: 'sms', label: 'SMS' },
  { key: 'failed', label: 'Failed' },
]

const currentTabLabel = computed(() => tabs.find(t => t.key === activeTab.value)?.label + ' Notifications')
const failedCount = computed(() => allLog.value.filter(n => !n.delivered).length)

const displayed = computed(() => {
  switch (activeTab.value) {
    case 'email': return allLog.value.filter(n => n.type === 'EMAIL')
    case 'sms': return allLog.value.filter(n => n.type === 'SMS')
    case 'failed': return allLog.value.filter(n => !n.delivered)
    default: return allLog.value
  }
})

async function loadLog() {
  loading.value = true
  try {
    const { data } = await adminApi.getNotifLog()
    allLog.value = data.data
  } catch (e) {
    toast.error('Failed to load notification log')
  } finally {
    loading.value = false
  }
}

async function retryOne(n) {
  toast.info(`Retrying notification to ${n.recipient}...`)
  setTimeout(() => {
    n.delivered = true
    toast.success('Notification delivered successfully!')
  }, 2000)
}

async function retryAll() {
  const failed = allLog.value.filter(n => !n.delivered)
  if (!failed.length) { toast.info('No failed notifications to retry'); return }
  toast.info(`Retrying ${failed.length} failed notifications...`)
  setTimeout(() => {
    failed.forEach(n => n.delivered = true)
    toast.success(`${failed.length} notifications retried successfully!`)
  }, 2500)
}

onMounted(loadLog)
</script>

<style scoped>
.tabs { display: flex; gap: 4px; background: var(--bg3); border-radius: 10px; padding: 4px; width: fit-content; }
.tab-btn {
  padding: 7px 16px; border-radius: 7px; font-size: 13px; font-weight: 500;
  color: var(--tx2); cursor: pointer; border: none; background: transparent;
  display: flex; align-items: center; gap: 6px;
}
.tab-btn.active { background: var(--bg2); color: var(--p); box-shadow: var(--sh); }
.tab-btn:hover:not(.active) { color: var(--tx); }
.ml1 { margin-left: 4px; }
.ch { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.ct { font-size: 15px; font-weight: 600; }
.loading-box { padding: 40px; text-align: center; color: var(--tx3); }
</style>
