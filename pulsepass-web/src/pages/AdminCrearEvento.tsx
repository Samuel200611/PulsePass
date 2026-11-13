import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { useForm, useWatch } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useCrearEvento } from '../funcionalidades/eventos/hooks'
import { CrearEventoSchema, EventCategorySchema, type CrearEventoForm } from '../funcionalidades/eventos/schemas'
import { useSedesActivas } from '../funcionalidades/sedes/hooks'
import { recordarEventoCreado } from '../funcionalidades/admin/eventosCreados'
import { ApiError } from '../lib/http'
import { RUTAS } from '../lib/rutas'
import { Campo } from '../componentes/Campo'
import { Boton, estilosBoton } from '../componentes/Boton'
import { Aviso } from '../componentes/Aviso'

const CAMPOS: ReadonlyArray<keyof CrearEventoForm> = ['eventCode', 'name', 'description', 'category', 'eventDate', 'minimumAge', 'venueCode']
const SELECT = 'min-h-11 w-full rounded-lg border bg-white px-3 text-sm focus-visible:outline-2 focus-visible:outline-marca-600 dark:bg-slate-900'

export default function AdminCrearEvento() {
  const navigate = useNavigate()
  const sedes = useSedesActivas()
  const crear = useCrearEvento()
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null)
  const { register, handleSubmit, setError, control, formState: { errors } } = useForm<CrearEventoForm>({
    resolver: zodResolver(CrearEventoSchema),
    defaultValues: { category: 'MUSIC', description: '', minimumAge: 0 },
  })

  const enviar = handleSubmit(async (datos) => {
    setErrorGeneral(null)
    try {
      const e = await crear.mutateAsync(datos)
      recordarEventoCreado({ eventCode: e.eventCode, name: e.name })
      void navigate(RUTAS.adminEvento(e.eventCode), { state: { creado: true } })
    } catch (err: unknown) {
      if (!(err instanceof ApiError)) return setErrorGeneral('Ocurrió un error inesperado. Inténtalo más tarde.')
      if (err.status === 403) return void navigate(RUTAS.sinPermisos)
      if (err.status === 400) {
        const sobrantes: string[] = []
        for (const [campo, mensaje] of Object.entries(err.details)) {
          const conocido = CAMPOS.find((c) => c === campo)
          if (conocido) setError(conocido, { message: mensaje }); else sobrantes.push(mensaje)
        }
        return setErrorGeneral(sobrantes.length ? sobrantes.join(' ') : null)
      }
      setErrorGeneral(err.message) // 409 (duplicado, sede inválida) / 404 (sede): mensaje del backend
    }
  })

  return (
    <div className="mx-auto max-w-3xl space-y-4">
      <Link to={RUTAS.adminEventos} className="text-sm font-medium text-marca-600 dark:text-marca-500">← Eventos</Link>
      <h1 className="font-titulo text-3xl font-bold">Nuevo evento</h1>
      <form onSubmit={enviar} noValidate className="space-y-4 rounded-2xl bg-white p-6 shadow-sm dark:bg-slate-900">
        {errorGeneral && <Aviso tono="error">{errorGeneral}</Aviso>}
        <Campo etiqueta="Código del evento" autoComplete="off" error={errors.eventCode?.message} {...register('eventCode')} />
        <Campo etiqueta="Nombre" error={errors.name?.message} {...register('name')} />
        <div>
          <label htmlFor="descripcion" className="mb-1 block text-sm font-medium">Descripción</label>
          <textarea id="descripcion" rows={3} aria-describedby="contador" aria-invalid={errors.description ? true : undefined} {...register('description')}
            className="w-full rounded-lg border border-slate-300 bg-white p-3 text-sm focus-visible:outline-2 focus-visible:outline-marca-600 dark:border-slate-700 dark:bg-slate-900" />
          <p id="contador" className="text-right text-xs text-slate-600 dark:text-slate-400">{useWatch({ control, name: 'description' }).length}/1000</p>
          {errors.description && <p className="text-sm text-red-700 dark:text-red-400">{errors.description.message}</p>}
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label htmlFor="categoria" className="mb-1 block text-sm font-medium">Categoría</label>
            <select id="categoria" {...register('category')} className={`${SELECT} border-slate-300 dark:border-slate-700`}>
              {EventCategorySchema.options.map((c) => <option key={c} value={c}>{c}</option>)}
            </select>
          </div>
          <Campo etiqueta="Edad mínima" type="number" min={0} max={120} error={errors.minimumAge?.message} {...register('minimumAge', { valueAsNumber: true })} />
        </div>
        <Campo etiqueta="Fecha y hora" type="datetime-local" error={errors.eventDate?.message} {...register('eventDate')} />
        <div>
          <label htmlFor="sede" className="mb-1 block text-sm font-medium">Sede</label>
          <select id="sede" aria-invalid={errors.venueCode ? true : undefined} {...register('venueCode')} className={`${SELECT} ${errors.venueCode ? 'border-red-500' : 'border-slate-300 dark:border-slate-700'}`}>
            <option value="">Selecciona una sede</option>
            {sedes.data?.map((s) => <option key={s.code} value={s.code}>{s.name} ({s.code})</option>)}
          </select>
          {errors.venueCode && <p className="mt-1 text-sm text-red-700 dark:text-red-400">{errors.venueCode.message}</p>}
        </div>
        <div className="flex justify-end gap-2">
          <Link to={RUTAS.adminEventos} className={estilosBoton('secundario')}>Cancelar</Link>
          <Boton type="submit" cargando={crear.isPending}>Crear evento</Boton>
        </div>
      </form>
    </div>
  )
}
