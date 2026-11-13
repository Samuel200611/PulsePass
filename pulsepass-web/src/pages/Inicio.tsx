import { Link } from 'react-router'
import { useEventosPublicados } from '../funcionalidades/eventos/hooks'
import { useArtistasActivos } from '../funcionalidades/artistas/hooks'
import { useAuth } from '../funcionalidades/auth/AuthContext'
import { TarjetaEvento } from '../componentes/TarjetaEvento'
import { Cargando, EstadoError, EstadoVacio } from '../componentes/Estados'
import { estilosBoton } from '../componentes/Boton'
import { iniciales } from '../lib/formato'
import { RUTAS } from '../lib/rutas'

export default function Inicio() {
  const { sesion } = useAuth()
  const eventos = useEventosPublicados()
  const artistas = useArtistasActivos()
  return (
    <div className="space-y-10">
      <section className="rounded-2xl bg-linear-to-r from-marca-600 to-acento-500 px-6 py-12 text-white sm:px-10">
        <h1 className="max-w-md font-titulo text-4xl font-bold">Descubre los mejores eventos</h1>
        <p className="mt-3 max-w-md">Música, cultura, deporte y tecnología en un solo lugar.</p>
        <div className="mt-6 flex flex-col gap-3 sm:flex-row">
          <Link to={RUTAS.eventos} className={estilosBoton('secundario', 'border-transparent !text-marca-700')}>Ver cartelera</Link>
          {sesion.estado === 'anonimo' && <Link to={RUTAS.registro} className={estilosBoton('fantasma', 'border border-white/70 !text-white hover:!bg-white/10')}>Crear cuenta</Link>}
        </div>
      </section>

      <section aria-labelledby="proximos">
        <div className="mb-4 flex items-baseline justify-between">
          <h2 id="proximos" className="font-titulo text-xl font-semibold">Próximos eventos</h2>
          <Link to={RUTAS.eventos} className="text-sm font-medium text-marca-600 dark:text-marca-500">Ver todos</Link>
        </div>
        {eventos.isPending ? <Cargando />
          : eventos.isError ? <EstadoError mensaje={eventos.error.message} onReintentar={() => void eventos.refetch()} />
          : eventos.data.length === 0 ? <EstadoVacio mensaje="Aún no hay eventos publicados" accion={{ texto: 'Ver artistas', a: RUTAS.artistas }} />
          : <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">{eventos.data.slice(0, 3).map((e) => <TarjetaEvento key={e.eventCode} evento={e} />)}</div>}
      </section>

      <section aria-labelledby="destacados">
        <div className="mb-4 flex items-baseline justify-between">
          <h2 id="destacados" className="font-titulo text-xl font-semibold">Artistas destacados</h2>
          <Link to={RUTAS.artistas} className="text-sm font-medium text-marca-600 dark:text-marca-500">Ver todos</Link>
        </div>
        {artistas.data && (
          <ul className="flex flex-wrap gap-4">
            {artistas.data.slice(0, 6).map((a) => (
              <li key={a.id} className="flex items-center gap-2">
                <span className="grid size-11 place-items-center rounded-full bg-marca-50 text-sm font-bold text-marca-700">{iniciales(a.stageName)}</span>
                <span className="text-sm font-semibold">{a.stageName}</span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
