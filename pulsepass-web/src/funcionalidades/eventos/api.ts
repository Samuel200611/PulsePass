import { z } from 'zod'
import { http } from '../../lib/http'
import { EventoResumenSchema, EventoSchema, type CrearEventoForm } from './schemas'

export const eventosApi = {
  publicados: () => http.get('/events/published', z.array(EventoResumenSchema)),
  porArtista: (stageName: string) => http.get('/events/by-artist', z.array(EventoResumenSchema), { stageName }),
  crear: (d: CrearEventoForm) => http.post('/events', EventoSchema, { ...d, description: d.description.trim() || null, eventDate: d.eventDate.length === 16 ? `${d.eventDate}:00` : d.eventDate }),
  publicar: (eventCode: string) => http.patch(`/events/${encodeURIComponent(eventCode)}/publish`, EventoSchema),
  asociarArtista: (eventCode: string, artistId: number) => http.post(`/events/${encodeURIComponent(eventCode)}/artists/${artistId}`, EventoSchema, undefined),
  porCodigo: (eventCode: string) => http.get(`/events/${encodeURIComponent(eventCode)}`, EventoSchema),
}
