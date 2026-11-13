import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from '../funcionalidades/auth/AuthContext'
import type { Rol } from '../funcionalidades/auth/schemas'
import { Cargando } from '../componentes/Estados'
import { RUTAS } from '../lib/rutas'

export default function RutaPorRol({ roles }: { roles: Rol[] }) {
  const { sesion, tieneRol } = useAuth()
  const location = useLocation()
  if (sesion.estado === 'cargando') return <Cargando />
  if (sesion.estado === 'anonimo') return <Navigate to={RUTAS.login} replace state={{ desde: location.pathname }} />
  if (!tieneRol(...roles)) return <Navigate to={RUTAS.sinPermisos} replace />
  return <Outlet />
}
