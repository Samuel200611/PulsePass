import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { authApi } from '../funcionalidades/auth/api'
import { RegistroSchema, type RegistroForm } from '../funcionalidades/auth/schemas'
import { ApiError } from '../lib/http'
import { RUTAS } from '../lib/rutas'
import { Campo } from '../componentes/Campo'
import { Boton } from '../componentes/Boton'
import { Aviso } from '../componentes/Aviso'

const CAMPOS: ReadonlyArray<keyof RegistroForm> = ['username', 'email', 'firstName', 'lastName', 'birthDate', 'password']

export default function Registro() {
  const navigate = useNavigate()
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null)
  const { register, handleSubmit, setError, formState: { errors } } = useForm<RegistroForm>({ resolver: zodResolver(RegistroSchema) })
  const registrar = useMutation({ mutationFn: authApi.registrar })

  const enviar = handleSubmit(async (datos) => {
    setErrorGeneral(null)
    try {
      await registrar.mutateAsync(datos)
      void navigate(RUTAS.login, { state: { registrado: true } })
    } catch (e: unknown) {
      if (!(e instanceof ApiError)) return setErrorGeneral('Ocurrió un error inesperado. Inténtalo más tarde.')
      if (e.status === 409) return setErrorGeneral('El usuario o correo ya está registrado.')
      if (e.status === 400) {
        const sobrantes: string[] = []
        for (const [campo, mensaje] of Object.entries(e.details)) {
          const conocido = CAMPOS.find((c) => c === campo)
          if (conocido) setError(conocido, { message: mensaje }); else sobrantes.push(mensaje)
        }
        if (sobrantes.length) setErrorGeneral(sobrantes.join(' '))
        return
      }
      setErrorGeneral(e.message)
    }
  })

  return (
    <div className="mx-auto max-w-md space-y-4 rounded-2xl bg-white p-6 shadow-sm dark:bg-slate-900">
      <h1 className="text-center font-titulo text-2xl font-bold">Crea tu cuenta</h1>
      <form onSubmit={enviar} noValidate className="space-y-4">
        <Campo etiqueta="Nombre de usuario" autoComplete="username" error={errors.username?.message} {...register('username')} />
        <Campo etiqueta="Correo electrónico" type="email" autoComplete="email" error={errors.email?.message} {...register('email')} />
        <div className="grid gap-4 sm:grid-cols-2">
          <Campo etiqueta="Nombres" autoComplete="given-name" error={errors.firstName?.message} {...register('firstName')} />
          <Campo etiqueta="Apellidos" autoComplete="family-name" error={errors.lastName?.message} {...register('lastName')} />
        </div>
        <Campo etiqueta="Fecha de nacimiento" type="date" autoComplete="bday" error={errors.birthDate?.message} {...register('birthDate')} />
        <Campo etiqueta="Contraseña (mín. 12 caracteres)" type="password" autoComplete="new-password" error={errors.password?.message} {...register('password')} />
        {errorGeneral && <Aviso tono="error">{errorGeneral}</Aviso>}
        <Boton type="submit" cargando={registrar.isPending} className="w-full">Crear cuenta</Boton>
      </form>
      <p className="text-center text-sm">¿Ya tienes cuenta? <Link to={RUTAS.login} className="font-semibold text-marca-600 dark:text-marca-500">Inicia sesión</Link></p>
    </div>
  )
}
