import { Link, useParams } from 'react-router'
import { useSede } from '../funcionalidades/sedes/hooks'
import { ApiError } from '../lib/http'
import { RUTAS } from '../lib/rutas'
import { Cargando, EstadoError } from '../componentes/Estados'
import NoEncontrada from './NoEncontrada'

export default function DetalleSede() {
  const { code = '' } = useParams()
  const { data: s, isPending, error, refetch } = useSede(code)
  if (isPending) return <Cargando />
  if (error instanceof ApiError && error.status === 404) return <NoEncontrada />
  if (error) return <EstadoError mensaje={error.message} onReintentar={() => void refetch()} />

  const filas: ReadonlyArray<[string, string]> = [
    ['Código', s.code], ['Ciudad', s.city], ...(s.address ? [['Dirección', s.address] as [string, string]] : []),
    ['Capacidad', `${s.capacity} personas`], ['Estado', s.active ? 'Activa' : 'Inactiva'],
  ]
  return (
    <div className="mx-auto max-w-2xl space-y-4">
      <Link to={RUTAS.sedes} className="text-sm font-medium text-marca-600 dark:text-marca-500">← Sedes</Link>
      <article className="space-y-4 rounded-2xl bg-white p-6 shadow-sm dark:bg-slate-900">
        <h1 className="font-titulo text-2xl font-bold">{s.name}</h1>
        <dl className="divide-y divide-slate-200 text-sm dark:divide-slate-700">
          {filas.map(([k, v]) => <div key={k} className="flex justify-between py-2"><dt className="text-slate-600 dark:text-slate-400">{k}</dt><dd className="font-medium">{v}</dd></div>)}
        </dl>
      </article>
    </div>
  )
}
