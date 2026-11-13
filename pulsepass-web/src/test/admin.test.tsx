import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createMemoryRouter } from 'react-router'
import { RouterProvider } from 'react-router/dom'
import { http as msw, HttpResponse } from 'msw'
import { servidor } from './setup'
import { renderConProveedores } from './utils'
import AdminCrearEvento from '../pages/AdminCrearEvento'
import AdminGestionEvento from '../pages/AdminGestionEvento'
import AdminEventos from '../pages/AdminEventos'
import { AuthProvider } from '../funcionalidades/auth/AuthContext'
import { rutas } from '../router'

const evento = (o: Record<string, unknown> = {}) => ({ id: 1, eventCode: 'CMF-2026', name: 'Caribbean Music Fest 2026', category: 'MUSIC', status: 'DRAFT',
  eventDate: '2026-12-12T20:00:00', venueName: 'Marina Convention Center', description: null, minimumAge: 18, venueCode: 'VEN-SMR-01', artists: [], ...o })
const solar = { id: 1, stageName: 'Solar Beat', active: true }
const neon = { id: 2, stageName: 'Neon Waves', active: true }
const sede = { id: 1, code: 'VEN-SMR-01', name: 'Marina Convention Center', city: 'Santa Marta', capacity: 3, active: true }

function conSesion(roles: string[]) {
  sessionStorage.setItem('pulsepass.sesion', JSON.stringify({ token: 't', expiraEn: Date.now() + 1e6 }))
  servidor.use(msw.get('*/api/auth/me', () => HttpResponse.json({ id: 9, username: 'admin', email: 'admin@example.test', roles })))
}

