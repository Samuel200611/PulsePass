import { QueryClient } from '@tanstack/react-query'
import { ApiError } from './http'

/** 4xx no se reintenta; red y 5xx, una vez. Las mutaciones nunca se reintentan. */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: (intentos, error) => intentos < 1 && (!(error instanceof ApiError) || error.status === 0 || error.status >= 500),
    },
    mutations: { retry: false },
  },
})
