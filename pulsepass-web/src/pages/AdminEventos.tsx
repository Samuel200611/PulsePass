import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { useQueryClient } from '@tanstack/react-query'
import { eventosApi } from '../funcionalidades/eventos/api'
import { leerEventosCreados } from '../funcionalidades/admin/eventosCreados'
import { ApiError } from '../lib/http'
import { mensajeDeError } from '../lib/errores'
import { RUTAS } from '../lib/rutas'
import { Boton, estilosBoton } from '../componentes/Boton'
import { Aviso } from '../componentes/Aviso'

export default function AdminEventos() {
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [codigo, setCodigo] = useState('')
  const [error, setError] = useState<string>()
  const [buscando, setBuscando] = useState(false)
  const creados = leerEventosCreados()

  const abrir = async (e: FormEvent) => {
    e.preventDefault()
    const c = codigo.trim()
    if (!c) return setError('Ingresa un código de evento')
    setError(undefined); setBuscando(true)
    try {
      await qc.fetchQuery({ queryKey: ['evento', c], queryFn: () => eventosApi.porCodigo(c) })
      void navigate(RUTAS.adminEvento(c))
    } catch (err: unknown) {
      setError(err instanceof ApiError && err.status === 404 ? 'Evento no encontrado' : mensajeDeError(err))
    } finally {
      setBuscando(false)
    }
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div className="flex items-center justify-between gap-3">
        <h1 className="font-titulo text-3xl font-bold">Administración de eventos</h1>
        <Link to={RUTAS.adminNuevo} className={estilosBoton('primario')}>+ Nuevo evento</Link>
      </div>
      <form onSubmit={(e) => void abrir(e)} className="flex items-end gap-2" noValidate>
        <label className="flex-1 text-sm font-medium">Buscar evento por código
          <input value={codigo} onChange={(e) => setCodigo(e.target.value)} autoComplete="off" className="mt-1 block min-h-11 w-full rounded-lg border border-slate-300 bg-white px-3 text-sm dark:border-slate-700 dark:bg-slate-900" />
        </label>
        <Boton type="submit" variante="secundario" cargando={buscando}>Abrir</Boton>
      </form>
      {error && <Aviso tono="error">{error}</Aviso>}

      <section aria-labelledby="creados" className="space-y-2">
        <h2 id="creados" className="font-titulo text-lg font-semibold">Creados en esta sesión</h2>
        {creados.length === 0 ? <p className="text-sm text-slate-600 dark:text-slate-400">Aún no has creado eventos en esta sesión.</p> : (
          <ul className="space-y-2">{creados.map((c) => (
            <li key={c.eventCode} className="flex items-center justify-between rounded-xl bg-white p-3 shadow-sm dark:bg-slate-900">
              <span><span className="font-mono font-bold">{c.eventCode}</span> · {c.name}</span>
              <Link to={RUTAS.adminEvento(c.eventCode)} className={estilosBoton('secundario')}>Gestionar</Link>
            </li>))}</ul>
        )}
        <Aviso tono="info">Nota: la API no ofrece un listado completo de eventos (GAP-01).</Aviso>
      </section>
    </div>
  )
}
