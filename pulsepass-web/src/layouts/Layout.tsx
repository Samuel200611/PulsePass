import { useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router'
import clsx from 'clsx'
import { useAuth } from '../funcionalidades/auth/AuthContext'
import { BotonTema } from '../componentes/BotonTema'
import { Icono } from '../componentes/Icono'
import { estilosBoton } from '../componentes/Boton'
import { RUTAS } from '../lib/rutas'

const ENLACES = [
  { a: RUTAS.eventos, texto: 'Eventos' },
  { a: RUTAS.artistas, texto: 'Artistas' },
  { a: RUTAS.sedes, texto: 'Sedes' },
]
const claseEnlace = ({ isActive }: { isActive: boolean }) =>
  clsx('rounded-md px-3 py-2 text-sm font-medium focus-visible:outline-2 focus-visible:outline-marca-600',
    isActive ? 'bg-marca-50 text-marca-700' : 'text-slate-600 hover:text-slate-900 dark:text-slate-300 dark:hover:text-white')

export default function Layout() {
  const { sesion, cerrarSesion, tieneRol } = useAuth()
  const enlaces: Array<{ a: string; texto: string }> = [
    ...ENLACES,
    ...(tieneRol('USER') ? [{ a: RUTAS.misTickets, texto: 'Mis tickets' }] : []),
    ...(tieneRol('STAFF', 'ADMIN') ? [{ a: RUTAS.validacion, texto: 'Validar ingreso' }, { a: RUTAS.ticketsPagados(), texto: 'Tickets pagados' }] : []),
    ...(tieneRol('ADMIN') ? [{ a: RUTAS.adminEventos, texto: 'Administrar eventos' }] : []),
  ]
  const navigate = useNavigate()
  const [abierto, setAbierto] = useState(false)
  const salir = () => { cerrarSesion(); setAbierto(false); void navigate(RUTAS.inicio) }

  const acciones = sesion.estado === 'autenticado' ? (
    <>
      <NavLink to={RUTAS.cuenta} className={claseEnlace} onClick={() => setAbierto(false)}>{sesion.usuario.username}</NavLink>
      <button type="button" onClick={salir} className={estilosBoton('secundario')}>Salir</button>
    </>
  ) : sesion.estado === 'anonimo' ? (
    <>
      <Link to={RUTAS.login} onClick={() => setAbierto(false)} className={estilosBoton('fantasma')}>Ingresar</Link>
      <Link to={RUTAS.registro} onClick={() => setAbierto(false)} className={estilosBoton('primario')}>Registrarse</Link>
    </>
  ) : null

  return (
    <div className="flex min-h-screen flex-col">
      <a href="#contenido" className="sr-only focus:not-sr-only focus:absolute focus:left-2 focus:top-2 focus:z-50 focus:rounded focus:bg-white focus:p-2">Saltar al contenido</a>
      <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/95 backdrop-blur dark:border-slate-800 dark:bg-slate-900/95">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-3">
          <Link to={RUTAS.inicio} className="flex items-center gap-2 font-titulo text-lg font-bold">
            <span className="grid size-8 place-items-center rounded-lg bg-marca-600 text-white"><Icono nombre="musica" className="size-5" /></span>PulsePass
          </Link>
          <nav aria-label="Principal" className="hidden items-center gap-1 md:flex">
            {enlaces.map((e) => <NavLink key={e.a} to={e.a} className={claseEnlace}>{e.texto}</NavLink>)}
          </nav>
          <div className="ml-auto hidden items-center gap-2 md:flex">{acciones}</div>
          <BotonTema />
          <button type="button" className="rounded-md p-2 md:hidden focus-visible:outline-2 focus-visible:outline-marca-600" aria-label={abierto ? 'Cerrar menú' : 'Abrir menú'} aria-expanded={abierto} onClick={() => setAbierto((v) => !v)}>
            <Icono nombre={abierto ? 'cerrar' : 'menu'} className="size-6" />
          </button>
        </div>
        {abierto && (
          <div className="space-y-2 border-t border-slate-200 px-4 py-3 md:hidden dark:border-slate-800">
            <nav aria-label="Móvil" className="flex flex-col">
              {enlaces.map((e) => <NavLink key={e.a} to={e.a} onClick={() => setAbierto(false)} className={claseEnlace}>{e.texto}</NavLink>)}
            </nav>
            <div className="flex gap-2">{acciones}</div>
          </div>
        )}
      </header>
      <main id="contenido" className="mx-auto w-full max-w-6xl flex-1 px-4 py-8"><Outlet /></main>
      <footer className="border-t border-slate-200 py-4 text-sm text-slate-600 dark:border-slate-800 dark:text-slate-400">
        <div className="mx-auto flex max-w-6xl justify-between px-4">
          <span>© 2026 PulsePass · Caso de estudio académico</span>
          <span className="flex gap-3"><Link to={RUTAS.sedes}>Sedes</Link><Link to={RUTAS.artistas}>Artistas</Link></span>
        </div>
      </footer>
    </div>
  )
}
