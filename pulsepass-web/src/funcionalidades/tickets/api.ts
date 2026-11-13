import { z } from 'zod'
import { http } from '../../lib/http'
import { TicketSchema, type TicketType } from './schemas'

export const ticketsApi = {
  comprar: (datos: { userEmail: string; eventCode: string; type: TicketType }) => http.post('/tickets', TicketSchema, datos),
  porUsuario: (email: string) => http.get('/tickets/by-user', z.array(TicketSchema), { email }),
  porCodigo: (code: string) => http.get(`/tickets/${encodeURIComponent(code)}`, TicketSchema),
  usar: (code: string) => http.patch(`/tickets/${encodeURIComponent(code)}/use`, TicketSchema),
  pagadosPorEvento: (eventCode: string) => http.get(`/events/${encodeURIComponent(eventCode)}/tickets/paid`, z.array(TicketSchema)),
  cancelar: (code: string) => http.patch(`/tickets/${encodeURIComponent(code)}/cancel`, TicketSchema),
}
