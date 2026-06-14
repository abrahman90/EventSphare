<template>
  <div>
    <div class="top-bar mb2">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input v-model="search" placeholder="Search users..." />
      </div>
      <div style="display:flex;gap:8px">
        <select v-model="filterRole" class="fs" style="width:130px">
          <option value="">All Roles</option>
          <option value="ADMIN">Admin</option>
          <option value="ORGANIZER">Organizer</option>
        </select>
        <select v-model="filterStatus" class="fs" style="width:160px">
          <option value="">All Statuses</option>
          <option value="ACTIVE">Active</option>
          <option value="PENDING_APPROVAL">Pending Approval</option>
          <option value="INACTIVE">Inactive</option>
        </select>
      </div>
    </div>

    <div class="card">
      <div v-if="loading" class="loading-box"><i class="fas fa-spinner fa-spin"></i> Loading...</div>
      <div v-else class="tw">
        <table>
          <thead>
            <tr><th></th><th>Name</th><th>Email</th><th>Phone</th><th>Role</th><th>Status</th><th>Joined</th><th>Actions</th></tr>
          </thead>
          <tbody>
            <tr v-for="u in filtered" :key="u.id">
              <td>
                <div class="user-av" :style="{ background: getColor(u.id) }">{{ u.fullName?.[0] }}</div>
              </td>
              <td>
                <strong>{{ u.fullName }}</strong>
                <div class="tx3c txs">{{ u.organization }}</div>
              </td>
              <td>{{ u.email }}</td>
              <td>{{ u.phone || '—' }}</td>
              <td>
                <span class="badge" :class="u.role === 'ADMIN' ? 'badge-p' : 'badge-ok'">
                  {{ u.role }}
                </span>
              </td>
              <td><StatusBadge :status="u.accountStatus" /></td>
              <td class="tx3c txs">{{ u.createdAt?.substring(0,10) }}</td>
              <td>
                <div style="display:flex;gap:5px;flex-wrap:wrap">
                  <button class="btn btn-g btn-sm" @click="openEdit(u)" title="Edit"><i class="fas fa-edit"></i></button>
                  <template v-if="u.role !== 'ADMIN'">
                    <button v-if="u.accountStatus === 'PENDING_APPROVAL'" class="btn btn-ok btn-sm" @click="approve(u.id)" title="Approve">
                      <i class="fas fa-check"></i>
                    </button>
                    <button v-if="u.accountStatus === 'ACTIVE'" class="btn btn-wa btn-sm" @click="deactivate(u.id)" title="Deactivate">
                      <i class="fas fa-ban"></i>
                    </button>
                    <button v-if="u.accountStatus === 'INACTIVE'" class="btn btn-ok btn-sm" @click="activate(u.id)" title="Activate">
                      <i class="fas fa-check-circle"></i>
                    </button>
                    <button class="btn btn-g btn-sm" @click="openResetPw(u)" title="Reset Password">
                      <i class="fas fa-key"></i>
                    </button>
                    <button class="btn btn-er btn-sm" @click="deleteUser(u)" title="Delete">
                      <i class="fas fa-trash"></i>
                    </button>
                  </template>
                </div>
              </td>
            </tr>
            <tr v-if="!filtered.length">
              <td colspan="8" style="text-align:center;color:var(--tx3);padding:30px">No users found</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Edit Modal -->
    <AppModal v-model="showEdit" :title="'Edit: ' + (editUser?.fullName || '')">
      <div class="fg"><label class="fl">Full Name</label><input v-model="editForm.fullName" class="fi" /></div>
      <div class="fg"><label class="fl">Phone</label><input v-model="editForm.phone" class="fi" /></div>
      <div class="fg"><label class="fl">Organization</label><input v-model="editForm.organization" class="fi" /></div>
      <div class="fg"><label class="fl">Role</label>
        <select v-model="editForm.role" class="fs">
          <option value="ADMIN">Admin</option>
          <option value="ORGANIZER">Organizer</option>
        </select>
      </div>
      <template #footer>
        <button class="btn btn-g" @click="showEdit = false">Cancel</button>
        <button class="btn btn-p" @click="saveEdit" :disabled="saving">
          <i v-if="saving" class="fas fa-spinner fa-spin"></i>
          <i v-else class="fas fa-save"></i> Save
        </button>
      </template>
    </AppModal>

    <!-- Reset Password Modal -->
    <AppModal v-model="showResetPw" :title="'Reset Password: ' + (editUser?.fullName || '')">
      <div class="al al-wa mb2">
        <i class="fas fa-exclamation-triangle"></i>
        The user will receive an email notification about their new password.
      </div>
      <div class="fg">
        <label class="fl req">New Password</label>
        <input v-model="newPassword" class="fi" type="password" placeholder="Min 8 characters" />
      </div>
      <template #footer>
        <button class="btn btn-g" @click="showResetPw = false">Cancel</button>
        <button class="btn btn-wa" @click="submitResetPw" :disabled="newPassword.length < 8 || saving">
          <i class="fas fa-key"></i> Reset & Notify
        </button>
      </template>
    </AppModal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { adminApi } from '../../api'
