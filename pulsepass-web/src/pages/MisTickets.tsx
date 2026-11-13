import { useState } from 'react'
import { Link } from 'react-router'
import clsx from 'clsx'
import { useMisTickets } from '../funcionalidades/tickets/hooks'
import type { Ticket, TicketStatus } from '../funcionalidades/tickets/schemas'
import { ModalCancelar } from '../funcionalidades/tickets/componentes/ModalCancelar'
import { BadgeTicket } from '../componentes/Badge'
import { Boton, estilosBoton } from '../componentes/Boton'
import { Cargando, EstadoError, EstadoVacio } from '../componentes/Estados'
import { formatearCOP, formatearFecha } from '../lib/formato'
import { RUTAS } from '../lib/rutas'

const FILTROS: ReadonlyArray<{ texto: string; estado: TicketStatus | null }> = [
  { texto: 'Todos', estado: null }, { texto: 'Pagados', estado: 'PAID' },
  { texto: 'Usados', estado: 'USED' }, { texto: 'Cancelados', estado: 'CANCELLED' },
]

export default function MisTickets() {
  const { data, isPending, error, refetch } = useMisTickets()
  const [filtro, setFiltro] = useState<TicketStatus | null>(null)
  const [aCancelar, setACancelar] = useState<Ticket | null>(null)
  const visibles = (data ?? []).filter((t) => filtro === null || t.status === filtro)

  return (
    <div className="space-y-6">
      <h1 className="font-titulo text-3xl font-bold">Mis tickets</h1>
      <div className="flex flex-wrap gap-2" role="group" aria-label="Filtrar por estado">
        {FILTROS.map((f) => (
          <button key={f.texto} type="button" aria-pressed={filtro === f.estado} onClick={() => setFiltro(f.estado)}
            className={clsx('min-h-11 rounded-full border px-4 text-sm font-medium focus-visible:outline-2 focus-visible:outline-marca-600',
              filtro === f.estado ? 'border-marca-600 bg-marca-600 text-white' : 'border-slate-300 bg-white dark:border-slate-700 dark:bg-slate-900')}>{f.texto}</button>
        ))}
      </div>
      {isPending ? <Cargando />
        : error ? <EstadoError mensaje={error.message} onReintentar={() => void refetch()} />
        : visibles.length === 0 ? <EstadoVacio mensaje="Aún no tienes tickets" accion={{ texto: 'Ver cartelera', a: RUTAS.eventos }} />
        : (
          <ul className="space-y-3">
            {visibles.map((t) => (
              <li key={t.ticketCode} className="flex flex-wrap items-center justify-between gap-3 rounded-xl bg-white p-4 shadow-sm dark:bg-slate-900">
                <div>
                  <p className="font-titulo font-semibold">{t.eventName}</p>
                  <p className="text-sm text-slate-600 dark:text-slate-400"><span className="font-mono">{t.ticketCode}</span> · {t.type} · {formatearCOP(t.price)} · {formatearFecha(t.purchaseDate)}</p>
                </div>
                <div className="flex items-center gap-2">
                  <BadgeTicket estado={t.status} />
                  <Link to={RUTAS.ticket(t.ticketCode)} className={estilosBoton('secundario')}>Ver detalle</Link>
                  {t.status === 'PAID' && <Boton variante="peligro" onClick={() => setACancelar(t)}>Cancelar</Boton>}
                </div>
              </li>
            ))}
          </ul>
        )}
      {aCancelar && <ModalCancelar ticket={aCancelar} onCerrar={() => setACancelar(null)} />}
    </div>
  )
}
