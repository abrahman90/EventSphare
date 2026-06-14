<template>
  <router-view />
</template>

<script setup>
import { onMounted } from 'vue'
import './assets/main.css'

onMounted(() => {
  const saved = localStorage.getItem('es_theme') || 'light'
  document.documentElement.setAttribute('data-theme', saved)
})
</script>