import { useToast } from 'vue-toastification'
import StatusBadge from '../../components/StatusBadge.vue'
import AppModal from '../../components/AppModal.vue'

const toast = useToast()
const users = ref([])
const loading = ref(true)
const saving = ref(false)
const search = ref('')
const filterRole = ref('')
const filterStatus = ref('')
const showEdit = ref(false)
const showResetPw = ref(false)
const editUser = ref(null)
const editForm = ref({})
const newPassword = ref('')

const colors = ['#1a56db','#059669','#7c3aed','#d97706','#0284c7','#dc2626']
const getColor = id => colors[id % colors.length]

const filtered = computed(() => {
  const q = search.value.toLowerCase()
  return users.value.filter(u => {
    const mq = !q || u.fullName?.toLowerCase().includes(q) || u.email?.toLowerCase().includes(q)
    const mr = !filterRole.value || u.role === filterRole.value
    const ms = !filterStatus.value || u.accountStatus === filterStatus.value
    return mq && mr && ms
  })
})

async function loadUsers() {
  loading.value = true
  try {
    const { data } = await adminApi.getUsers()
    users.value = data.data
  } catch (e) {
    toast.error('Failed to load users')
  } finally {
    loading.value = false
  }
}

async function approve(id) {
  try {
    await adminApi.approveUser(id)
    const u = users.value.find(u => u.id === id)
    if (u) u.accountStatus = 'ACTIVE'
    toast.success('Organizer approved! Email & SMS sent.')
  } catch (e) { toast.error('Failed to approve') }
}

async function deactivate(id) {
  if (!confirm('Deactivate this user?')) return
  try {
    await adminApi.deactivateUser(id)
    const u = users.value.find(u => u.id === id)
    if (u) u.accountStatus = 'INACTIVE'
    toast.info('User deactivated')
  } catch (e) { toast.error('Failed') }
}

async function activate(id) {
  try {
    await adminApi.activateUser(id)
    const u = users.value.find(u => u.id === id)
    if (u) u.accountStatus = 'ACTIVE'
    toast.success('User activated')
  } catch (e) { toast.error('Failed') }
}

async function deleteUser(u) {
  if (!confirm(`Delete ${u.fullName}? This cannot be undone.`)) return
  try {
    await adminApi.deleteUser(u.id)
    users.value = users.value.filter(x => x.id !== u.id)
    toast.success('User deleted')
  } catch (e) { toast.error('Failed') }
}

function openEdit(u) {
  editUser.value = u
  editForm.value = { fullName: u.fullName, phone: u.phone, organization: u.organization, role: u.role }
  showEdit.value = true
}

async function saveEdit() {
  saving.value = true
  try {
    const { data } = await adminApi.updateUser(editUser.value.id, editForm.value)
    const idx = users.value.findIndex(u => u.id === editUser.value.id)
    if (idx >= 0) users.value[idx] = { ...users.value[idx], ...data.data }
    showEdit.value = false
    toast.success('User updated')
  } catch (e) { toast.error('Failed') } finally { saving.value = false }
}

function openResetPw(u) {
  editUser.value = u
  newPassword.value = ''
  showResetPw.value = true
}

async function submitResetPw() {
  saving.value = true
  try {
    await adminApi.resetPassword(editUser.value.id, newPassword.value)
    showResetPw.value = false
    toast.success('Password reset. Notification email sent.')
  } catch (e) { toast.error('Failed') } finally { saving.value = false }
}

onMounted(loadUsers)
</script>

<style scoped>
.top-bar { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px; }
.search-box { display: flex; align-items: center; gap: 7px; background: var(--bg3); border: 1px solid var(--bd); border-radius: 8px; padding: 6px 12px; }
.search-box input { border: none; background: transparent; outline: none; font-size: 13px; color: var(--tx); width: 180px; }
.loading-box { padding: 40px; text-align: center; color: var(--tx3); }
.user-av { width: 32px; height: 32px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 13px; color: #fff; flex-shrink: 0; }
</style>
