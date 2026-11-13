import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { useAuth } from '../../auth/AuthContext'
import { useComprarTicket } from '../hooks'
import { TicketTypeSchema, type TicketType } from '../schemas'
import type { Evento } from '../../eventos/schemas'
import { Boton, estilosBoton } from '../../../componentes/Boton'
import { Modal } from '../../../componentes/Modal'
import { Aviso } from '../../../componentes/Aviso'
import { mensajeDeError } from '../../../lib/errores'
import { RUTAS } from '../../../lib/rutas'

export function PanelCompra({ evento }: { evento: Evento }) {
  const { sesion, tieneRol } = useAuth()
  const navigate = useNavigate()
  const [tipo, setTipo] = useState<TicketType>('GENERAL')
  const [confirmando, setConfirmando] = useState(false)
  const comprar = useComprarTicket(evento.eventCode)
  const abierta = evento.status === 'PUBLISHED'

  if (sesion.estado === 'cargando' || (sesion.estado === 'autenticado' && !tieneRol('USER'))) return null // STAFF/ADMIN no compran

  const barraFija = abierta && sesion.estado === 'autenticado'
  const confirmar = () => comprar.mutate(tipo, { onSuccess: (t) => void navigate(RUTAS.confirmacion(t.ticketCode)) })
  const cerrar = () => { setConfirmando(false); comprar.reset() }

  return (
    <>
    <aside className="h-fit space-y-3 rounded-xl bg-white p-5 shadow-sm dark:bg-slate-900" aria-label="Compra">
      <h2 className="font-titulo text-lg font-semibold">Entradas</h2>
      {!abierta ? <p className="text-sm text-slate-600 dark:text-slate-400">La venta no está disponible: el evento está {evento.status}.</p>
        : sesion.estado === 'anonimo'
          ? <Link to={RUTAS.login} state={{ desde: RUTAS.evento(evento.eventCode) }} className={estilosBoton('primario', 'w-full')}>Inicia sesión para comprar</Link>
          : (
            <>
              <fieldset className="space-y-2">
                <legend className="mb-1 text-sm text-slate-600 dark:text-slate-400">Tipo de entrada</legend>
                {TicketTypeSchema.options.map((t) => (
                  <label key={t} className="flex min-h-11 cursor-pointer items-center gap-2 rounded-lg border border-slate-300 px-3 text-sm has-checked:border-marca-500 has-checked:bg-marca-50 dark:border-slate-700 dark:has-checked:bg-slate-800">
                    <input type="radio" name="tipo" value={t} checked={tipo === t} onChange={() => setTipo(t)} className="accent-marca-600" />{t}
                  </label>
                ))}
              </fieldset>
              <Boton className="w-full max-md:hidden" onClick={() => setConfirmando(true)}>Comprar entrada</Boton>
            </>
          )}
      <p className="text-xs text-slate-600 dark:text-slate-400">El precio lo define el sistema al confirmar.</p>

      {confirmando && sesion.estado === 'autenticado' && (
        <Modal titulo="Confirmar compra" onCerrar={cerrar}>
          <dl className="divide-y divide-slate-200 text-sm dark:divide-slate-700">
            {([['Evento', evento.name], ['Tipo', tipo], ['Cuenta', sesion.usuario.email]] as const).map(([k, v]) => (
              <div key={k} className="flex justify-between py-2"><dt className="text-slate-600 dark:text-slate-400">{k}</dt><dd className="font-medium">{v}</dd></div>
            ))}
          </dl>
          {comprar.isError && <Aviso tono="error">{mensajeDeError(comprar.error)}</Aviso>}
          <div className="flex justify-end gap-2">
            <Boton variante="secundario" onClick={cerrar}>Cancelar</Boton>
            <Boton onClick={confirmar} cargando={comprar.isPending}>Confirmar</Boton>
          </div>
        </Modal>
      )}
    </aside>
    {barraFija && (
      <div className="fixed inset-x-0 bottom-0 z-30 border-t border-slate-200 bg-white p-3 md:hidden dark:border-slate-800 dark:bg-slate-900">
        <Boton className="w-full" onClick={() => setConfirmando(true)}>Comprar</Boton>
      </div>
    )}
    </>
  )
}
