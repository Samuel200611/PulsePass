import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useAuth } from '../funcionalidades/auth/AuthContext'
import { LoginSchema, type LoginForm } from '../funcionalidades/auth/schemas'
import { ApiError } from '../lib/http'
import { leerEstadoNav } from '../lib/navegacion'
import { RUTAS } from '../lib/rutas'
import { Campo } from '../componentes/Campo'
import { Boton } from '../componentes/Boton'
import { Aviso } from '../componentes/Aviso'

export default function Login() {
  const { iniciarSesion } = useAuth()
  const navigate = useNavigate()
  const estado = leerEstadoNav(useLocation().state)
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null)
  const { register, handleSubmit, setError, formState: { errors, isSubmitting } } = useForm<LoginForm>({ resolver: zodResolver(LoginSchema) })

  const enviar = handleSubmit(async (datos) => {
    setErrorGeneral(null)
    try {
      await iniciarSesion(datos)
      void navigate(estado.desde ?? RUTAS.inicio, { replace: true })
    } catch (e: unknown) {
      if (!(e instanceof ApiError)) return setErrorGeneral('Ocurrió un error inesperado. Inténtalo más tarde.')
      if (e.status === 401) return setErrorGeneral('Credenciales inválidas.')
      if (e.status === 400) {
        for (const [campo, mensaje] of Object.entries(e.details)) if (campo === 'identifier' || campo === 'password') setError(campo, { message: mensaje })
        return
      }
      setErrorGeneral(e.status === 0 ? e.message : 'Inténtalo más tarde.')
    }
  })

  return (
    <div className="mx-auto max-w-sm space-y-4 rounded-2xl bg-white p-6 shadow-sm dark:bg-slate-900">
      <h1 className="text-center font-titulo text-2xl font-bold">Inicia sesión</h1>
      {estado.registrado && <Aviso tono="exito">Cuenta creada, inicia sesión.</Aviso>}
      {estado.motivo === 'expirada' && <Aviso tono="info">Tu sesión expiró. Inicia sesión de nuevo.</Aviso>}
      <form onSubmit={enviar} noValidate className="space-y-4">
        <Campo etiqueta="Usuario o correo" autoComplete="username" error={errors.identifier?.message} {...register('identifier')} />
        <Campo etiqueta="Contraseña" type="password" autoComplete="current-password" error={errors.password?.message} {...register('password')} />
        {errorGeneral && <Aviso tono="error">{errorGeneral}</Aviso>}
        <Boton type="submit" cargando={isSubmitting} className="w-full">Ingresar</Boton>
      </form>
      <p className="text-center text-sm">¿No tienes cuenta? <Link to={RUTAS.registro} className="font-semibold text-marca-600 dark:text-marca-500">Regístrate</Link></p>
    </div>
  )
}
