import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { useTicket } from '../funcionalidades/tickets/hooks'
import { ModalCancelar } from '../funcionalidades/tickets/componentes/ModalCancelar'
import { BadgeTicket } from '../componentes/Badge'
import { Boton } from '../componentes/Boton'
import { Cargando, EstadoError } from '../componentes/Estados'
import { formatearCOP, formatearFechaHora } from '../lib/formato'
import { RUTAS } from '../lib/rutas'

export default function DetalleTicket() {
  const { ticketCode = '' } = useParams()
  const { data: t, isPending, error } = useTicket(ticketCode)
  const [cancelando, setCancelando] = useState(false)
  const [copiado, setCopiado] = useState(false)

  if (isPending) return <Cargando />
  if (error) return <EstadoError mensaje="Ticket no disponible" /> // 403/404 sin revelar si existe

  const copiar = () => void navigator.clipboard.writeText(t.ticketCode).then(() => setCopiado(true))
  const filas: ReadonlyArray<[string, string]> = [['Tipo', t.type], ['Precio', formatearCOP(t.price)], ['Comprado el', formatearFechaHora(t.purchaseDate)], ['Titular', t.userEmail]]

  return (
    <div className="mx-auto max-w-2xl space-y-4">
      <Link to={RUTAS.misTickets} className="text-sm font-medium text-marca-600 dark:text-marca-500">← Mis tickets</Link>
      <article className="space-y-4 rounded-xl bg-white p-6 shadow-sm dark:bg-slate-900">
        <div className="flex items-start justify-between gap-3"><h1 className="font-titulo text-2xl font-bold">{t.eventName}</h1><BadgeTicket estado={t.status} /></div>
        <div className="flex items-center justify-between border-b border-slate-200 pb-3 dark:border-slate-700">
          <span className="text-sm text-slate-600 dark:text-slate-400">Código</span>
          <span className="flex items-center gap-2"><span className="font-mono">{t.ticketCode}</span><Boton variante="secundario" onClick={copiar}>{copiado ? 'Copiado' : 'Copiar'}</Boton></span>
        </div>
        <dl className="divide-y divide-slate-200 text-sm dark:divide-slate-700">
          {filas.map(([k, v]) => <div key={k} className="flex justify-between py-2"><dt className="text-slate-600 dark:text-slate-400">{k}</dt><dd className="font-medium">{v}</dd></div>)}
        </dl>
        <p className="text-sm text-slate-600 dark:text-slate-400">Presenta este código en el ingreso.</p>
      </article>
      {t.status === 'PAID' && <div className="flex justify-end"><Boton variante="peligro" onClick={() => setCancelando(true)}>Cancelar ticket</Boton></div>}
      {cancelando && <ModalCancelar ticket={t} onCerrar={() => setCancelando(false)} />}
    </div>
  )
}
