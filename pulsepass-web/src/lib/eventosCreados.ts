import { z } from 'zod'
import { EventStatusSchema } from '../funcionalidades/eventos/schemas'

// GAP-01: la API no lista todos los eventos; se recuerdan los creados en esta pestaña (sessionStorage).
const Esquema = z.array(z.object({ eventCode: z.string(), name: z.string(), status: EventStatusSchema }))
export type EventoCreado = z.infer<typeof Esquema>[number]
const CLAVE = 'pulsepass.eventos-creados'

export function leerEventosCreados(): EventoCreado[] {
  try {
    const r = Esquema.safeParse(JSON.parse(sessionStorage.getItem(CLAVE) ?? '[]'))
    return r.success ? r.data : []
  } catch { return [] }
}

export function guardarEventoCreado(e: EventoCreado) {
  const resto = leerEventosCreados().filter((x) => x.eventCode !== e.eventCode)
  sessionStorage.setItem(CLAVE, JSON.stringify([e, ...resto]))
}
