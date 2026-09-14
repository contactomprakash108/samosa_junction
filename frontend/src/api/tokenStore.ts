
const TOKEN_KEY = 'samosa.accessToken'

type Listener = () => void

const listeners = new Set<Listener>()

function notify() {
  listeners.forEach((listener) => listener())
}

export const tokenStore = {
  get(): string | null {
    return window.localStorage.getItem(TOKEN_KEY)
  },

  set(token: string) {
    window.localStorage.setItem(TOKEN_KEY, token)
    notify()
  },

  clear() {
    window.localStorage.removeItem(TOKEN_KEY)
    notify()
  },

  subscribe(listener: Listener) {
    listeners.add(listener)
    return () => {
      listeners.delete(listener)
    }
  },
}
