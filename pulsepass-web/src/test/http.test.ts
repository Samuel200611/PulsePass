import { http as msw, HttpResponse } from 'msw'
import { z } from 'zod'
import { servidor } from './setup'
import { ApiError, http, setToken, setUnauthorizedHandler } from '../lib/http'

const Esquema = z.object({ ok: z.boolean() })

describe('lib/http', () => {
  it('añade Authorization Bearer cuando hay token', async () => {
    let cabecera: string | null = null
    servidor.use(msw.get('*/api/algo', ({ request }) => { cabecera = request.headers.get('authorization'); return HttpResponse.json({ ok: true }) }))
    setToken('abc')
    await http.get('/algo', Esquema)
    expect(cabecera).toBe('Bearer abc')
  })

  it('ErrorResponse válido → ApiError con status, mensaje y details', async () => {
    servidor.use(msw.get('*/api/algo', () => HttpResponse.json(
      { timestamp: 't', status: 400, error: 'Bad Request', message: 'Datos inválidos', details: { email: 'Correo inválido' } }, { status: 400 })))
    const error = await http.get('/algo', Esquema).catch((e: unknown) => e)
    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status: 400, message: 'Datos inválidos', details: { email: 'Correo inválido' } })
  })

  it('cuerpo de error inválido → ApiError genérico sin filtrar datos internos', async () => {
    servidor.use(msw.get('*/api/algo', () => new HttpResponse('<html>stacktrace</html>', { status: 500 })))
    const error = await http.get('/algo', Esquema).catch((e: unknown) => e)
    expect(error).toMatchObject({ status: 500, message: 'Ocurrió un error inesperado. Inténtalo más tarde.' })
  })

  it('401 con sesión dispara el cierre de sesión', async () => {
    const alNoAutorizado = vi.fn()
    setUnauthorizedHandler(alNoAutorizado)
    setToken('vencido')
    servidor.use(msw.get('*/api/algo', () => HttpResponse.json({ status: 401, message: 'x' }, { status: 401 })))
    await http.get('/algo', Esquema).catch(() => undefined)
    expect(alNoAutorizado).toHaveBeenCalledOnce()
  })

  it('respuesta con formato inesperado falla en la validación zod', async () => {
    servidor.use(msw.get('*/api/algo', () => HttpResponse.json({ otro: 1 })))
    await expect(http.get('/algo', Esquema)).rejects.toBeInstanceOf(ApiError)
  })
})
