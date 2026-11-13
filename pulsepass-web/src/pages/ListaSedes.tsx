import { useSedesActivas } from '../funcionalidades/sedes/hooks'
import { Cargando, EstadoError, EstadoVacio } from '../componentes/Estados'
import { Link } from 'react-router'
import { Icono } from '../componentes/Icono'
import { RUTAS } from '../lib/rutas'

export default function ListaSedes() {
  const { data, isPending, error, refetch } = useSedesActivas()
  return (
    <div className="space-y-6">
      <h1 className="font-titulo text-3xl font-bold">Sedes</h1>
      {isPending ? <Cargando /> : error ? <EstadoError mensaje={error.message} onReintentar={() => void refetch()} />
        : data.length === 0 ? <EstadoVacio mensaje="Aún no hay sedes activas" />
        : <ul className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">{data.map((s) => (
            <li key={s.code}><Link to={RUTAS.sede(s.code)} className="block space-y-1 rounded-xl bg-white p-4 shadow-sm hover:shadow-md focus-visible:outline-2 focus-visible:outline-marca-600 dark:bg-slate-900">
              <h2 className="font-titulo font-semibold">{s.name}</h2>
              <p className="flex items-center gap-1.5 text-sm text-slate-600 dark:text-slate-400"><Icono nombre="pin" className="size-4" />{s.city} · Cap. {s.capacity}</p>
            </Link></li>))}</ul>}
    </div>
  )
}
