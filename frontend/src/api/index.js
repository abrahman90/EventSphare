import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8082',  
  timeout: 30000,
})

api.interceptors.request.use(config => {
  const token = localStorage.getItem('es_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      localStorage.clear()
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

export default api

// ── Auth ──────────────────────────────────────────────────────────────────────
export const authApi = {
  login: d => api.post('/api/auth/login', d),
  register: d => api.post('/api/auth/register', d),
  me: () => api.get('/api/auth/me'),
}

// ── Admin ─────────────────────────────────────────────────────────────────────
export const adminApi = {
  dashboard: () => api.get('/api/admin/dashboard'),

  // Users
  getUsers: () => api.get('/api/admin/users'),
  getOrganizers: () => api.get('/api/admin/users/organizers'),
  getPending: () => api.get('/api/admin/users/pending'),
  approveUser: id => api.post(`/api/admin/users/${id}/approve`),
  deactivateUser: id => api.post(`/api/admin/users/${id}/deactivate`),
  activateUser: id => api.post(`/api/admin/users/${id}/activate`),
  deleteUser: id => api.delete(`/api/admin/users/${id}`),
  resetPassword: (id, newPassword) => api.patch(`/api/admin/users/${id}/reset-password`, { newPassword }),
  updateUser: (id, data) => api.patch(`/api/admin/users/${id}`, data),

  // Events
  getEvents: () => api.get('/api/admin/events'),
  getEvent: id => api.get(`/api/admin/events/${id}`),
  updateEventStatus: (id, data) => api.patch(`/api/admin/events/${id}/status`, data),

  // Payments
  getPayments: () => api.get('/api/admin/payments'),
  verifyPayment: (id, data) => api.post(`/api/admin/payments/${id}/verify`, data),

  // Invitations
  generateCards: (eventId) =>
    api.get(`/api/admin/invitations/generate/${eventId}`, {
      responseType: 'blob'
    }),
  downloadCards: (eventId) =>
    api.get(`/api/admin/invitations/download/${eventId}`, {
      responseType: 'blob'
    }),
  getCards: eventId => api.get(`/api/admin/invitations/event/${eventId}`),
  getStats: eventId => api.get(`/api/admin/invitations/stats/${eventId}`),

  // Notifications
  getNotifLog: () => api.get('/api/admin/notifications/log'),
  getFailedNotifs: () => api.get('/api/admin/notifications/failed'),
}

// ── Organizer ─────────────────────────────────────────────────────────────────
export const orgApi = {
  dashboard: () => api.get('/api/organizer/dashboard'),
  profile: () => api.get('/api/organizer/profile'),

  // Events
  createEvent: data => api.post('/api/organizer/events', data),
  getMyEvents: () => api.get('/api/organizer/events'),
  getEvent: id => api.get(`/api/organizer/events/${id}`),

  // Payments
  getPayment: eventId => api.get(`/api/organizer/payments/event/${eventId}`),
  uploadProof: (paymentId, file) => {
    const form = new FormData()
    form.append('file', file)
    return api.post(`/api/organizer/payments/${paymentId}/upload-proof`, form, {
     // headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  downloadExcelTemplate: () =>
  api.get(`/api/organizer/invitations/excel-template`, {
    responseType: 'blob'
  }),

  // Invitations
  getMyCards: eventId => api.get(`/api/organizer/invitations/event/${eventId}`),
  sendSingle: data => api.post('/api/organizer/invitations/send-single', data),
  sendBulk: data => api.post('/api/organizer/invitations/send-bulk', data),
  sendFromExcel: (eventId, file, sendEmail = true, sendSms = false) => {
    const form = new FormData()
    form.append('eventId', eventId)
    form.append('file', file)
    form.append('sendEmail', sendEmail)
    form.append('sendSms', sendSms)
    return api.post('/api/organizer/invitations/send-from-excel', form)
  },
  getStats: eventId => api.get(`/api/organizer/invitations/stats/${eventId}`),
  scan: invitationId => api.post('/api/organizer/invitations/scan', { invitationId }),
  downloadMyCards: (eventId) =>
    api.get(`/api/organizer/invitations/download/${eventId}`, {
      responseType: 'blob'
    }),
  updateCardParticipant: (cardId, data) =>
    api.patch(`/api/organizer/invitations/cards/${cardId}/participant`, data),
}
