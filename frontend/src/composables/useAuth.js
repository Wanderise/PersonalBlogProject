import { reactive } from 'vue'
import { getDownloadUrl } from '@/api/file.js'

function loadLocalUser() {
  try {
    return JSON.parse(localStorage.getItem('user') || '{}')
  } catch {
    return {}
  }
}

const state = reactive({
  token: localStorage.getItem('token') || '',
  user: loadLocalUser(),
  avatarUrl: '',
  get isLoggedIn() {
    return !!this.token
  },
  get userName() {
    return this.user.name || ''
  }
})

export function useAuth() {
  async function loadAvatar() {
    const key = state.user.image
    if (!key) {
      state.avatarUrl = ''
      return
    }
    try {
      const res = await getDownloadUrl(key)
      state.avatarUrl = res.data.downloadUrl
    } catch {
      state.avatarUrl = ''
    }
  }

  async function setAuth(token, user) {
    state.token = token
    state.user = user
    localStorage.setItem('token', token)
    localStorage.setItem('user', JSON.stringify(user))
    await loadAvatar()
  }

  function clearAuth() {
    state.token = ''
    state.user = {}
    state.avatarUrl = ''
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  function updateLocalUser(patch) {
    Object.assign(state.user, patch)
    localStorage.setItem('user', JSON.stringify(state.user))
  }

  return { state, loadAvatar, setAuth, clearAuth, updateLocalUser }
}
