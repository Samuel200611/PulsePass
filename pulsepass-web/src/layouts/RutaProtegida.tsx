import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from '../funcionalidades/auth/AuthContext'
import { Cargando } from '../componentes/Estados'
import { RUTAS } from '../lib/rutas'

/** Solo experiencia de usuario: la seguridad real la aplica el servidor. */
export default function RutaProtegida() {
  const { sesion } = useAuth()
  const location = useLocation()
  if (sesion.estado === 'cargando') return <Cargando />
  if (sesion.estado === 'anonimo') return <Navigate to={RUTAS.login} replace state={{ desde: location.pathname }} />
  return <Outlet />
}
