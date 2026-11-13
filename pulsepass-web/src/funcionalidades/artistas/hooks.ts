import { z } from 'zod'
import { useQuery } from '@tanstack/react-query'
import { http } from '../../lib/http'
import { ArtistaSchema } from '../eventos/schemas'

export const useArtistasActivos = () =>
  useQuery({ queryKey: ['artistas', 'activos'], queryFn: () => http.get('/artists/active', z.array(ArtistaSchema)), staleTime: 300_000 })

export const useArtista = (id: number) =>
  useQuery({ queryKey: ['artista', id], queryFn: () => http.get(`/artists/${id}`, ArtistaSchema), staleTime: 300_000, enabled: Number.isInteger(id) })
