import { z } from 'zod'

const EstadoNavSchema = z.object({
  desde: z.string().startsWith('/').optional(),
  registrado: z.boolean().optional(),
  motivo: z.literal('expirada').optional(),
})
export type EstadoNav = z.infer<typeof EstadoNavSchema>

/** El state de la navegación es un dato externo: se valida antes de usarlo. */
export function leerEstadoNav(state: unknown): EstadoNav {
  const r = EstadoNavSchema.safeParse(state)
  return r.success ? r.data : {}
}
