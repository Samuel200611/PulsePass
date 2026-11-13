import { useEffect, useId, useRef, type KeyboardEvent, type ReactNode } from 'react'
import { Icono } from './Icono'

const FOCOS = 'button:not([disabled]), a[href], input:not([disabled]), select, textarea, [tabindex]:not([tabindex="-1"])'

export function Modal({ titulo, onCerrar, children }: { titulo: string; onCerrar: () => void; children: ReactNode }) {
  const id = useId()
  const ref = useRef<HTMLDivElement>(null)
  useEffect(() => { ref.current?.focus() }, [])

  // Cierra con Esc y mantiene el foco dentro del diálogo.
  const alTeclear = (e: KeyboardEvent<HTMLDivElement>) => {
    if (e.key === 'Escape') return onCerrar()
    if (e.key !== 'Tab' || !ref.current) return
    const items = [...ref.current.querySelectorAll<HTMLElement>(FOCOS)]
    const primero = items[0]
    const ultimo = items[items.length - 1]
    if (!primero || !ultimo) return
    if (e.shiftKey && document.activeElement === primero) { e.preventDefault(); ultimo.focus() }
    else if (!e.shiftKey && document.activeElement === ultimo) { e.preventDefault(); primero.focus() }
  }

  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-slate-900/50 p-4" onClick={onCerrar}>
      <div ref={ref} role="dialog" aria-modal="true" aria-labelledby={id} tabIndex={-1} onKeyDown={alTeclear} onClick={(e) => e.stopPropagation()}
        className="w-full max-w-md space-y-4 rounded-2xl bg-white p-6 shadow-xl outline-none dark:bg-slate-900">
        <div className="flex items-center justify-between">
          <h2 id={id} className="font-titulo text-lg font-semibold">{titulo}</h2>
          <button type="button" onClick={onCerrar} aria-label="Cerrar" className="rounded p-1 focus-visible:outline-2 focus-visible:outline-marca-600"><Icono nombre="cerrar" /></button>
        </div>
        {children}
      </div>
    </div>
  )
}
