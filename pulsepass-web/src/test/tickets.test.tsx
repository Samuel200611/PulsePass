import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http as msw, HttpResponse } from 'msw'
import { servidor } from './setup'
import { renderConProveedores } from './utils'
import DetalleEvento from '../pages/DetalleEvento'
import MisTickets from '../pages/MisTickets'
import DetalleTicket from '../pages/DetalleTicket'
import type { Ticket } from '../funcionalidades/tickets/schemas'

const EMAIL = 'andrea@example.test'
const evento = { id: 1, eventCode: 'CMF-2026', name: 'Caribbean Music Fest 2026', category: 'MUSIC', status: 'PUBLISHED', eventDate: '2026-12-12T20:00:00',
  venueName: 'Marina Convention Center', description: null, minimumAge: 18, venueCode: 'VEN-SMR-01', artists: [] }
const ticket = (o: Partial<Ticket> = {}): Ticket => ({ id: 1, ticketCode: 'TKT-8F2A1C', type: 'VIP', price: 250000, status: 'PAID',
  purchaseDate: '2026-11-05T14:32:00', userEmail: EMAIL, eventCode: 'CMF-2026', eventName: 'Caribbean Music Fest 2026', ...o })

function conSesion(roles: string[] = ['USER']) {
  sessionStorage.setItem('pulsepass.sesion', JSON.stringify({ token: 't', expiraEn: Date.now() + 1e6 }))
  servidor.use(msw.get('*/api/auth/me', () => HttpResponse.json({ id: 1, username: 'andrea', email: EMAIL, roles })))
}
const abrirCompra = async (tipo?: string) => {
  if (tipo) await userEvent.click(await screen.findByLabelText(tipo))
  await userEvent.click(await screen.findByRole('button', { name: 'Comprar entrada' }))
}

