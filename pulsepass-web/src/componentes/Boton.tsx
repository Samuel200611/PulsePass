import type { ComponentProps } from 'react'
import clsx from 'clsx'

export type VarianteBoton = 'primario' | 'secundario' | 'peligro' | 'fantasma'

const BASE = 'inline-flex min-h-11 items-center justify-center gap-2 rounded-lg px-4 text-sm font-semibold transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-marca-600 disabled:cursor-not-allowed disabled:opacity-60'
const VARIANTES: Record<VarianteBoton, string> = {
  primario: 'bg-marca-600 text-white hover:bg-marca-700',
  secundario: 'border border-slate-300 bg-white text-slate-900 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100 dark:hover:bg-slate-800',
  peligro: 'bg-red-600 text-white hover:bg-red-700',
  fantasma: 'text-marca-600 hover:bg-marca-50 dark:text-marca-500 dark:hover:bg-slate-800',
}
export const estilosBoton = (variante: VarianteBoton = 'primario', extra?: string) => clsx(BASE, VARIANTES[variante], extra)

interface BotonProps extends ComponentProps<'button'> {
  variante?: VarianteBoton
  cargando?: boolean
}

export function Boton({ variante = 'primario', cargando = false, className, children, disabled, ...props }: BotonProps) {
  return (
    <button {...props} disabled={disabled || cargando} aria-busy={cargando || undefined} className={estilosBoton(variante, className)}>
      {cargando ? 'Cargando…' : children}
    </button>
  )
}
