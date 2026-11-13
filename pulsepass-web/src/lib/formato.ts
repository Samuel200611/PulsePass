const fecha = new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'short', year: 'numeric' })
const fechaHora = new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit', hour12: false })
const cop = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })

export const formatearFecha = (iso: string) => fecha.format(new Date(iso))
export const formatearFechaHora = (iso: string) => fechaHora.format(new Date(iso))
export const formatearCOP = (valor: number) => cop.format(valor)
export const iniciales = (texto: string) => texto.split(/\s+/).filter(Boolean).slice(0, 2).map((p) => p[0]?.toUpperCase() ?? '').join('')
