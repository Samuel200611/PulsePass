import { screen } from '@testing-library/react'
import { http as msw, HttpResponse } from 'msw'
import { servidor } from './setup'
import { renderConProveedores } from './utils'
import CatalogoEventos from '../pages/CatalogoEventos'
import DetalleEvento from '../pages/DetalleEvento'

const evento = { id: 1, eventCode: 'CMF-2026', name: 'Caribbean Music Fest 2026', category: 'MUSIC', status: 'PUBLISHED', eventDate: '2026-12-12T20:00:00', venueName: 'Marina Convention Center' }

describe('catálogo', () => {
  it('lista los eventos publicados', async () => {
    servidor.use(msw.get('*/api/events/published', () => HttpResponse.json([evento])), msw.get('*/api/artists/active', () => HttpResponse.json([])))
    renderConProveedores(<CatalogoEventos />, { ruta: '/eventos', path: '/eventos' })
    expect(await screen.findByText('Caribbean Music Fest 2026')).toBeInTheDocument()
  })

  it('lista vacía → estado vacío', async () => {
    servidor.use(msw.get('*/api/events/published', () => HttpResponse.json([])), msw.get('*/api/artists/active', () => HttpResponse.json([])))
    renderConProveedores(<CatalogoEventos />, { ruta: '/eventos', path: '/eventos' })
    expect(await screen.findByText('Sin resultados para tu búsqueda')).toBeInTheDocument()
  })

  it('el filtro por texto se lee de la URL', async () => {
    servidor.use(msw.get('*/api/events/published', () => HttpResponse.json([evento])), msw.get('*/api/artists/active', () => HttpResponse.json([])))
    renderConProveedores(<CatalogoEventos />, { ruta: '/eventos?q=inexistente', path: '/eventos' })
    expect(await screen.findByText('Sin resultados para tu búsqueda')).toBeInTheDocument()
  })

  it('detalle: 404 → "Evento no disponible"', async () => {
    servidor.use(msw.get('*/api/events/XX', () => HttpResponse.json({ status: 404, message: 'no' }, { status: 404 })))
    renderConProveedores(<DetalleEvento />, { ruta: '/eventos/XX', path: '/eventos/:eventCode' })
    expect(await screen.findByText('Evento no disponible')).toBeInTheDocument()
  })

  it('detalle: evento SOLD_OUT no ofrece la compra', async () => {
    servidor.use(msw.get('*/api/events/CMF-2026', () => HttpResponse.json({ ...evento, status: 'SOLD_OUT', description: null, minimumAge: 18, venueCode: 'VEN-SMR-01', artists: [] })))
    renderConProveedores(<DetalleEvento />, { ruta: '/eventos/CMF-2026', path: '/eventos/:eventCode' })
    expect(await screen.findByText(/La venta no está disponible/)).toBeInTheDocument()
    expect(screen.queryByText('Inicia sesión para comprar')).not.toBeInTheDocument()
  })
})
