<template>
  <span class="badge" :class="badgeClass">{{ label }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ status: String })

const map = {
  APPROVED: { cls: 'badge-ok', label: 'Approved' },
  ACTIVE: { cls: 'badge-ok', label: 'Active' },
  DELIVERED: { cls: 'badge-ok', label: 'Delivered' },
  PENDING: { cls: 'badge-wa', label: 'Pending' },
  PENDING_APPROVAL: { cls: 'badge-wa', label: 'Pending Approval' },
  UNDER_REVIEW: { cls: 'badge-in', label: 'Under Review' },
  REJECTED: { cls: 'badge-er', label: 'Rejected' },
  FAILED: { cls: 'badge-er', label: 'Failed' },
  INACTIVE: { cls: 'badge-er', label: 'Inactive' },
  IN_PROGRESS: { cls: 'badge-ac', label: 'In Progress' },
  COMPLETED: { cls: 'badge-gray', label: 'Completed' },
  SENT: { cls: 'badge-p', label: 'Sent' },
  OPENED: { cls: 'badge-ok', label: 'Opened' },
}

const badgeClass = computed(() => map[props.status]?.cls || 'badge-gray')
const label = computed(() => map[props.status]?.label || props.status)
</script>
