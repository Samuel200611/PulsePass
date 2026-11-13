import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http as msw, HttpResponse } from 'msw'
import { servidor } from './setup'
import { renderConProveedores } from './utils'
import DetalleArtista from '../pages/DetalleArtista'
import DetalleSede from '../pages/DetalleSede'
import DetalleEvento from '../pages/DetalleEvento'
import { BotonTema } from '../componentes/BotonTema'

const solar = { id: 1, stageName: 'Solar Beat', genre: 'Electrónica', country: 'Colombia', active: true }
const resumen = { id: 1, eventCode: 'CMF-2026', name: 'Caribbean Music Fest 2026', category: 'MUSIC', status: 'PUBLISHED', eventDate: '2026-12-12T20:00:00', venueName: 'Marina Convention Center' }
const sede = { id: 1, code: 'VEN-SMR-01', name: 'Marina Convention Center', city: 'Santa Marta', address: 'Calle 1 # 2-3', capacity: 3, active: true }
const noEncontrada = { status: 404, message: 'x' }

describe('detalle de artista', () => {
  const montar = (id: string) => renderConProveedores(<DetalleArtista />, { ruta: `/artistas/${id}`, path: '/artistas/:id' })

  it('muestra el artista y sus eventos', async () => {
    servidor.use(msw.get('*/api/artists/1', () => HttpResponse.json(solar)), msw.get('*/api/events/by-artist', ({ request }) => {
      expect(new URL(request.url).searchParams.get('stageName')).toBe('Solar Beat'); return HttpResponse.json([resumen]) }))
    montar('1')
    expect(await screen.findByRole('heading', { name: 'Solar Beat' })).toBeInTheDocument()
    expect(await screen.findByText('Caribbean Music Fest 2026')).toBeInTheDocument()
  })

  it('sin eventos → "Sin eventos próximos"', async () => {
    servidor.use(msw.get('*/api/artists/1', () => HttpResponse.json(solar)), msw.get('*/api/events/by-artist', () => HttpResponse.json([])))
    montar('1')
    expect(await screen.findByText('Sin eventos próximos')).toBeInTheDocument()
  })

  it('id no numérico → 404 de UI sin llamar a la API', () => {
    montar('abc') // cualquier petición dispararía onUnhandledFrame: 'error'
    expect(screen.getByText('No encontramos esta página.')).toBeInTheDocument()
  })

  it('404 de la API → página no encontrada', async () => {
    servidor.use(msw.get('*/api/artists/99', () => HttpResponse.json(noEncontrada, { status: 404 })))
    montar('99')
    expect(await screen.findByText('No encontramos esta página.')).toBeInTheDocument()
  })
})

describe('detalle de sede', () => {
  const montar = (code: string) => renderConProveedores(<DetalleSede />, { ruta: `/sedes/${code}`, path: '/sedes/:code' })

  it('muestra código, ciudad, dirección y capacidad', async () => {
    servidor.use(msw.get('*/api/venues/VEN-SMR-01', () => HttpResponse.json(sede)))
    montar('VEN-SMR-01')
    expect(await screen.findByRole('heading', { name: 'Marina Convention Center' })).toBeInTheDocument()
    expect(screen.getByText('3 personas')).toBeInTheDocument()
    expect(screen.getByText('Calle 1 # 2-3')).toBeInTheDocument()
  })

  it('404 → página no encontrada', async () => {
    servidor.use(msw.get('*/api/venues/XX', () => HttpResponse.json(noEncontrada, { status: 404 })))
    montar('XX')
    expect(await screen.findByText('No encontramos esta página.')).toBeInTheDocument()
  })
})

describe('barra de compra fija (móvil)', () => {
  const evento = { ...resumen, description: null, minimumAge: 18, venueCode: 'VEN-SMR-01', artists: [] }
  const montar = () => renderConProveedores(<DetalleEvento />, { ruta: '/eventos/CMF-2026', path: '/eventos/:eventCode' })
  const sesion = (roles: string[]) => {
    sessionStorage.setItem('pulsepass.sesion', JSON.stringify({ token: 't', expiraEn: Date.now() + 1e6 }))
    servidor.use(msw.get('*/api/auth/me', () => HttpResponse.json({ id: 1, username: 'andrea', email: 'andrea@example.test', roles })))
  }

  it('USER: la barra "Comprar" abre el mismo modal de confirmación', async () => {
    sesion(['USER'])
    servidor.use(msw.get('*/api/events/CMF-2026', () => HttpResponse.json(evento)))
    montar()
    await userEvent.click(await screen.findByRole('button', { name: 'Comprar' }))
    expect(await screen.findByRole('dialog')).toBeInTheDocument()
  })

  it('anónimo y STAFF no ven la barra de compra', async () => {
    servidor.use(msw.get('*/api/events/CMF-2026', () => HttpResponse.json(evento)))
    montar()
    await screen.findByRole('heading', { name: 'Caribbean Music Fest 2026' })
    expect(screen.queryByRole('button', { name: 'Comprar' })).not.toBeInTheDocument()
  })
})

describe('modo oscuro', () => {
  it('el interruptor alterna la clase .dark en <html> y recuerda la preferencia', async () => {
    render(<BotonTema />)
    const boton = screen.getByRole('button', { name: 'Modo oscuro' })
    expect(boton).toHaveAttribute('aria-pressed', 'false')
    await userEvent.click(boton)
    expect(document.documentElement).toHaveClass('dark')
    expect(boton).toHaveAttribute('aria-pressed', 'true')
    expect(localStorage.getItem('pulsepass.tema')).toBe('oscuro')
    await userEvent.click(boton)
    expect(document.documentElement).not.toHaveClass('dark')
  })

  it('arranca con la preferencia guardada', () => {
    localStorage.setItem('pulsepass.tema', 'oscuro')
    render(<BotonTema />)
    expect(document.documentElement).toHaveClass('dark')
  })

  it('un valor guardado inválido se ignora (validado con zod)', () => {
    localStorage.setItem('pulsepass.tema', 'violeta')
    render(<BotonTema />)
    expect(document.documentElement).not.toHaveClass('dark')
  })
})
