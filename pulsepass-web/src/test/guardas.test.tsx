import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import type { ReactNode } from 'react'
import { AuthContext, type Sesion } from '../funcionalidades/auth/AuthContext'
import type { Rol } from '../funcionalidades/auth/schemas'
import RutaProtegida from '../layouts/RutaProtegida'
import RutaPorRol from '../layouts/RutaPorRol'

const conSesion = (roles: Rol[] | null): Sesion =>
  roles ? { estado: 'autenticado', token: 't', expiraEn: Date.now() + 1e6, usuario: { id: 1, username: 'u', email: 'u@x.test', roles } } : { estado: 'anonimo' }

function pintar(sesion: Sesion, guarda: ReactNode) {
  const valor = {
    sesion, iniciarSesion: async () => undefined, cerrarSesion: () => undefined,
    tieneRol: (...r: Rol[]) => sesion.estado === 'autenticado' && r.some((x) => sesion.usuario.roles.includes(x)),
  }
  render(
    <AuthContext.Provider value={valor}>
      <MemoryRouter initialEntries={['/admin']}>
        <Routes>
          <Route element={guarda}><Route path="/admin" element={<p>Panel</p>} /></Route>
          <Route path="/login" element={<p>Pantalla de login</p>} />
          <Route path="/403" element={<p>Sin permisos</p>} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

describe('guardas de ruta', () => {
  it('RutaProtegida: anónimo → /login', () => { pintar(conSesion(null), <RutaProtegida />); expect(screen.getByText('Pantalla de login')).toBeInTheDocument() })
  it('RutaProtegida: autenticado → renderiza', () => { pintar(conSesion(['USER']), <RutaProtegida />); expect(screen.getByText('Panel')).toBeInTheDocument() })
  it('RutaPorRol: USER en ruta ADMIN → /403', () => { pintar(conSesion(['USER']), <RutaPorRol roles={['ADMIN']} />); expect(screen.getByText('Sin permisos')).toBeInTheDocument() })
  it('RutaPorRol: ADMIN en ruta ADMIN → renderiza', () => { pintar(conSesion(['ADMIN']), <RutaPorRol roles={['ADMIN']} />); expect(screen.getByText('Panel')).toBeInTheDocument() })
  it('RutaPorRol: anónimo → /login', () => { pintar(conSesion(null), <RutaPorRol roles={['STAFF', 'ADMIN']} />); expect(screen.getByText('Pantalla de login')).toBeInTheDocument() })
})
