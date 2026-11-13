import clsx from 'clsx'
import type { EventStatus } from '../funcionalidades/eventos/schemas'

export const TONOS_EVENTO: Record<EventStatus, string> = {
  DRAFT: 'bg-slate-200 text-slate-700 dark:bg-slate-700 dark:text-slate-100',
  PUBLISHED: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-200',
  SOLD_OUT: 'bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-200',
  CANCELLED: 'bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-200',
  FINISHED: 'bg-slate-300 text-slate-800 dark:bg-slate-700 dark:text-slate-100',
}

export function BadgeEstado({ estado }: { estado: EventStatus }) {
  return <span className={clsx('inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold', TONOS_EVENTO[estado])}>{estado}</span>
}

import type { TicketStatus } from '../funcionalidades/tickets/schemas'

export const TONOS_TICKET: Record<TicketStatus, string> = {
  RESERVED: 'bg-sky-100 text-sky-800 dark:bg-sky-900/40 dark:text-sky-200',
  PAID: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-200',
  USED: 'bg-slate-300 text-slate-800 dark:bg-slate-700 dark:text-slate-100',
  CANCELLED: 'bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-200',
}

export function BadgeTicket({ estado }: { estado: TicketStatus }) {
  return <span className={clsx('inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold', TONOS_TICKET[estado])}>{estado}</span>
}
