import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createMemoryRouter } from 'react-router'
import { RouterProvider } from 'react-router/dom'
import { http as msw, HttpResponse } from 'msw'
import { servidor } from './setup'
import { renderConProveedores } from './utils'
import ValidacionIngreso from '../pages/ValidacionIngreso'
import TicketsPagadosEvento from '../pages/TicketsPagadosEvento'
import { AuthProvider } from '../funcionalidades/auth/AuthContext'
import { rutas } from '../router'
import type { Ticket } from '../funcionalidades/tickets/schemas'

const ticket = (o: Partial<Ticket> = {}): Ticket => ({ id: 1, ticketCode: 'TKT-8F2A1C', type: 'VIP', price: 250000, status: 'PAID',
  purchaseDate: '2026-11-05T14:32:00', userEmail: 'andrea@example.test', eventCode: 'CMF-2026', eventName: 'Caribbean Music Fest 2026', ...o })

function conSesion(roles: string[]) {
  sessionStorage.setItem('pulsepass.sesion', JSON.stringify({ token: 't', expiraEn: Date.now() + 1e6 }))
  servidor.use(msw.get('*/api/auth/me', () => HttpResponse.json({ id: 9, username: 'laura', email: 'laura@example.test', roles })))
}
const buscar = async (codigo: string) => {
  await userEvent.type(screen.getByLabelText('Código de ticket'), codigo)
  await userEvent.click(screen.getByRole('button', { name: 'Buscar' }))
}

describe('validación de ingreso', () => {
  const montar = () => renderConProveedores(<ValidacionIngreso />, { ruta: '/staff/validacion', path: '/staff/validacion' })

  it('código vacío → error de campo y 0 peticiones', async () => {
    const peticiones = vi.fn()
    servidor.use(msw.get('*/api/tickets/*', () => { peticiones(); return HttpResponse.json(ticket()) }))
    montar()
    await userEvent.click(screen.getByRole('button', { name: 'Buscar' }))
    expect(await screen.findByText('Ingresa el código del ticket')).toBeInTheDocument()
    expect(peticiones).not.toHaveBeenCalled()
  })

  it('busca y marca como usado → "Ingreso registrado." y badge USED', async () => {
    let usado = false
    servidor.use(
      msw.get('*/api/tickets/TKT-8F2A1C', () => HttpResponse.json(ticket({ status: usado ? 'USED' : 'PAID' }))),
      msw.patch('*/api/tickets/TKT-8F2A1C/use', () => { usado = true; return HttpResponse.json(ticket({ status: 'USED' })) }),
    )
    montar()
    await buscar('TKT-8F2A1C')
    await userEvent.click(await screen.findByRole('button', { name: 'Marcar como usado' }))
    expect(await screen.findByText('Ingreso registrado.')).toBeInTheDocument()
    expect(await screen.findByText('USED')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Marcar como usado' })).not.toBeInTheDocument()
    expect(screen.getByLabelText('Código de ticket')).toHaveValue('')
  })

  it('409 → advertencia con el mensaje del backend', async () => {
    servidor.use(
      msw.get('*/api/tickets/TKT-8F2A1C', () => HttpResponse.json(ticket())),
      msw.patch('*/api/tickets/TKT-8F2A1C/use', () => HttpResponse.json({ status: 409, message: 'El ticket ya fue utilizado.' }, { status: 409 })),
    )
    montar()
    await buscar('TKT-8F2A1C')
    await userEvent.click(await screen.findByRole('button', { name: 'Marcar como usado' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('El ticket ya fue utilizado.')
  })

  it.each([[404, 'Ticket no encontrado'], [403, 'Sin permisos']])('%i → "%s"', async (status, texto) => {
    servidor.use(msw.get('*/api/tickets/TKT-X', () => HttpResponse.json({ status, message: 'x' }, { status })))
    montar()
    await buscar('TKT-X')
    expect(await screen.findByText(texto)).toBeInTheDocument()
  })
})

describe('tickets pagados por evento', () => {
  const montar = () => renderConProveedores(<TicketsPagadosEvento />, { ruta: '/staff/eventos/CMF-2026/tickets', path: '/staff/eventos/:eventCode/tickets' })
  const dos = [ticket(), ticket({ id: 2, ticketCode: 'TKT-91B07D', type: 'GENERAL', price: 120000, userEmail: 'carlos@example.test' })]

  it('muestra la tabla con filas y el total', async () => {
    servidor.use(msw.get('*/api/events/CMF-2026/tickets/paid', () => HttpResponse.json(dos)))
    montar()
    const tabla = await screen.findByRole('table')
    expect(within(tabla).getAllByRole('row')).toHaveLength(3) // encabezado + 2
    expect(within(tabla).getByText('TKT-91B07D')).toBeInTheDocument()
    expect(screen.getByText('Total: 2')).toBeInTheDocument()
  })

  it('vacío → "No hay tickets pagados"; evento inexistente → 404', async () => {
    servidor.use(msw.get('*/api/events/CMF-2026/tickets/paid', () => HttpResponse.json([])))
    montar()
    expect(await screen.findByText('No hay tickets pagados')).toBeInTheDocument()
  })

  it('404 del evento → "Evento no encontrado"', async () => {
    servidor.use(msw.get('*/api/events/CMF-2026/tickets/paid', () => HttpResponse.json({ status: 404, message: 'x' }, { status: 404 })))
    montar()
    expect(await screen.findByText('Evento no encontrado')).toBeInTheDocument()
  })

  it('"Usar" marca el ticket y la lista se actualiza', async () => {
    let restantes = dos
    servidor.use(
      msw.get('*/api/events/CMF-2026/tickets/paid', () => HttpResponse.json(restantes)),
      msw.patch('*/api/tickets/TKT-8F2A1C/use', () => { restantes = dos.slice(1); return HttpResponse.json(ticket({ status: 'USED' })) }),
    )
    montar()
    const tabla = await screen.findByRole('table')
    await userEvent.click(within(within(tabla).getByText('TKT-8F2A1C').closest('tr')!).getByRole('button', { name: 'Usar' }))
    await screen.findByText('Total: 1')
    expect(within(screen.getByRole('table')).queryByText('TKT-8F2A1C')).not.toBeInTheDocument()
  })
})

describe('acceso por rol (router real)', () => {
  const abrir = (ruta: string) => {
    const cliente = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(<QueryClientProvider client={cliente}><AuthProvider><RouterProvider router={createMemoryRouter(rutas, { initialEntries: [ruta] })} /></AuthProvider></QueryClientProvider>)
  }

  it('USER en /staff/validacion → 403', async () => {
    conSesion(['USER']); abrir('/staff/validacion')
    expect(await screen.findByText('No tienes permisos para acceder a esta sección.')).toBeInTheDocument()
  })

  it('STAFF entra a la validación y ve los enlaces de operación', async () => {
    conSesion(['STAFF']); abrir('/staff/validacion')
    expect(await screen.findByRole('heading', { name: 'Validación de ingreso' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Tickets pagados' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Mis tickets' })).not.toBeInTheDocument()
  })

  it('anónimo → login', async () => {
    abrir('/staff/validacion')
    expect(await screen.findByRole('heading', { name: 'Inicia sesión' })).toBeInTheDocument()
  })
})
