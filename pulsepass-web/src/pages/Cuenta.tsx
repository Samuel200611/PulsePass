import { useNavigate } from 'react-router'
import { useAuth } from '../funcionalidades/auth/AuthContext'
import { Boton } from '../componentes/Boton'
import { RUTAS } from '../lib/rutas'
import { iniciales } from '../lib/formato'

export default function Cuenta() {
  const { sesion, cerrarSesion } = useAuth()
  const navigate = useNavigate()
  if (sesion.estado !== 'autenticado') return null
  const { usuario } = sesion
  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <h1 className="font-titulo text-3xl font-bold">Mi cuenta</h1>
      <div className="flex items-center gap-4 rounded-xl bg-white p-5 shadow-sm dark:bg-slate-900">
        <span className="grid size-14 place-items-center rounded-full bg-marca-50 text-lg font-bold text-marca-700">{iniciales(usuario.username)}</span>
        <div className="flex-1"><p className="font-titulo text-lg font-semibold">@{usuario.username}</p><p className="text-sm text-slate-600 dark:text-slate-400">{usuario.email}</p></div>
        <p className="text-sm">Rol: <strong>{usuario.roles.join(', ')}</strong></p>
      </div>
      <Boton variante="secundario" onClick={() => { cerrarSesion(); void navigate(RUTAS.inicio) }}>Cerrar sesión</Boton>
    </div>
  )
}
