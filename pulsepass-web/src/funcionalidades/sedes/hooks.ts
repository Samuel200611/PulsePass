import { z } from 'zod'
import { useQuery } from '@tanstack/react-query'
import { http } from '../../lib/http'

export const SedeSchema = z.object({
  id: z.number(), code: z.string(), name: z.string(), city: z.string(),
  address: z.string().nullish(), capacity: z.number(), active: z.boolean(),
})

export const useSedesActivas = () =>
  useQuery({ queryKey: ['sedes', 'activas'], queryFn: () => http.get('/venues/active', z.array(SedeSchema)), staleTime: 300_000 })

export const useSede = (code: string) =>
  useQuery({ queryKey: ['sede', code], queryFn: () => http.get(`/venues/${encodeURIComponent(code)}`, SedeSchema), staleTime: 300_000 })
