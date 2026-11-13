import { Link, useParams } from 'react-router'
import { useArtista } from '../funcionalidades/artistas/hooks'
import { useEventosPorArtista } from '../funcionalidades/eventos/hooks'
import { ApiError } from '../lib/http'
import { iniciales } from '../lib/formato'
import { RUTAS } from '../lib/rutas'
import { TarjetaEvento } from '../componentes/TarjetaEvento'
import { Cargando, EstadoError, EstadoVacio } from '../componentes/Estados'
import NoEncontrada from './NoEncontrada'

export default function DetalleArtista() {
  const { id = '' } = useParams()
  const numero = /^\d+$/.test(id) ? Number(id) : Number.NaN // un id no numérico es 404 de UI, sin llamar a la API
  const artista = useArtista(numero)
  const eventos = useEventosPorArtista(artista.data?.stageName ?? '')

  if (Number.isNaN(numero)) return <NoEncontrada />
  if (artista.isPending) return <Cargando />
  if (artista.error instanceof ApiError && artista.error.status === 404) return <NoEncontrada />
  if (artista.error) return <EstadoError mensaje={artista.error.message} onReintentar={() => void artista.refetch()} />
  const a = artista.data

  return (
    <div className="space-y-6">
      <Link to={RUTAS.artistas} className="text-sm font-medium text-marca-600 dark:text-marca-500">← Artistas</Link>
      <div className="flex items-center gap-5 rounded-2xl bg-white p-6 shadow-sm dark:bg-slate-900">
        <span className="grid size-24 shrink-0 place-items-center rounded-full bg-marca-50 text-3xl font-bold text-marca-700">{iniciales(a.stageName)}</span>
        <div className="space-y-1">
          <h1 className="font-titulo text-3xl font-bold">{a.stageName}</h1>
          <p className="text-sm text-slate-600 dark:text-slate-400">Nombre artístico{a.genre ? ` · ${a.genre}` : ''}{a.country ? ` · ${a.country}` : ''}</p>
          <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold ${a.active ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-200' : 'bg-slate-200 text-slate-700 dark:bg-slate-700 dark:text-slate-100'}`}>{a.active ? 'Activo' : 'Inactivo'}</span>
        </div>
      </div>
      <section aria-labelledby="proximos-artista" className="space-y-3">
        <h2 id="proximos-artista" className="font-titulo text-xl font-semibold">Próximos eventos de {a.stageName}</h2>
        {eventos.isPending ? <Cargando />
          : eventos.error ? <EstadoError mensaje={eventos.error.message} onReintentar={() => void eventos.refetch()} />
          : eventos.data.length === 0 ? <EstadoVacio mensaje="Sin eventos próximos" />
          : <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">{eventos.data.map((e) => <TarjetaEvento key={e.eventCode} evento={e} />)}</div>}
      </section>
    </div>
  )
}
