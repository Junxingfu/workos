import { onMounted, onUnmounted } from 'vue'

const eventName = 'workos:capture-created'

export function notifyCaptureCreated() {
  window.dispatchEvent(new Event(eventName))
}

export function useCaptureRefresh(refresh: () => void | Promise<void>) {
  const listener = () => { void refresh() }
  onMounted(() => window.addEventListener(eventName, listener))
  onUnmounted(() => window.removeEventListener(eventName, listener))
}
