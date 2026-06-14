<template>
  <div class="auth-wrap">
    <div class="auth-card">
      <div class="auth-logo">
        <div class="logo-ic"><i class="fas fa-calendar-star"></i></div>
        <h1>EventSphere</h1>
        <p>Smart Event Management System</p>
      </div>

      <form @submit.prevent="handleLogin">
        <div class="fg">
          <label class="fl req">Email Address</label>
          <input v-model="form.email" class="fi" type="email" placeholder="admin@eventsphere.com" required />
        </div>
        <div class="fg">
          <label class="fl req">Password</label>
          <div style="position:relative">
            <input v-model="form.password" class="fi" :type="showPw ? 'text' : 'password'"
                   placeholder="••••••••" required style="padding-right:40px" />
            <span @click="showPw=!showPw" class="pw-eye">
              <i :class="showPw ? 'fas fa-eye-slash' : 'fas fa-eye'"></i>
            </span>
          </div>
        </div>

        <div v-if="error" class="al al-er mb1">
          <i class="fas fa-times-circle"></i> {{ error }}
        </div>

        <button type="submit" class="btn btn-p btn-lg btn-full" :disabled="loading">
          <i v-if="loading" class="fas fa-spinner fa-spin"></i>
          <i v-else class="fas fa-sign-in-alt"></i>
          {{ loading ? 'Signing in...' : 'Sign In' }}
        </button>
      </form>

      <div class="dv"></div>
      <p style="text-align:center;font-size:13px;color:var(--tx2)">
        New organizer?
        <router-link to="/register" style="color:var(--p);font-weight:600">Register here</router-link>
      </p>

      <div style="margin-top:16px;background:var(--bg3);border-radius:8px;padding:12px;font-size:12px;color:var(--tx3)">
        <div><strong>Admin:</strong> admin@eventsphere.com / Admin@2025</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { useToast } from 'vue-toastification'

const router = useRouter()
const auth = useAuthStore()
const toast = useToast()

const form = ref({ email: '', password: '' })
const loading = ref(false)
const error = ref('')
const showPw = ref(false)

async function handleLogin() {
  loading.value = true
  error.value = ''
  try {
    const user = await auth.login(form.value)
    toast.success(`Welcome back, ${user.fullName}!`)
    if (user.role === 'ADMIN') router.push('/admin/dashboard')
    else router.push('/organizer/dashboard')
  } catch (e) {
    error.value = e.response?.data?.message || 'Invalid credentials'
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
  width: 100%; max-width: 420px; box-shadow: 0 25px 60px rgba(0,0,0,.4);
}
.auth-logo { text-align: center; margin-bottom: 28px; }
.logo-ic {
  width: 56px; height: 56px; background: var(--p); border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  font-size: 26px; color: #fff; margin: 0 auto 12px;
}
.auth-logo h1 { font-size: 26px; font-weight: 800; letter-spacing: -.5px; color: var(--tx); }
.auth-logo p { font-size: 13px; color: var(--tx2); margin-top: 4px; }
.pw-eye {
  position: absolute; right: 12px; top: 50%; transform: translateY(-50%);
  cursor: pointer; color: var(--tx3);
}
</style>
