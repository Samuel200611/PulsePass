import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http as msw, HttpResponse } from 'msw'
import { servidor } from './setup'
import { renderConProveedores } from './utils'
import Login from '../pages/Login'
import Registro from '../pages/Registro'

const respuestaLogin = { accessToken: 'jwt-de-prueba', tokenType: 'Bearer', expiresIn: 900 }
const me = { id: 1, username: 'andrea', email: 'andrea@example.test', roles: ['USER'] }

describe('Login', () => {
  it('login válido → sesión y redirección a la ruta de origen', async () => {
    let autorizacion: string | null = null
    servidor.use(
      msw.post('*/api/auth/login', () => HttpResponse.json(respuestaLogin)),
      msw.get('*/api/auth/me', ({ request }) => { autorizacion = request.headers.get('authorization'); return HttpResponse.json(me) }),
    )
    renderConProveedores(<Login />, { ruta: '/login', path: '/login', otras: { '/': 'Inicio' } })
    await userEvent.type(screen.getByLabelText('Usuario o correo'), 'andrea')
    await userEvent.type(screen.getByLabelText('Contraseña'), 'ClaveSegura-2026')
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }))
    expect(await screen.findByText('Inicio')).toBeInTheDocument()
    expect(autorizacion).toBe('Bearer jwt-de-prueba')
    expect(sessionStorage.getItem('pulsepass.sesion')).toContain('expiraEn')
  })

  it('401 → "Credenciales inválidas." genérico', async () => {
    servidor.use(msw.post('*/api/auth/login', () => HttpResponse.json({ status: 401, message: 'Bad credentials' }, { status: 401 })))
    renderConProveedores(<Login />, { ruta: '/login', path: '/login' })
    await userEvent.type(screen.getByLabelText('Usuario o correo'), 'andrea')
    await userEvent.type(screen.getByLabelText('Contraseña'), 'incorrecta')
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Credenciales inválidas.')
  })

  it('campos vacíos → errores y 0 peticiones', async () => {
    const peticiones = vi.fn()
    servidor.use(msw.post('*/api/auth/login', () => { peticiones(); return HttpResponse.json(respuestaLogin) }))
    renderConProveedores(<Login />, { ruta: '/login', path: '/login' })
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }))
    expect(await screen.findByText('Ingresa tu usuario o correo')).toBeInTheDocument()
    expect(peticiones).not.toHaveBeenCalled()
  })
})

describe('Registro', () => {
  async function llenar(password: string) {
    await userEvent.type(screen.getByLabelText('Nombre de usuario'), 'andrea')
    await userEvent.type(screen.getByLabelText('Correo electrónico'), 'andrea@example.test')
    await userEvent.type(screen.getByLabelText('Nombres'), 'Andrea')
    await userEvent.type(screen.getByLabelText('Apellidos'), 'Ruiz')
    await userEvent.type(screen.getByLabelText('Fecha de nacimiento'), '1998-04-12')
    await userEvent.type(screen.getByLabelText(/Contraseña/), password)
    await userEvent.click(screen.getByRole('button', { name: 'Crear cuenta' }))
  }

  it('contraseña de 11 caracteres → error y 0 peticiones', async () => {
    const peticiones = vi.fn()
    servidor.use(msw.post('*/api/users', () => { peticiones(); return HttpResponse.json({}) }))
    renderConProveedores(<Registro />, { ruta: '/registro', path: '/registro' })
    await llenar('12345678901')
    expect(await screen.findByText('Mínimo 12 caracteres')).toBeInTheDocument()
    expect(peticiones).not.toHaveBeenCalled()
  })

  it('201 → redirige a /login', async () => {
    servidor.use(msw.post('*/api/users', () => HttpResponse.json({ username: 'andrea', email: 'andrea@example.test' }, { status: 201 })))
    renderConProveedores(<Registro />, { ruta: '/registro', path: '/registro', otras: { '/login': 'Pantalla de login' } })
    await llenar('ClaveSegura-2026')
    expect(await screen.findByText('Pantalla de login')).toBeInTheDocument()
  })

  it('409 → mensaje genérico sin detalles internos', async () => {
    servidor.use(msw.post('*/api/users', () => HttpResponse.json({ status: 409, message: 'duplicate key users_email_key' }, { status: 409 })))
    renderConProveedores(<Registro />, { ruta: '/registro', path: '/registro' })
    await llenar('ClaveSegura-2026')
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('El usuario o correo ya está registrado.'))
    expect(screen.queryByText(/users_email_key/)).not.toBeInTheDocument()
  })
})
