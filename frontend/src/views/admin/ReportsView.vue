<template>
  <div>
    <div class="stat-grid">
      <StatCard icon="fas fa-calendar-check" iconBg="#d1fae5" iconColor="#059669" :value="stats.totalEvents" label="Total Events" />
      <StatCard icon="fas fa-check-circle" iconBg="#d1fae5" iconColor="#059669" :value="stats.approvedEvents" label="Approved" />
      <StatCard icon="fas fa-hourglass" iconBg="#fef3c7" iconColor="#d97706" :value="stats.pendingEvents" label="Pending" />
      <StatCard icon="fas fa-dollar-sign" iconBg="#fee2e2" iconColor="#dc2626" :value="'$' + (stats.totalRevenue||0).toLocaleString()" label="Revenue" />
    </div>
    <div class="gr2">
      <div class="card">
        <div class="ch"><div class="ct">Revenue Overview (Monthly)</div></div>
        <div style="height:240px;display:flex;align-items:center;justify-content:center;color:var(--tx3)">
          <div style="text-align:center">
            <i class="fas fa-chart-bar" style="font-size:48px;margin-bottom:12px;display:block"></i>
            Chart renders with Chart.js integration in production
          </div>
        </div>
      </div>
      <div class="card">
        <div class="ch"><div class="ct">Events by Category</div></div>
        <div style="height:240px;display:flex;align-items:center;justify-content:center;color:var(--tx3)">
          <div style="text-align:center">
            <i class="fas fa-chart-pie" style="font-size:48px;margin-bottom:12px;display:block"></i>
            Doughnut chart with live data
          </div>
        </div>
      </div>
    </div>
    <div class="mt2 card">
      <div class="ch">
        <div class="ct">Export Reports</div>
        <div style="display:flex;gap:8px">
          <input type="date" class="fi" style="width:150px" />
          <input type="date" class="fi" style="width:150px" />
          <button class="btn btn-p btn-sm"><i class="fas fa-filter"></i> Filter</button>
        </div>
      </div>
      <div style="display:flex;gap:10px;flex-wrap:wrap">
        <button class="btn btn-g"><i class="fas fa-file-pdf"></i> Export PDF</button>
        <button class="btn btn-g"><i class="fas fa-file-excel"></i> Export Excel</button>
        <button class="btn btn-g"><i class="fas fa-file-csv"></i> Export CSV</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '../../api'
import StatCard from '../../components/StatCard.vue'

const stats = ref({ totalEvents: 0, approvedEvents: 0, pendingEvents: 0, totalRevenue: 0 })

onMounted(async () => {
  try {
    const { data } = await adminApi.dashboard()
    stats.value = data.data
  } catch (e) {}
})
</script>

<style scoped>
.stat-grid { display: grid; grid-template-columns: repeat(4,1fr); gap: 14px; margin-bottom: 22px; }
.gr2 { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }
.ch { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; flex-wrap: wrap; gap: 10px; }
.ct { font-size: 15px; font-weight: 600; }
@media (max-width: 900px) { .stat-grid { grid-template-columns: 1fr 1fr; } .gr2 { grid-template-columns: 1fr; } }
</style>
