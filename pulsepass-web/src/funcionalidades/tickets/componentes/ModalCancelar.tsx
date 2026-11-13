import { useCancelarTicket } from '../hooks'
import type { Ticket } from '../schemas'
import { Boton } from '../../../componentes/Boton'
import { Modal } from '../../../componentes/Modal'
import { Aviso } from '../../../componentes/Aviso'
import { mensajeDeError } from '../../../lib/errores'

/** Un 409 se muestra dentro del modal sin cerrarlo. */
export function ModalCancelar({ ticket, onCerrar }: { ticket: Ticket; onCerrar: () => void }) {
  const cancelar = useCancelarTicket()
  return (
    <Modal titulo="Cancelar ticket" onCerrar={onCerrar}>
      <p className="text-sm">¿Quieres cancelar el ticket <strong className="font-mono">{ticket.ticketCode}</strong> ({ticket.type}) de {ticket.eventName}?</p>
      {cancelar.isError && <Aviso tono="error">{mensajeDeError(cancelar.error)}</Aviso>}
      <div className="flex justify-end gap-2">
        <Boton variante="secundario" onClick={onCerrar}>Volver</Boton>
        <Boton variante="peligro" cargando={cancelar.isPending} onClick={() => cancelar.mutate(ticket.ticketCode, { onSuccess: onCerrar })}>Cancelar ticket</Boton>
      </div>
    </Modal>
  )
}
