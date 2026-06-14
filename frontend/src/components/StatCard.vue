<template>
  <div class="stat-card">
    <div class="stat-icon" :style="{ background: iconBg, color: iconColor }">
      <i :class="icon"></i>
    </div>
    <div class="stat-info">
      <div class="stat-value">{{ value }}</div>
      <div class="stat-label">{{ label }}</div>
      <div v-if="change" class="stat-change" :class="changeType">
        <i :class="changeType === 'up' ? 'fas fa-arrow-up' : 'fas fa-arrow-down'"></i>
        {{ change }}
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  icon: String,
  iconBg: String,
  iconColor: String,
  value: [String, Number],
  label: String,
  change: String,
  changeType: { type: String, default: 'up' }
})
</script>

<style scoped>
.stat-card {
  background: var(--bg2); border: 1px solid var(--bd); border-radius: var(--r2);
  padding: 16px; display: flex; align-items: center; gap: 12px;
  transition: all .2s;
}
.stat-card:hover { transform: translateY(-2px); box-shadow: var(--shh); }
.stat-icon {
  width: 44px; height: 44px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 19px; flex-shrink: 0;
}
.stat-value { font-size: 24px; font-weight: 700; color: var(--tx); line-height: 1.1; }
.stat-label { font-size: 12px; color: var(--tx3); margin-top: 2px; }
.stat-change { font-size: 11px; margin-top: 4px; }
.stat-change.up { color: var(--ok); }
.stat-change.down { color: var(--er); }
</style>
