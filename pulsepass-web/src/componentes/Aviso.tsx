import clsx from 'clsx'
import { Icono } from './Icono'

export type TonoAviso = 'exito' | 'error' | 'info' | 'advertencia'
const TONOS: Record<TonoAviso, string> = {
  exito: 'border-emerald-200 bg-emerald-50 text-emerald-800 dark:border-emerald-800 dark:bg-emerald-950 dark:text-emerald-200',
  error: 'border-red-200 bg-red-50 text-red-800 dark:border-red-800 dark:bg-red-950 dark:text-red-200',
  advertencia: 'border-amber-300 bg-amber-50 text-amber-900 dark:border-amber-800 dark:bg-amber-950 dark:text-amber-200',
  info: 'border-marca-500/30 bg-marca-50 text-marca-700 dark:bg-slate-800 dark:text-marca-50',
}

export function Aviso({ tono, children }: { tono: TonoAviso; children: React.ReactNode }) {
  return (
    <div role={tono === 'error' || tono === 'advertencia' ? 'alert' : 'status'} aria-live={tono === 'error' || tono === 'advertencia' ? 'assertive' : 'polite'}
      className={clsx('flex items-start gap-2 rounded-lg border px-3 py-2 text-sm', TONOS[tono])}>
      <Icono nombre={tono === 'error' || tono === 'advertencia' ? 'alerta' : 'check'} className="mt-0.5 size-4 shrink-0" />
      <span>{children}</span>
    </div>
  )
}
