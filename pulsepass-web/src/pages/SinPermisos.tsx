import { Link } from 'react-router'
import { estilosBoton } from '../componentes/Boton'
import { RUTAS } from '../lib/rutas'

export default function SinPermisos() {
  return (
    <div className="mx-auto max-w-md space-y-3 rounded-2xl bg-white p-8 text-center shadow-sm dark:bg-slate-900">
      <p className="font-titulo text-5xl font-bold text-marca-600 dark:text-marca-500">403</p>
      <p>No tienes permisos para acceder a esta sección.</p>
      <Link to={RUTAS.inicio} className={estilosBoton('primario')}>Volver al inicio</Link>
    </div>
  )
}
