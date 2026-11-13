import { Link, useParams } from 'react-router'
import { useEvento } from '../funcionalidades/eventos/hooks'
import { PanelCompra } from '../funcionalidades/tickets/componentes/PanelCompra'
import { ApiError } from '../lib/http'
import { BadgeEstado } from '../componentes/Badge'
import { CATEGORIAS } from '../componentes/TarjetaEvento'
import { Icono } from '../componentes/Icono'
import { Cargando, EstadoError } from '../componentes/Estados'
import { formatearFechaHora, iniciales } from '../lib/formato'
import { RUTAS } from '../lib/rutas'

export default function DetalleEvento() {
  const { eventCode = '' } = useParams()
  const { data: evento, isPending, error, refetch } = useEvento(eventCode)

  if (isPending) return <Cargando />
  if (error instanceof ApiError && (error.status === 404 || error.status === 403)) {
    return <EstadoError mensaje="Evento no disponible" />
  }
  if (error) return <EstadoError mensaje={error.message} onReintentar={() => void refetch()} />

  const c = CATEGORIAS[evento.category]
  return (
    <div className="space-y-4 pb-24 md:pb-0">
      <Link to={RUTAS.eventos} className="text-sm font-medium text-marca-600 dark:text-marca-500">← Volver a la cartelera</Link>
      <div className="grid gap-6 md:grid-cols-[2fr_1fr]">
        <article className="overflow-hidden rounded-xl bg-white shadow-sm dark:bg-slate-900">
          <div className={`flex h-48 items-center justify-center ${c.fondo} ${c.texto}`}><Icono nombre={c.icono} className="size-14" /></div>
          <div className="space-y-4 p-6">
            <div className="flex items-center gap-2"><span className={`text-xs font-semibold ${c.texto}`}>{evento.category}</span><BadgeEstado estado={evento.status} /></div>
            <h1 className="font-titulo text-3xl font-bold">{evento.name}</h1>
            <p className="flex flex-wrap gap-x-4 gap-y-1 text-sm text-slate-600 dark:text-slate-400">
              <span className="flex items-center gap-1.5"><Icono nombre="calendario" className="size-4" />{formatearFechaHora(evento.eventDate)}</span>
              <span>Edad mínima: {evento.minimumAge}</span>
            </p>
            {evento.description && <p>{evento.description}</p>}
            <section aria-labelledby="artistas-evento">
              <h2 id="artistas-evento" className="mb-2 font-titulo text-lg font-semibold">Artistas</h2>
              {evento.artists.length === 0 ? <p className="text-sm text-slate-600 dark:text-slate-400">Aún sin artistas asociados.</p> : (
                <ul className="flex flex-wrap gap-3">{evento.artists.map((a) => (
                  <li key={a.id}><Link to={RUTAS.artista(a.id)} className="flex items-center gap-2 text-sm hover:underline"><span className="grid size-9 place-items-center rounded-full bg-marca-50 text-xs font-bold text-marca-700">{iniciales(a.stageName)}</span>{a.stageName}</Link></li>
                ))}</ul>
              )}
            </section>
            <section aria-labelledby="sede-evento">
              <h2 id="sede-evento" className="mb-2 font-titulo text-lg font-semibold">Sede</h2>
              <Link to={RUTAS.sede(evento.venueCode)} className="flex items-center gap-3 rounded-lg border border-slate-200 p-3 dark:border-slate-700">
                <Icono nombre="edificio" /><span><span className="block font-semibold">{evento.venueName}</span><span className="text-sm text-slate-600 dark:text-slate-400">{evento.venueCode}</span></span>
              </Link>
            </section>
          </div>
        </article>

        <PanelCompra evento={evento} />
      </div>
    </div>
  )
}
