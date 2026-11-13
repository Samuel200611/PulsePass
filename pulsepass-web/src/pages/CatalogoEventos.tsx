import { useSearchParams } from 'react-router'
import { useEventosPorArtista, useEventosPublicados } from '../funcionalidades/eventos/hooks'
import { useArtistasActivos } from '../funcionalidades/artistas/hooks'
import { EventCategorySchema } from '../funcionalidades/eventos/schemas'
import { TarjetaEvento } from '../componentes/TarjetaEvento'
import { Cargando, EstadoError, EstadoVacio } from '../componentes/Estados'
import { Boton } from '../componentes/Boton'

const CONTROL = 'min-h-11 w-full rounded-lg border border-slate-300 bg-white px-3 text-sm focus-visible:outline-2 focus-visible:outline-marca-600 dark:border-slate-700 dark:bg-slate-900'

export default function CatalogoEventos() {
  const [params, setParams] = useSearchParams()
  const q = params.get('q') ?? ''
  const categoria = EventCategorySchema.safeParse(params.get('categoria')).data
  const artista = params.get('artista') ?? ''

  const publicados = useEventosPublicados()
  const porArtista = useEventosPorArtista(artista)
  const artistas = useArtistasActivos()
  const consulta = artista ? porArtista : publicados

  const cambiar = (clave: string, valor: string) => setParams((p) => {
    const sig = new URLSearchParams(p)
    if (valor) sig.set(clave, valor); else sig.delete(clave)
    return sig
  }, { replace: true })

  const visibles = (consulta.data ?? []).filter((e) =>
    e.name.toLowerCase().includes(q.trim().toLowerCase()) && (!categoria || e.category === categoria))

  return (
    <div className="space-y-6">
      <h1 className="font-titulo text-3xl font-bold">Cartelera</h1>
      <form role="search" className="grid grid-cols-1 gap-3 md:grid-cols-[2fr_1fr_1fr_auto] md:items-end" onSubmit={(e) => e.preventDefault()}>
        <label className="text-sm font-medium">Buscar por nombre
          <input type="search" value={q} onChange={(e) => cambiar('q', e.target.value)} placeholder="Buscar por nombre…" className={`mt-1 ${CONTROL}`} />
        </label>
        <label className="text-sm font-medium">Categoría
          <select value={categoria ?? ''} onChange={(e) => cambiar('categoria', e.target.value)} className={`mt-1 ${CONTROL}`}>
            <option value="">Todas</option>
            {EventCategorySchema.options.map((c) => <option key={c} value={c}>{c}</option>)}
          </select>
        </label>
        <label className="text-sm font-medium">Artista
          <select value={artista} onChange={(e) => cambiar('artista', e.target.value)} className={`mt-1 ${CONTROL}`}>
            <option value="">Todos</option>
            {artistas.data?.map((a) => <option key={a.id} value={a.stageName}>{a.stageName}</option>)}
          </select>
        </label>
        <Boton type="button" variante="secundario" onClick={() => setParams({}, { replace: true })}>Limpiar</Boton>
      </form>

      {consulta.isPending ? <Cargando />
        : consulta.isError ? <EstadoError mensaje={consulta.error.message} onReintentar={() => void consulta.refetch()} />
        : visibles.length === 0 ? <EstadoVacio mensaje="Sin resultados para tu búsqueda" />
        : (
          <>
            <p className="text-sm text-slate-600 dark:text-slate-400" aria-live="polite">{visibles.length} {visibles.length === 1 ? 'evento' : 'eventos'}</p>
            <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">{visibles.map((e) => <TarjetaEvento key={e.eventCode} evento={e} />)}</div>
          </>
        )}
    </div>
  )
}