describe('compra', () => {
  const montar = () => renderConProveedores(<DetalleEvento />, { ruta: '/eventos/CMF-2026', path: '/eventos/:eventCode', otras: { '/compra/confirmacion/:ticketCode': 'Pantalla de confirmación' } })

  it('envía el email de la sesión y navega a la confirmación (201)', async () => {
    conSesion()
    let cuerpo: unknown
    servidor.use(
      msw.get('*/api/events/CMF-2026', () => HttpResponse.json(evento)),
      msw.post('*/api/tickets', async ({ request }) => { cuerpo = await request.json(); return HttpResponse.json(ticket(), { status: 201 }) }),
    )
    montar()
    await abrirCompra('VIP')
    await userEvent.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Confirmar' }))
    expect(await screen.findByText('Pantalla de confirmación')).toBeInTheDocument()
    expect(cuerpo).toEqual({ userEmail: EMAIL, eventCode: 'CMF-2026', type: 'VIP' })
  })

  it('409 → mensaje del backend dentro del modal, sin navegar', async () => {
    conSesion()
    servidor.use(
      msw.get('*/api/events/CMF-2026', () => HttpResponse.json(evento)),
      msw.post('*/api/tickets', () => HttpResponse.json({ status: 409, message: 'El usuario no cumple la edad mínima del evento.' }, { status: 409 })),
    )
    montar()
    await abrirCompra()
    const modal = await screen.findByRole('dialog')
    await userEvent.click(within(modal).getByRole('button', { name: 'Confirmar' }))
    expect(await within(modal).findByRole('alert')).toHaveTextContent('El usuario no cumple la edad mínima del evento.')
    expect(screen.queryByText('Pantalla de confirmación')).not.toBeInTheDocument()
  })

  it('anónimo → "Inicia sesión para comprar"', async () => {
    servidor.use(msw.get('*/api/events/CMF-2026', () => HttpResponse.json(evento)))
    montar()
    expect(await screen.findByText('Inicia sesión para comprar')).toBeInTheDocument()
  })

  it('STAFF no ve el panel de compra', async () => {
    conSesion(['STAFF'])
    servidor.use(msw.get('*/api/events/CMF-2026', () => HttpResponse.json(evento)))
    montar()
    expect(await screen.findByRole('heading', { name: 'Caribbean Music Fest 2026' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Comprar entrada' })).not.toBeInTheDocument()
  })
})

describe('mis tickets', () => {
  const montar = () => renderConProveedores(<MisTickets />, { ruta: '/mis-tickets', path: '/mis-tickets' })
  const lista = [ticket(), ticket({ id: 2, ticketCode: 'TKT-5C90E2', status: 'USED', type: 'GENERAL' }), ticket({ id: 3, ticketCode: 'TKT-77AD13', status: 'CANCELLED' })]

  it('lista y filtra por estado; Cancelar solo aparece en PAID', async () => {
    conSesion()
    servidor.use(msw.get('*/api/tickets/by-user', ({ request }) => { expect(new URL(request.url).searchParams.get('email')).toBe(EMAIL); return HttpResponse.json(lista) }))
    montar()
    expect(await screen.findByText('TKT-8F2A1C')).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: 'Cancelar' })).toHaveLength(1)
    await userEvent.click(screen.getByRole('button', { name: 'Usados' }))
    expect(screen.getByText('TKT-5C90E2')).toBeInTheDocument()
    expect(screen.queryByText('TKT-8F2A1C')).not.toBeInTheDocument()
  })

  it('sin tickets → estado vacío', async () => {
    conSesion()
    servidor.use(msw.get('*/api/tickets/by-user', () => HttpResponse.json([])))
    montar()
    expect(await screen.findByText('Aún no tienes tickets')).toBeInTheDocument()
  })

  it('cancelar un ticket PAID → 200 y la lista se actualiza', async () => {
    conSesion()
    let cancelado = false
    servidor.use(
      msw.get('*/api/tickets/by-user', () => HttpResponse.json([ticket({ status: cancelado ? 'CANCELLED' : 'PAID' })])),
      msw.patch('*/api/tickets/TKT-8F2A1C/cancel', () => { cancelado = true; return HttpResponse.json(ticket({ status: 'CANCELLED' })) }),
    )
    montar()
    await userEvent.click(await screen.findByRole('button', { name: 'Cancelar' }))
    await userEvent.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Cancelar ticket' }))
    expect(await screen.findByText('CANCELLED')).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('cancelar → 409 muestra el mensaje y mantiene el modal', async () => {
    conSesion()
    servidor.use(
      msw.get('*/api/tickets/by-user', () => HttpResponse.json([ticket()])),
      msw.patch('*/api/tickets/TKT-8F2A1C/cancel', () => HttpResponse.json({ status: 409, message: 'El ticket no puede cancelarse en su estado actual.' }, { status: 409 })),
    )
    montar()
    await userEvent.click(await screen.findByRole('button', { name: 'Cancelar' }))
    const modal = await screen.findByRole('dialog')
    await userEvent.click(within(modal).getByRole('button', { name: 'Cancelar ticket' }))
    expect(await within(modal).findByRole('alert')).toHaveTextContent('El ticket no puede cancelarse en su estado actual.')
    expect(screen.getByRole('dialog')).toBeInTheDocument()
  })

  it('Esc cierra el modal', async () => {
    conSesion()
    servidor.use(msw.get('*/api/tickets/by-user', () => HttpResponse.json([ticket()])))
    montar()
    await userEvent.click(await screen.findByRole('button', { name: 'Cancelar' }))
    await screen.findByRole('dialog')
    await userEvent.keyboard('{Escape}')
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })
})

describe('detalle de ticket', () => {
  const montar = (code: string) => renderConProveedores(<DetalleTicket />, { ruta: `/mis-tickets/${code}`, path: '/mis-tickets/:ticketCode' })

  it('muestra el ticket propio', async () => {
    conSesion()
    servidor.use(msw.get('*/api/tickets/TKT-8F2A1C', () => HttpResponse.json(ticket())))
    montar('TKT-8F2A1C')
    expect(await screen.findByText('Presenta este código en el ingreso.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Cancelar ticket' })).toBeInTheDocument()
  })

  it.each([403, 404])('%i → "Ticket no disponible" sin datos del ticket', async (status) => {
    conSesion()
    servidor.use(msw.get('*/api/tickets/TKT-AJENO', () => HttpResponse.json({ status, message: 'x' }, { status })))
    montar('TKT-AJENO')
    expect(await screen.findByText('Ticket no disponible')).toBeInTheDocument()
  })

  it('ticket USED → sin botón de cancelar', async () => {
    conSesion()
    servidor.use(msw.get('*/api/tickets/TKT-8F2A1C', () => HttpResponse.json(ticket({ status: 'USED' }))))
    montar('TKT-8F2A1C')
    expect(await screen.findByText('USED')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Cancelar ticket' })).not.toBeInTheDocument()
  })
})
