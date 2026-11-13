import { z } from 'zod'

export const RolSchema = z.enum(['USER', 'STAFF', 'ADMIN'])
export type Rol = z.infer<typeof RolSchema>

export const AuthResponseSchema = z.object({
  accessToken: z.string().min(1),
  tokenType: z.literal('Bearer'),
  expiresIn: z.number(),
})
export const UsuarioAutenticadoSchema = z.object({
  id: z.number(),
  username: z.string(),
  email: z.string(),
  roles: z.array(RolSchema),
})
export type UsuarioAutenticado = z.infer<typeof UsuarioAutenticadoSchema>

export const SesionGuardadaSchema = z.object({ token: z.string().min(1), expiraEn: z.number() })

export const LoginSchema = z.object({
  identifier: z.string().trim().min(1, 'Ingresa tu usuario o correo'),
  password: z.string().min(1, 'Ingresa tu contraseña'),
})
export type LoginForm = z.infer<typeof LoginSchema>

const ayer = () => new Date(new Date().setHours(23, 59, 59, 999))
export const RegistroSchema = z.object({
  username: z.string().trim().min(1, 'Ingresa un nombre de usuario'),
  email: z.email('Ingresa un correo válido'),
  firstName: z.string().trim().min(1, 'Ingresa tus nombres'),
  lastName: z.string().trim().min(1, 'Ingresa tus apellidos'),
  birthDate: z.string().min(1, 'Ingresa tu fecha de nacimiento')
    .refine((v) => new Date(v) <= ayer(), 'La fecha no puede ser futura'),
  password: z.string()
    .min(12, 'Mínimo 12 caracteres')
    .refine((v) => new TextEncoder().encode(v).length <= 72, 'Máximo 72 bytes'),
})
export type RegistroForm = z.infer<typeof RegistroSchema>

export const UsuarioRegistradoSchema = z.object({ username: z.string(), email: z.string() })
