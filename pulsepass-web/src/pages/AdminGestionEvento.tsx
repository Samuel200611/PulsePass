import { useState } from 'react'
import { Link, useLocation, useParams } from 'react-router'
import { useAsociarArtista, useEvento, usePublicarEvento } from '../funcionalidades/eventos/hooks'
import { useArtistasActivos } from '../funcionalidades/artistas/hooks'
import { ApiError } from '../lib/http'
import { mensajeDeError } from '../lib/errores'
import { formatearFechaHora, iniciales } from '../lib/formato'
import { RUTAS } from '../lib/rutas'
import { BadgeEstado } from '../componentes/Badge'
import { Boton } from '../componentes/Boton'
import { Modal } from '../componentes/Modal'
import { Aviso } from '../componentes/Aviso'
import { Cargando, EstadoError } from '../componentes/Estados'

export default function AdminGestionEvento() {
  const { eventCode = '' } = useParams()
  const creado = (useLocation().state as { creado?: boolean } | null)?.creado === true
  const { data: evento, isPending, error, refetch } = useEvento(eventCode)
  const artistas = useArtistasActivos()
  const publicar = usePublicarEvento(eventCode)
  const asociar = useAsociarArtista(eventCode)
  const [confirmando, setConfirmando] = useState(false)
  const [elegido, setElegido] = useState('')

  if (isPending) return <Cargando />
  if (error instanceof ApiError && error.status === 404) return <EstadoError mensaje="Evento no encontrado" />
  if (error) return <EstadoError mensaje={error.message} onReintentar={() => void refetch()} />

  // La UI evita duplicados, pero el backend sigue siendo la autoridad (409).
  const disponibles = (artistas.data ?? []).filter((a) => !evento.artists.some((x) => x.id === a.id))
  const cerrar = () => { setConfirmando(false); publicar.reset() }

  return (
    <div className="mx-auto max-w-3xl space-y-4">
      <Link to={RUTAS.adminEventos} className="text-sm font-medium text-marca-600 dark:text-marca-500">← Eventos</Link>
      {creado && <Aviso tono="exito">Evento creado.</Aviso>}
      <div className="flex items-start justify-between gap-3">
        <div>
          <h1 className="font-titulo text-3xl font-bold">{evento.name}</h1>
          <p className="text-sm text-slate-600 dark:text-slate-400"><span className="font-mono">{evento.eventCode}</span> · {evento.category} · {formatearFechaHora(evento.eventDate)} · Mín. {evento.minimumAge} · {evento.venueName}</p>
        </div>
        <BadgeEstado estado={evento.status} />
      </div>

      <section aria-labelledby="publicacion" className="flex items-center justify-between gap-3 rounded-xl bg-white p-5 shadow-sm dark:bg-slate-900">
        <div><h2 id="publicacion" className="font-titulo text-lg font-semibold">Publicación</h2><p className="text-sm text-slate-600 dark:text-slate-400">El evento es visible al público una vez publicado.</p></div>
        <Boton disabled={evento.status !== 'DRAFT'} onClick={() => setConfirmando(true)}>Publicar evento</Boton>
      </section>

      <section aria-labelledby="artistas-admin" className="space-y-3 rounded-xl bg-white p-5 shadow-sm dark:bg-slate-900">
        <h2 id="artistas-admin" className="font-titulo text-lg font-semibold">Artistas asociados</h2>
        {evento.artists.length === 0 ? <p className="text-sm text-slate-600 dark:text-slate-400">Aún no hay artistas asociados.</p> : (
          <ul className="flex flex-wrap gap-3">{evento.artists.map((a) => (
            <li key={a.id} className="flex items-center gap-2 text-sm"><span className="grid size-9 place-items-center rounded-full bg-marca-50 text-xs font-bold text-marca-700">{iniciales(a.stageName)}</span>{a.stageName}</li>))}</ul>
        )}
        <div className="flex items-end gap-2">
          <label className="flex-1 text-sm font-medium">Agregar artista
            <select value={elegido} onChange={(e) => setElegido(e.target.value)} className="mt-1 block min-h-11 w-full rounded-lg border border-slate-300 bg-white px-3 text-sm dark:border-slate-700 dark:bg-slate-900">
              <option value="">Selecciona un artista</option>
              {disponibles.map((a) => <option key={a.id} value={a.id}>{a.stageName}</option>)}
            </select>
          </label>
          <Boton variante="secundario" disabled={!elegido} cargando={asociar.isPending} onClick={() => asociar.mutate(Number(elegido), { onSuccess: () => setElegido('') })}>+ Asociar</Boton>
        </div>
        {asociar.isError && <Aviso tono="advertencia">{mensajeDeError(asociar.error)}</Aviso>}
      </section>

      <Link to={RUTAS.ticketsPagados(evento.eventCode)} className="inline-block text-sm font-medium text-marca-600 dark:text-marca-500">Tickets pagados →</Link>

      {confirmando && (
        <Modal titulo="Publicar evento" onCerrar={cerrar}>
          <p className="text-sm">¿Publicar <strong>{evento.name}</strong>? Será visible para todo el público.</p>
          {publicar.isError && <Aviso tono="advertencia">{mensajeDeError(publicar.error)}</Aviso>}
          <div className="flex justify-end gap-2">
            <Boton variante="secundario" onClick={cerrar}>Volver</Boton>
            <Boton cargando={publicar.isPending} onClick={() => publicar.mutate(undefined, { onSuccess: cerrar })}>Publicar</Boton>
          </div>
        </Modal>
      )}
    </div>
  )
}
