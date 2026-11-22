import { ref } from 'vue'
import { api } from './api'
import { authState } from './auth'

/** 全局未读数：导航栏红点用 */
export const unread = ref(0)

export async function refreshUnread() {
  if (!authState.token) {
    unread.value = 0
    return
  }
  try {
    unread.value = (await api.unreadCount()) || 0
  } catch (e) {
    unread.value = 0
  }
}
