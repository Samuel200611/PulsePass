import { useState, type FormEvent } from 'react'
import { useBuscarTicket, useUsarTicket } from '../funcionalidades/tickets/hooks'
import { ApiError } from '../lib/http'
import { mensajeDeError } from '../lib/errores'
import { BadgeTicket } from '../componentes/Badge'
import { Boton } from '../componentes/Boton'
import { Campo } from '../componentes/Campo'
import { Aviso } from '../componentes/Aviso'
import { Cargando } from '../componentes/Estados'

export default function ValidacionIngreso() {
  const [entrada, setEntrada] = useState('')
  const [codigo, setCodigo] = useState('')
  const [errorCampo, setErrorCampo] = useState<string>()
  const [registrado, setRegistrado] = useState(false)
  const ticket = useBuscarTicket(codigo)
  const usar = useUsarTicket()

  const buscar = (e: FormEvent) => {
    e.preventDefault()
    const c = entrada.trim()
    if (!c) return setErrorCampo('Ingresa el código del ticket')
    setErrorCampo(undefined); setRegistrado(false); usar.reset()
    if (c === codigo) void ticket.refetch(); else setCodigo(c)
  }
  const marcar = (code: string) => usar.mutate(code, {
    onSuccess: () => { setRegistrado(true); setEntrada('') },
    onError: () => void ticket.refetch(), // el 409 suele indicar un estado desactualizado
  })

  const errorBusqueda = ticket.error instanceof ApiError
    ? (ticket.error.status === 404 ? 'Ticket no encontrado' : ticket.error.status === 403 ? 'Sin permisos' : ticket.error.message)
    : ticket.error ? mensajeDeError(ticket.error) : null

  return (
    <div className="mx-auto max-w-2xl space-y-4">
      <h1 className="font-titulo text-3xl font-bold">Validación de ingreso</h1>
      <form onSubmit={buscar} noValidate className="flex items-end gap-2">
        <div className="flex-1"><Campo etiqueta="Código de ticket" value={entrada} onChange={(e) => setEntrada(e.target.value)} error={errorCampo} autoFocus autoComplete="off" className="font-mono" /></div>
        <Boton type="submit" className="mb-0.5">Buscar</Boton>
      </form>

      {codigo && ticket.isPending && <Cargando />}
      {errorBusqueda && <Aviso tono="error">{errorBusqueda}</Aviso>}
      {ticket.data && (
        <article className="space-y-4 rounded-xl bg-white p-6 shadow-sm dark:bg-slate-900" aria-label="Ticket encontrado">
          <div className="flex items-start justify-between gap-3">
            <div><h2 className="font-titulo text-xl font-semibold">{ticket.data.eventName}</h2>
              <p className="text-slate-600 dark:text-slate-400">{ticket.data.type} · Titular: {ticket.data.userEmail}</p></div>
            <BadgeTicket estado={ticket.data.status} />
          </div>
          {ticket.data.status === 'PAID' && <Boton className="w-full min-h-14 text-base" cargando={usar.isPending} onClick={() => marcar(ticket.data.ticketCode)}>Marcar como usado</Boton>}
        </article>
      )}
      {registrado && <Aviso tono="exito">Ingreso registrado.</Aviso>}
      {usar.isError && <Aviso tono="advertencia">{mensajeDeError(usar.error)}</Aviso>}
    </div>
  )
}
