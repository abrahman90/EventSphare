<template>
  <div class="auth-wrap">
    <div class="auth-card">
      <div class="auth-logo">
        <div class="logo-ic"><i class="fas fa-calendar-star"></i></div>
        <h1>Join EventSphere</h1>
        <p>Organizer Registration</p>
      </div>

      <div v-if="success" class="al al-ok">
        <i class="fas fa-check-circle"></i>
        Registration submitted! An admin will review and approve your account. Check your email.
      </div>

      <form v-else @submit.prevent="handleRegister">
        <div class="g2">
          <div class="fg">
            <label class="fl req">Full Name</label>
            <input v-model="form.fullName" class="fi" placeholder="John Mwamba" required />
          </div>
          <div class="fg">
            <label class="fl req">Phone</label>
            <input v-model="form.phone" class="fi" placeholder="+255 712 345 678" required />
          </div>
        </div>
        <div class="fg">
          <label class="fl req">Email Address</label>
          <input v-model="form.email" class="fi" type="email" placeholder="john@events.co.tz" required />
        </div>
        <div class="fg">
          <label class="fl">Organization / Company</label>
          <input v-model="form.organization" class="fi" placeholder="Events Co. Ltd (optional)" />
        </div>
        <div class="fg">
          <label class="fl req">Password</label>
          <input v-model="form.password" class="fi" type="password" placeholder="Min 8 characters" required minlength="8" />
          <div class="fh">At least 8 characters</div>
        </div>

        <div v-if="error" class="al al-er mb1">
          <i class="fas fa-times-circle"></i> {{ error }}
        </div>

        <button type="submit" class="btn btn-p btn-lg btn-full" :disabled="loading">
          <i v-if="loading" class="fas fa-spinner fa-spin"></i>
          <i v-else class="fas fa-user-plus"></i>
          {{ loading ? 'Submitting...' : 'Register' }}
        </button>
      </form>

      <div class="dv"></div>
      <p style="text-align:center;font-size:13px;color:var(--tx2)">
        Already have an account?
        <router-link to="/login" style="color:var(--p);font-weight:600">Sign In</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
const form = ref({ fullName: '', email: '', phone: '', password: '', organization: '' })
const loading = ref(false)
const error = ref('')
const success = ref(false)

async function handleRegister() {
  loading.value = true
  error.value = ''
  try {
    await auth.register(form.value)
    success.value = true
  } catch (e) {
    error.value = e.response?.data?.message || 'Registration failed'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-wrap {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0f172a 100%);
  padding: 20px;
}
.auth-card {
  background: var(--bg2); border-radius: 20px; padding: 38px;
  width: 100%; max-width: 500px; box-shadow: 0 25px 60px rgba(0,0,0,.4);
}
.auth-logo { text-align: center; margin-bottom: 28px; }
.logo-ic {
  width: 56px; height: 56px; background: var(--ok); border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  font-size: 26px; color: #fff; margin: 0 auto 12px;
}
.auth-logo h1 { font-size: 24px; font-weight: 800; color: var(--tx); }
.auth-logo p { font-size: 13px; color: var(--tx2); margin-top: 4px; }
</style>
