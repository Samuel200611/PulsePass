import { Link } from 'react-router'
import { BadgeEstado } from './Badge'
import { Icono, type NombreIcono } from './Icono'
import { RUTAS } from '../lib/rutas'
import { formatearFecha } from '../lib/formato'
import type { EventCategory, EventoResumen } from '../funcionalidades/eventos/schemas'

export const CATEGORIAS: Record<EventCategory, { fondo: string; texto: string; icono: NombreIcono }> = {
  MUSIC: { fondo: 'bg-indigo-100 dark:bg-indigo-900/40', texto: 'text-indigo-800 dark:text-indigo-200', icono: 'musica' },
  TECHNOLOGY: { fondo: 'bg-sky-100 dark:bg-sky-900/40', texto: 'text-sky-800 dark:text-sky-200', icono: 'pantalla' },
  SPORTS: { fondo: 'bg-emerald-100 dark:bg-emerald-900/40', texto: 'text-emerald-800 dark:text-emerald-200', icono: 'balon' },
  CULTURE: { fondo: 'bg-rose-100 dark:bg-rose-900/40', texto: 'text-rose-800 dark:text-rose-200', icono: 'edificio' },
  EDUCATION: { fondo: 'bg-amber-100 dark:bg-amber-900/40', texto: 'text-amber-800 dark:text-amber-200', icono: 'birrete' },
  ENTERTAINMENT: { fondo: 'bg-violet-100 dark:bg-violet-900/40', texto: 'text-violet-800 dark:text-violet-200', icono: 'estrella' },
}

export function TarjetaEvento({ evento }: { evento: EventoResumen }) {
  const c = CATEGORIAS[evento.category]
  return (
    <Link to={RUTAS.evento(evento.eventCode)} className="block overflow-hidden rounded-xl bg-white shadow-sm transition-shadow hover:shadow-md focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-marca-600 dark:bg-slate-900">
      <div className={`flex h-32 items-center justify-center ${c.fondo} ${c.texto}`}><Icono nombre={c.icono} className="size-10" /></div>
      <div className="space-y-1 p-4">
        <p className={`text-xs font-semibold ${c.texto}`}>{evento.category}</p>
        <h3 className="font-titulo text-base font-semibold">{evento.name}</h3>
        <p className="flex items-center gap-1.5 text-sm text-slate-600 dark:text-slate-400"><Icono nombre="calendario" className="size-4" />{formatearFecha(evento.eventDate)}</p>
        <p className="flex items-center gap-1.5 text-sm text-slate-600 dark:text-slate-400"><Icono nombre="pin" className="size-4" />{evento.venueName}</p>
        <div className="pt-1"><BadgeEstado estado={evento.status} /></div>
      </div>
    </Link>
  )
}
