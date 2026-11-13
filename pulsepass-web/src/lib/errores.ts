import { ApiError } from './http'

/** Mensaje seguro para mostrar: el de la API en 4xx esperados, genérico en el resto. */
export function mensajeDeError(e: unknown): string {
  if (e instanceof ApiError) return e.status === 403 ? 'No tienes permiso para realizar esta acción.' : e.message
  return 'Ocurrió un error inesperado. Inténtalo más tarde.'
}
