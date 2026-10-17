import { onMounted, onUnmounted } from 'vue'

const eventName = 'workos:capture-created'
const channel = typeof BroadcastChannel === 'undefined' ? null : new BroadcastChannel('workos-changes')
channel?.addEventListener('message', () => {
  window.dispatchEvent(new Event(eventName))
  window.dispatchEvent(new Event('workos:attention-changed'))
})

export function notifyCaptureCreated() {
  window.dispatchEvent(new Event(eventName))

}

export function useCaptureRefresh(refresh: () => void | Promise<void>) {
  const listener = () => { void refresh() }
  onMounted(() => window.addEventListener(eventName, listener))
  onUnmounted(() => window.removeEventListener(eventName, listener))
}

export function notifyWorkspaceChanged() {
  channel?.postMessage('changed')
  window.dispatchEvent(new Event('workos:attention-changed'))
}
