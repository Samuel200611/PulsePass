import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { setupServer } from 'msw/node'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { setToken, setUnauthorizedHandler } from '../lib/http'

export const servidor = setupServer()

beforeAll(() => servidor.listen({ onUnhandledFrame: 'error' }))
afterEach(() => {
  cleanup()
  servidor.resetHandlers()
  sessionStorage.clear()
  localStorage.clear()
  document.documentElement.classList.remove('dark')
  setToken(null)
  setUnauthorizedHandler(null)
})
afterAll(() => servidor.close())
