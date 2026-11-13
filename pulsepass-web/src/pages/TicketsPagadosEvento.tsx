import { useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router'
import { useTicketsPagadosPorEvento, useUsarTicket } from '../funcionalidades/tickets/hooks'
import type { Ticket } from '../funcionalidades/tickets/schemas'
import { ApiError } from '../lib/http'
import { mensajeDeError } from '../lib/errores'
import { formatearCOP } from '../lib/formato'
import { RUTAS } from '../lib/rutas'
import { BadgeTicket } from '../componentes/Badge'
import { Boton } from '../componentes/Boton'
import { Aviso } from '../componentes/Aviso'
import { Cargando, EstadoError, EstadoVacio } from '../componentes/Estados'
import { Tabla, type Columna } from '../componentes/Tabla'

export default function TicketsPagadosEvento() {
  const { eventCode = '' } = useParams()
  const navigate = useNavigate()
  const [entrada, setEntrada] = useState(eventCode)
  const { data, isPending, error, refetch } = useTicketsPagadosPorEvento(eventCode)
  const usar = useUsarTicket()
  const ir = (e: FormEvent) => { e.preventDefault(); if (entrada.trim()) void navigate(RUTAS.ticketsPagados(entrada.trim())) }

  const columnas: Columna<Ticket>[] = [
    { encabezado: 'Código', celda: (t) => <span className="font-mono">{t.ticketCode}</span> },
    { encabezado: 'Tipo', celda: (t) => t.type },
    { encabezado: 'Titular', celda: (t) => t.userEmail },
    { encabezado: 'Precio', celda: (t) => formatearCOP(t.price) },
    { encabezado: 'Estado', celda: (t) => <BadgeTicket estado={t.status} /> },
    { encabezado: 'Acción', celda: (t) => <Boton onClick={() => usar.mutate(t.ticketCode)} cargando={usar.isPending && usar.variables === t.ticketCode}>Usar</Boton> },
  ]

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <h1 className="font-titulo text-3xl font-bold">{eventCode ? `Tickets pagados · ${eventCode}` : 'Tickets pagados'}</h1>
        <form onSubmit={ir} className="flex items-end gap-2">
          <label className="text-sm font-medium">Evento
            <input value={entrada} onChange={(e) => setEntrada(e.target.value)} autoComplete="off" className="mt-1 block min-h-11 w-40 rounded-lg border border-slate-300 bg-white px-3 text-sm dark:border-slate-700 dark:bg-slate-900" />
          </label>
          <Boton type="submit" variante="secundario">Ir</Boton>
        </form>
      </div>
      {usar.isError && <Aviso tono="advertencia">{mensajeDeError(usar.error)}</Aviso>}

      {!eventCode ? <EstadoVacio mensaje="Ingresa un código de evento para ver sus tickets pagados" />
        : isPending ? <Cargando />
        : error instanceof ApiError && error.status === 404 ? <EstadoError mensaje="Evento no encontrado" />
        : error ? <EstadoError mensaje={error.message} onReintentar={() => void refetch()} />
        : data.length === 0 ? <EstadoVacio mensaje="No hay tickets pagados" />
        : (
          <>
            <p className="text-sm text-slate-600 dark:text-slate-400">Total: {data.length}</p>
            <Tabla columnas={columnas} filas={data} clave={(t) => t.ticketCode} />
            <ul className="space-y-3 md:hidden" aria-label="Tickets pagados (móvil)">
              {data.map((t) => (
                <li key={t.ticketCode} className="space-y-2 rounded-xl bg-white p-4 shadow-sm dark:bg-slate-900">
                  <div className="flex items-center justify-between"><span className="font-mono font-bold">{t.ticketCode}</span><BadgeTicket estado={t.status} /></div>
                  <p className="text-sm text-slate-600 dark:text-slate-400">{t.type} · {t.userEmail} · {formatearCOP(t.price)}</p>
                  <Boton className="w-full" onClick={() => usar.mutate(t.ticketCode)}>Usar</Boton>
                </li>
              ))}
            </ul>
          </>
        )}
    </div>
  )
}
