import { useEffect, useState } from 'react'
import { aplicarTema, temaInicial, type Tema } from '../lib/tema'
import { Icono } from './Icono'

export function BotonTema() {
  const [tema, setTema] = useState<Tema>(temaInicial)
  useEffect(() => { aplicarTema(tema) }, [tema])
  return (
    <button type="button" aria-label="Modo oscuro" aria-pressed={tema === 'oscuro'} onClick={() => setTema((t) => (t === 'oscuro' ? 'claro' : 'oscuro'))}
      className="rounded-md p-2 text-slate-600 hover:bg-slate-100 focus-visible:outline-2 focus-visible:outline-marca-600 dark:text-slate-300 dark:hover:bg-slate-800">
      <Icono nombre={tema === 'oscuro' ? 'sol' : 'luna'} className="size-5" />
    </button>
  )
}
