import { Link } from 'react-router'
import { Boton, estilosBoton } from './Boton'
import { Aviso } from './Aviso'
import { Icono } from './Icono'

export function Cargando() {
  return (
    <div aria-busy="true" aria-label="Cargando" className="animate-pulse space-y-3">
      <div className="h-32 rounded-xl bg-slate-200 dark:bg-slate-800" />
      <div className="h-4 w-1/3 rounded bg-slate-200 dark:bg-slate-800" />
      <div className="h-4 w-2/3 rounded bg-slate-200 dark:bg-slate-800" />
    </div>
  )
}

export function EstadoVacio({ mensaje, accion }: { mensaje: string; accion?: { texto: string; a: string } }) {
  return (
    <div className="flex flex-col items-center gap-3 rounded-xl bg-white p-8 text-center shadow-sm dark:bg-slate-900">
      <Icono nombre="bandeja" className="size-8 text-slate-500" />
      <p className="font-semibold">{mensaje}</p>
      {accion && <Link to={accion.a} className={estilosBoton('secundario')}>{accion.texto}</Link>}
    </div>
  )
}

export function EstadoError({ mensaje, onReintentar }: { mensaje: string; onReintentar?: () => void }) {
  return (
    <div className="space-y-3 rounded-xl bg-white p-6 shadow-sm dark:bg-slate-900">
      <Aviso tono="error">{mensaje}</Aviso>
      {onReintentar && <Boton onClick={onReintentar}>Reintentar</Boton>}
    </div>
  )
}
