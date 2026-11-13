import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { eventosApi } from './api'

export const useEventosPublicados = () =>
  useQuery({ queryKey: ['eventos', 'publicados'], queryFn: eventosApi.publicados, staleTime: 60_000 })

export const useEventosPorArtista = (stageName: string) =>
  useQuery({ queryKey: ['eventos', 'artista', stageName], queryFn: () => eventosApi.porArtista(stageName), staleTime: 60_000, enabled: stageName !== '' })

export const useEvento = (eventCode: string) =>
  useQuery({ queryKey: ['evento', eventCode], queryFn: () => eventosApi.porCodigo(eventCode), staleTime: 30_000 })

export function useCrearEvento() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: eventosApi.crear,
    onSuccess: (e) => qc.setQueryData(['evento', e.eventCode], e), // siembra la caché con el resultado
  })
}

export function usePublicarEvento(eventCode: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () => eventosApi.publicar(eventCode),
    onSuccess: () => Promise.all([
      qc.invalidateQueries({ queryKey: ['evento', eventCode] }),
      qc.invalidateQueries({ queryKey: ['eventos', 'publicados'] }),
    ]),
  })
}

export function useAsociarArtista(eventCode: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (artistId: number) => eventosApi.asociarArtista(eventCode, artistId),
    onSuccess: () => Promise.all([
      qc.invalidateQueries({ queryKey: ['evento', eventCode] }),
      qc.invalidateQueries({ queryKey: ['eventos', 'artista'] }),
    ]),
  })
}
