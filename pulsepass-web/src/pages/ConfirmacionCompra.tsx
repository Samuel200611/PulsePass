import { Link, useParams } from 'react-router'
import { useTicket } from '../funcionalidades/tickets/hooks'
import { BadgeTicket } from '../componentes/Badge'
import { Cargando, EstadoError } from '../componentes/Estados'
import { estilosBoton } from '../componentes/Boton'
import { Icono } from '../componentes/Icono'
import { formatearCOP } from '../lib/formato'
import { RUTAS } from '../lib/rutas'

export default function ConfirmacionCompra() {
  const { ticketCode = '' } = useParams()
  const { data: t, isPending, error } = useTicket(ticketCode)
  if (isPending) return <Cargando />
  if (error) return <EstadoError mensaje="Ticket no disponible" />
  return (
    <div className="mx-auto max-w-md space-y-3 rounded-2xl bg-white p-8 text-center shadow-sm dark:bg-slate-900">
      <span className="mx-auto grid size-12 place-items-center rounded-full bg-emerald-100 text-emerald-700"><Icono nombre="check" className="size-6" /></span>
      <h1 className="font-titulo text-2xl font-bold">¡Compra realizada con éxito!</h1>
      <p className="font-semibold">{t.eventName}</p>
      <p className="text-sm text-slate-600 dark:text-slate-400">Ticket <span className="font-mono">{t.ticketCode}</span> · {t.type} · {formatearCOP(t.price)}</p>
      <BadgeTicket estado={t.status} />
      <div className="flex justify-center gap-2 pt-2">
        <Link to={RUTAS.misTickets} className={estilosBoton('primario')}>Ver mis tickets</Link>
        <Link to={RUTAS.eventos} className={estilosBoton('secundario')}>Seguir explorando</Link>
      </div>
    </div>
  )
}
