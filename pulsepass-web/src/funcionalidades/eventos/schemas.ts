import { z } from 'zod'

export const EventCategorySchema = z.enum(['MUSIC', 'SPORTS', 'TECHNOLOGY', 'EDUCATION', 'CULTURE', 'ENTERTAINMENT'])
export const EventStatusSchema = z.enum(['DRAFT', 'PUBLISHED', 'SOLD_OUT', 'CANCELLED', 'FINISHED'])
export type EventCategory = z.infer<typeof EventCategorySchema>
export type EventStatus = z.infer<typeof EventStatusSchema>

export const ArtistaSchema = z.object({
  id: z.number(),
  stageName: z.string(),
  country: z.string().nullish(),
  genre: z.string().nullish(),
  active: z.boolean(),
})
export type Artista = z.infer<typeof ArtistaSchema>

export const EventoResumenSchema = z.object({
  id: z.number(),
  eventCode: z.string(),
  name: z.string(),
  category: EventCategorySchema,
  status: EventStatusSchema,
  eventDate: z.string(),
  venueName: z.string(),
})
export type EventoResumen = z.infer<typeof EventoResumenSchema>

export const EventoSchema = EventoResumenSchema.extend({
  description: z.string().nullish(),
  minimumAge: z.number(),
  venueCode: z.string(),
  artists: z.array(ArtistaSchema),
})
export type Evento = z.infer<typeof EventoSchema>

export const CrearEventoSchema = z.object({
  eventCode: z.string().trim().min(1, 'Ingresa el código del evento').max(30, 'Máximo 30 caracteres'),
  name: z.string().trim().min(1, 'Ingresa el nombre').max(150, 'Máximo 150 caracteres'),
  description: z.string().max(1000, 'Máximo 1000 caracteres'),
  category: EventCategorySchema,
  minimumAge: z.number('Ingresa la edad mínima').int('Debe ser un entero').min(0, 'No puede ser negativa').max(120, 'Máximo 120'),
  eventDate: z.string().min(1, 'Ingresa la fecha y hora'),
  venueCode: z.string().min(1, 'Selecciona una sede'),
})
export type CrearEventoForm = z.infer<typeof CrearEventoSchema>
