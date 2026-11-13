import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { setToken, setUnauthorizedHandler } from '../../lib/http'
import { authApi } from './api'
import { SesionGuardadaSchema, type LoginForm, type Rol, type UsuarioAutenticado } from './schemas'

export type Sesion =
  | { estado: 'cargando' }
  | { estado: 'anonimo' }
  | { estado: 'autenticado'; token: string; expiraEn: number; usuario: UsuarioAutenticado }

interface AuthValor {
  sesion: Sesion
  iniciarSesion: (datos: LoginForm) => Promise<void>
  cerrarSesion: () => void
  tieneRol: (...roles: Rol[]) => boolean
}

export const AuthContext = createContext<AuthValor | null>(null)
const CLAVE = 'pulsepass.sesion'

function leerGuardada() {
  try {
    const bruto = sessionStorage.getItem(CLAVE)
    const r = SesionGuardadaSchema.safeParse(bruto ? JSON.parse(bruto) : null)
    return r.success && r.data.expiraEn > Date.now() ? r.data : null
  } catch {
    return null
  }
}

export function AuthProvider({ children, onSesionExpirada }: { children: ReactNode; onSesionExpirada?: () => void }) {
  const queryClient = useQueryClient()
  const [sesion, setSesion] = useState<Sesion>(() => (leerGuardada() ? { estado: 'cargando' } : { estado: 'anonimo' }))

  const cerrarSesion = useCallback(() => {
    setToken(null)
    sessionStorage.removeItem(CLAVE)
    sessionStorage.removeItem('pulsepass.eventosCreados')
    queryClient.clear()
    setSesion({ estado: 'anonimo' })
  }, [queryClient])

  // Restaurar la sesión al recargar: el token se valida (zod + expiración) y el rol sale de /auth/me.
  useEffect(() => {
    const guardada = leerGuardada()
    if (!guardada) return
    let activo = true
    setToken(guardada.token)
    authApi.me().then(
      (usuario) => { if (activo) setSesion({ estado: 'autenticado', ...guardada, usuario }) },
      () => { if (activo) { setToken(null); sessionStorage.removeItem(CLAVE); setSesion({ estado: 'anonimo' }) } },
    )
    return () => { activo = false }
  }, [])

  // Cualquier 401 posterior al login cierra la sesión local y avisa para redirigir.
  useEffect(() => {
    setUnauthorizedHandler(() => { cerrarSesion(); onSesionExpirada?.() })
    return () => setUnauthorizedHandler(null)
  }, [cerrarSesion, onSesionExpirada])

  // Cierre local al vencer el token.
  const expiraEn = sesion.estado === 'autenticado' ? sesion.expiraEn : null
  useEffect(() => {
    if (expiraEn === null) return
    const t = setTimeout(() => { cerrarSesion(); onSesionExpirada?.() }, Math.max(expiraEn - Date.now(), 0))
    return () => clearTimeout(t)
  }, [expiraEn, cerrarSesion, onSesionExpirada])

  const login = useMutation({
    mutationFn: async (datos: LoginForm) => {
      const r = await authApi.login(datos)
      setToken(r.accessToken)
      try {
        return { r, usuario: await authApi.me() }
      } catch (e) {
        setToken(null)
        throw e
      }
    },
  })

  const iniciarSesion = useCallback(async (datos: LoginForm) => {
    const { r, usuario } = await login.mutateAsync(datos)
    const guardada = { token: r.accessToken, expiraEn: Date.now() + r.expiresIn * 1000 }
    sessionStorage.setItem(CLAVE, JSON.stringify(guardada))
    setSesion({ estado: 'autenticado', ...guardada, usuario })
  }, [login])

  const valor = useMemo<AuthValor>(() => ({
    sesion,
    iniciarSesion,
    cerrarSesion,
    tieneRol: (...roles) => sesion.estado === 'autenticado' && roles.some((r) => sesion.usuario.roles.includes(r)),
  }), [sesion, iniciarSesion, cerrarSesion])

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider')
  return ctx
}
