import { z } from 'zod'

const CLAVE = 'pulsepass.eventosCreados'
const ListaSchema = z.array(z.object({ eventCode: z.string(), name: z.string() }))
export type EventoCreado = z.infer<typeof ListaSchema>[number]

/** GAP-01: la API no lista todos los eventos; se recuerdan los creados en esta pestaña (se pierde al cerrarla). */
export function leerEventosCreados(): EventoCreado[] {
  try {
    const r = ListaSchema.safeParse(JSON.parse(sessionStorage.getItem(CLAVE) ?? '[]'))
    return r.success ? r.data : []
  } catch {
    return []
  }
}

export function recordarEventoCreado(e: EventoCreado) {
  const resto = leerEventosCreados().filter((x) => x.eventCode !== e.eventCode)
  sessionStorage.setItem(CLAVE, JSON.stringify([e, ...resto]))
}
