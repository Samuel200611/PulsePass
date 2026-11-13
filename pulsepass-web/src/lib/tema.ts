import { z } from 'zod'

const TemaSchema = z.enum(['claro', 'oscuro'])
export type Tema = z.infer<typeof TemaSchema>
const CLAVE = 'pulsepass.tema'

/** Preferencia guardada (validada con zod) o, si no hay, la del sistema. */
export function temaInicial(): Tema {
  try {
    const guardado = TemaSchema.safeParse(localStorage.getItem(CLAVE))
    if (guardado.success) return guardado.data
  } catch { /* almacenamiento no disponible */ }
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'oscuro' : 'claro'
}

export function aplicarTema(tema: Tema) {
  document.documentElement.classList.toggle('dark', tema === 'oscuro')
  try { localStorage.setItem(CLAVE, tema) } catch { /* se ignora */ }
}
