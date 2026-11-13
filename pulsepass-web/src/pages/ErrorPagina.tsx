import { Link, isRouteErrorResponse, useRouteError } from 'react-router'
import { estilosBoton } from '../componentes/Boton'
import { RUTAS } from '../lib/rutas'

export default function ErrorPagina() {
  const error = useRouteError()
  const mensaje = isRouteErrorResponse(error) && error.status === 404 ? 'No encontramos esta página.' : 'Ocurrió un error inesperado. Inténtalo más tarde.'
  return (
    <div className="mx-auto mt-16 max-w-md space-y-3 rounded-2xl bg-white dark:bg-slate-900 p-8 text-center shadow-sm">
      <h1 className="font-titulo text-2xl font-bold">¡Ups! Algo salió mal.</h1>
      <p>{mensaje}</p>
      <div className="flex justify-center gap-2">
        <button type="button" className={estilosBoton('primario')} onClick={() => window.location.reload()}>Reintentar</button>
        <Link to={RUTAS.inicio} className={estilosBoton('secundario')}>Volver al inicio</Link>
      </div>
    </div>
  )
}
