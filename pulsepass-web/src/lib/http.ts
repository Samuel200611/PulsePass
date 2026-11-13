import { z } from 'zod'

export class ApiError extends Error {
  readonly status: number
  readonly details: Record<string, string>
  constructor(status: number, message: string, details: Record<string, string> = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.details = details
  }
}

const ErrorResponseSchema = z.object({
  status: z.number(),
  message: z.string(),
  details: z.record(z.string(), z.string()).nullish(),
})

const BASE: string = import.meta.env.VITE_API_BASE_URL ?? '/api'
let token: string | null = null
let alNoAutorizado: (() => void) | null = null

export const setToken = (t: string | null) => { token = t }
export const getToken = () => token
export const setUnauthorizedHandler = (h: (() => void) | null) => { alNoAutorizado = h }

const mensajeGenerico = (status: number) =>
  status >= 500 ? 'Ocurrió un error inesperado. Inténtalo más tarde.' : 'No se pudo completar la solicitud.'

async function aApiError(res: Response): Promise<ApiError> {
  try {
    const parsed = ErrorResponseSchema.safeParse(await res.json())
    if (parsed.success && res.status < 500) {
      return new ApiError(res.status, parsed.data.message, parsed.data.details ?? {})
    }
  } catch {
    /* cuerpo no JSON: se usa el mensaje genérico */
  }
  return new ApiError(res.status, mensajeGenerico(res.status))
}

interface Opciones {
  method?: 'GET' | 'POST' | 'PATCH'
  body?: unknown
  query?: Record<string, string>
  /** El 401 no cierra la sesión (p. ej. credenciales incorrectas en el login). */
  sin401Global?: boolean
}

export async function request<T>(path: string, schema: z.ZodType<T>, op: Opciones = {}): Promise<T> {
  const url = new URL(BASE + path, window.location.origin)
  for (const [k, v] of Object.entries(op.query ?? {})) url.searchParams.set(k, v)

  const headers: Record<string, string> = { Accept: 'application/json' }
  if (op.body !== undefined) headers['Content-Type'] = 'application/json'
  if (token) headers.Authorization = `Bearer ${token}`

  let res: Response
  try {
    res = await fetch(url, {
      method: op.method ?? 'GET',
      headers,
      body: op.body !== undefined ? JSON.stringify(op.body) : undefined,
    })
  } catch {
    throw new ApiError(0, 'No pudimos conectar con el servidor.')
  }

  if (!res.ok) {
    if (res.status === 401 && token && !op.sin401Global) alNoAutorizado?.()
    throw await aApiError(res)
  }

  const parsed = schema.safeParse(await res.json())
  if (!parsed.success) throw new ApiError(500, 'La respuesta del servidor no tiene el formato esperado.')
  return parsed.data
}

export const http = {
  get: <T>(path: string, schema: z.ZodType<T>, query?: Record<string, string>) => request(path, schema, { query }),
  post: <T>(path: string, schema: z.ZodType<T>, body: unknown, opciones: Omit<Opciones, 'method' | 'body'> = {}) =>
    request(path, schema, { ...opciones, method: 'POST', body }),
  patch: <T>(path: string, schema: z.ZodType<T>) => request(path, schema, { method: 'PATCH' }),
}
