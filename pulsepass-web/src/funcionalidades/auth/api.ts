import { http } from '../../lib/http'
import { AuthResponseSchema, UsuarioAutenticadoSchema, UsuarioRegistradoSchema, type LoginForm, type RegistroForm } from './schemas'

export const authApi = {
  login: (datos: LoginForm) => http.post('/auth/login', AuthResponseSchema, datos, { sin401Global: true }),
  me: () => http.get('/auth/me', UsuarioAutenticadoSchema),
  registrar: (datos: RegistroForm) => http.post('/users', UsuarioRegistradoSchema, datos, { sin401Global: true }),
}