describe('crear evento', () => {
  const montar = () => renderConProveedores(<AdminCrearEvento />, { ruta: '/admin/eventos/nuevo', path: '/admin/eventos/nuevo', otras: { '/admin/eventos/:eventCode': 'Pantalla de gestión' } })
  const llenar = async () => {
    await userEvent.type(screen.getByLabelText('Código del evento'), 'CMF-2026')
    await userEvent.type(screen.getByLabelText('Nombre'), 'Caribbean Music Fest 2026')
    await userEvent.type(screen.getByLabelText('Fecha y hora'), '2026-12-12T20:00')
    await userEvent.selectOptions(await screen.findByLabelText('Sede'), 'VEN-SMR-01')
  }
  beforeEach(() => servidor.use(msw.get('*/api/venues/active', () => HttpResponse.json([sede]))))

  it('campos vacíos → errores por campo y 0 peticiones', async () => {
    const peticiones = vi.fn()
    servidor.use(msw.post('*/api/events', () => { peticiones(); return HttpResponse.json(evento()) }))
    montar()
    await userEvent.click(screen.getByRole('button', { name: 'Crear evento' }))
    expect(await screen.findByText('Ingresa el código del evento')).toBeInTheDocument()
    expect(screen.getByText('Ingresa el nombre')).toBeInTheDocument()
    expect(screen.getByText('Selecciona una sede', { selector: 'p' })).toBeInTheDocument() // el mensaje, no la opción vacía
    expect(peticiones).not.toHaveBeenCalled()
  })

  it('201 → envía el cuerpo esperado y navega a la gestión', async () => {
    let cuerpo: unknown
    servidor.use(msw.post('*/api/events', async ({ request }) => { cuerpo = await request.json(); return HttpResponse.json(evento(), { status: 201 }) }))
    montar()
    await llenar()
    await userEvent.click(screen.getByRole('button', { name: 'Crear evento' }))
    expect(await screen.findByText('Pantalla de gestión')).toBeInTheDocument()
    expect(cuerpo).toMatchObject({ eventCode: 'CMF-2026', category: 'MUSIC', minimumAge: 0, venueCode: 'VEN-SMR-01', eventDate: '2026-12-12T20:00:00', description: null })
    expect(sessionStorage.getItem('pulsepass.eventosCreados')).toContain('CMF-2026')
  })

  it('400 con details → errores en los campos', async () => {
    servidor.use(msw.post('*/api/events', () => HttpResponse.json({ status: 400, message: 'inválido', details: { eventDate: 'La fecha del evento debe ser futura' } }, { status: 400 })))
    montar()
    await llenar()
    await userEvent.click(screen.getByRole('button', { name: 'Crear evento' }))
    expect(await screen.findByText('La fecha del evento debe ser futura')).toBeInTheDocument()
  })

  it('409 → aviso con el mensaje del backend', async () => {
    servidor.use(msw.post('*/api/events', () => HttpResponse.json({ status: 409, message: 'Ya existe un evento con ese código.' }, { status: 409 })))
    montar()
    await llenar()
    await userEvent.click(screen.getByRole('button', { name: 'Crear evento' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Ya existe un evento con ese código.')
  })
})

describe('gestionar evento', () => {
  const montar = () => renderConProveedores(<AdminGestionEvento />, { ruta: '/admin/eventos/CMF-2026', path: '/admin/eventos/:eventCode' })
  beforeEach(() => servidor.use(msw.get('*/api/artists/active', () => HttpResponse.json([solar, neon]))))

  it('publicar con confirmación → badge PUBLISHED y botón deshabilitado', async () => {
    let publicado = false
    servidor.use(
      msw.get('*/api/events/CMF-2026', () => HttpResponse.json(evento({ status: publicado ? 'PUBLISHED' : 'DRAFT' }))),
      msw.patch('*/api/events/CMF-2026/publish', () => { publicado = true; return HttpResponse.json(evento({ status: 'PUBLISHED' })) }),
    )
    montar()
    await userEvent.click(await screen.findByRole('button', { name: 'Publicar evento' }))
    await userEvent.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Publicar' }))
    expect(await screen.findByText('PUBLISHED')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Publicar evento' })).toBeDisabled()
  })

  it('publicar → 409 se muestra dentro del modal', async () => {
    servidor.use(
      msw.get('*/api/events/CMF-2026', () => HttpResponse.json(evento())),
      msw.patch('*/api/events/CMF-2026/publish', () => HttpResponse.json({ status: 409, message: 'El evento no puede publicarse en su estado actual.' }, { status: 409 })),
    )
    montar()
    await userEvent.click(await screen.findByRole('button', { name: 'Publicar evento' }))
    const modal = await screen.findByRole('dialog')
    await userEvent.click(within(modal).getByRole('button', { name: 'Publicar' }))
    expect(await within(modal).findByRole('alert')).toHaveTextContent('El evento no puede publicarse en su estado actual.')
  })

  it('evento ya publicado → publicar deshabilitado', async () => {
    servidor.use(msw.get('*/api/events/CMF-2026', () => HttpResponse.json(evento({ status: 'PUBLISHED' }))))
    montar()
    expect(await screen.findByRole('button', { name: 'Publicar evento' })).toBeDisabled()
  })

  it('asociar artista → aparece en la lista y deja de ofrecerse', async () => {
    let artists: unknown[] = [solar]
    servidor.use(
      msw.get('*/api/events/CMF-2026', () => HttpResponse.json(evento({ artists }))),
      msw.post('*/api/events/CMF-2026/artists/2', () => { artists = [solar, neon]; return HttpResponse.json(evento({ artists })) }),
    )
    montar()
    const lista = await screen.findByLabelText('Agregar artista')
    expect(within(lista).queryByRole('option', { name: 'Solar Beat' })).not.toBeInTheDocument() // ya asociado
    await userEvent.selectOptions(lista, 'Neon Waves')
    await userEvent.click(screen.getByRole('button', { name: '+ Asociar' }))
    await vi.waitFor(() => expect(screen.getAllByRole('listitem')).toHaveLength(2)) // Solar Beat + Neon Waves
    expect(within(screen.getByLabelText('Agregar artista')).queryByRole('option', { name: 'Neon Waves' })).not.toBeInTheDocument()
  })

  it('asociar → 409 duplicado muestra el mensaje', async () => {
    servidor.use(
      msw.get('*/api/events/CMF-2026', () => HttpResponse.json(evento())),
      msw.post('*/api/events/CMF-2026/artists/1', () => HttpResponse.json({ status: 409, message: 'El artista ya está asociado al evento.' }, { status: 409 })),
    )
    montar()
    await userEvent.selectOptions(await screen.findByLabelText('Agregar artista'), 'Solar Beat')
    await userEvent.click(screen.getByRole('button', { name: '+ Asociar' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('El artista ya está asociado al evento.')
  })

  it('evento inexistente → "Evento no encontrado"', async () => {
    servidor.use(msw.get('*/api/events/CMF-2026', () => HttpResponse.json({ status: 404, message: 'x' }, { status: 404 })))
    montar()
    expect(await screen.findByText('Evento no encontrado')).toBeInTheDocument()
  })
})

describe('administración de eventos', () => {
  const montar = () => renderConProveedores(<AdminEventos />, { ruta: '/admin/eventos', path: '/admin/eventos', otras: { '/admin/eventos/:eventCode': 'Pantalla de gestión' } })

  it('abre un evento existente por código', async () => {
    servidor.use(msw.get('*/api/events/CMF-2026', () => HttpResponse.json(evento())))
    montar()
    await userEvent.type(screen.getByLabelText('Buscar evento por código'), 'CMF-2026')
    await userEvent.click(screen.getByRole('button', { name: 'Abrir' }))
    expect(await screen.findByText('Pantalla de gestión')).toBeInTheDocument()
  })

  it('código inexistente → "Evento no encontrado"', async () => {
    servidor.use(msw.get('*/api/events/XX', () => HttpResponse.json({ status: 404, message: 'x' }, { status: 404 })))
    montar()
    await userEvent.type(screen.getByLabelText('Buscar evento por código'), 'XX')
    await userEvent.click(screen.getByRole('button', { name: 'Abrir' }))
    expect(await screen.findByText('Evento no encontrado')).toBeInTheDocument()
  })

  it('lista los eventos creados en esta sesión', () => {
    sessionStorage.setItem('pulsepass.eventosCreados', JSON.stringify([{ eventCode: 'CMF-2026', name: 'Caribbean Music Fest 2026' }]))
    montar()
    expect(screen.getByText('CMF-2026')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Gestionar' })).toBeInTheDocument()
  })
})

describe('acceso por rol (router real)', () => {
  const abrir = (ruta: string) => {
    const cliente = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(<QueryClientProvider client={cliente}><AuthProvider><RouterProvider router={createMemoryRouter(rutas, { initialEntries: [ruta] })} /></AuthProvider></QueryClientProvider>)
  }
  it.each([['USER'], ['STAFF']])('%s en /admin/eventos → 403', async (rol) => {
    conSesion([rol]); abrir('/admin/eventos')
    expect(await screen.findByText('No tienes permisos para acceder a esta sección.')).toBeInTheDocument()
  })
  it('ADMIN entra y ve "Administrar eventos" en el navbar', async () => {
    conSesion(['ADMIN']); abrir('/admin/eventos')
    expect(await screen.findByRole('heading', { name: 'Administración de eventos' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Administrar eventos' })).toBeInTheDocument()
  })
})
