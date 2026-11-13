import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../auth/AuthContext'
import { ticketsApi } from './api'
import type { TicketType } from './schemas'

/** El email del comprador sale SIEMPRE de la sesión, nunca de un campo editable. */
const useEmailSesion = () => {
  const { sesion } = useAuth()
  return sesion.estado === 'autenticado' ? sesion.usuario.email : null
}

export function useMisTickets() {
  const email = useEmailSesion()
  return useQuery({ queryKey: ['tickets', 'usuario', email], queryFn: () => ticketsApi.porUsuario(email ?? ''), enabled: email !== null, staleTime: 15_000 })
}

export const useTicket = (code: string) =>
  useQuery({ queryKey: ['ticket', code], queryFn: () => ticketsApi.porCodigo(code), staleTime: 15_000 })

export function useComprarTicket(eventCode: string) {
  const email = useEmailSesion()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (type: TicketType) => {
      if (email === null) throw new Error('Sin sesión')
      return ticketsApi.comprar({ userEmail: email, eventCode, type })
    },
    onSuccess: () => Promise.all([
      qc.invalidateQueries({ queryKey: ['tickets', 'usuario', email] }),
      qc.invalidateQueries({ queryKey: ['evento', eventCode] }),
    ]),
  })
}

export function useCancelarTicket() {
  const email = useEmailSesion()
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ticketsApi.cancelar,
    onSuccess: (t) => Promise.all([
      qc.invalidateQueries({ queryKey: ['tickets', 'usuario', email] }),
      qc.invalidateQueries({ queryKey: ['ticket', t.ticketCode] }),
    ]),
  })
}

/** Consulta bajo demanda para la puerta: siempre dato fresco. */
export const useBuscarTicket = (code: string) =>
  useQuery({ queryKey: ['ticket', code], queryFn: () => ticketsApi.porCodigo(code), enabled: code !== '', staleTime: 0 })

export const useTicketsPagadosPorEvento = (eventCode: string) =>
  useQuery({ queryKey: ['tickets', 'pagados', eventCode], queryFn: () => ticketsApi.pagadosPorEvento(eventCode), enabled: eventCode !== '', staleTime: 15_000 })

export function useUsarTicket() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ticketsApi.usar,
    onSuccess: (t) => Promise.all([
      qc.invalidateQueries({ queryKey: ['ticket', t.ticketCode] }),
      qc.invalidateQueries({ queryKey: ['tickets', 'pagados'] }),
    ]),
  })
}
