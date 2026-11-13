import { useArtistasActivos } from '../funcionalidades/artistas/hooks'
import { Cargando, EstadoError, EstadoVacio } from '../componentes/Estados'
import { Link } from 'react-router'
import { iniciales } from '../lib/formato'
import { RUTAS } from '../lib/rutas'

export default function ListaArtistas() {
  const { data, isPending, error, refetch } = useArtistasActivos()
  return (
    <div className="space-y-6">
      <h1 className="font-titulo text-3xl font-bold">Artistas</h1>
      {isPending ? <Cargando /> : error ? <EstadoError mensaje={error.message} onReintentar={() => void refetch()} />
        : data.length === 0 ? <EstadoVacio mensaje="Aún no hay artistas activos" />
        : <ul className="grid grid-cols-2 gap-4 md:grid-cols-4">{data.map((a) => (
            <li key={a.id}><Link to={RUTAS.artista(a.id)} className="flex flex-col items-center gap-2 rounded-xl bg-white p-5 text-center shadow-sm hover:shadow-md focus-visible:outline-2 focus-visible:outline-marca-600 dark:bg-slate-900">
              <span className="grid size-16 place-items-center rounded-full bg-marca-50 text-xl font-bold text-marca-700">{iniciales(a.stageName)}</span>
              <span className="font-titulo font-semibold">{a.stageName}</span>
              {a.genre && <span className="text-sm text-slate-600 dark:text-slate-400">{a.genre}</span>}
            </Link></li>))}</ul>}
    </div>
  )
}
