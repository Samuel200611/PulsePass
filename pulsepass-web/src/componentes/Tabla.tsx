import type { ReactNode } from 'react'

export interface Columna<T> {
  encabezado: string
  celda: (fila: T) => ReactNode
}

/** Tabla genérica. En móvil la página debe ofrecer la alternativa en tarjetas (esta se oculta bajo md). */
export function Tabla<T>({ columnas, filas, clave }: { columnas: ReadonlyArray<Columna<T>>; filas: ReadonlyArray<T>; clave: (fila: T) => string }) {
  return (
    <div className="hidden overflow-hidden rounded-xl bg-white shadow-sm md:block dark:bg-slate-900">
      <table className="w-full text-left text-sm">
        <thead className="border-b border-slate-200 text-slate-600 dark:border-slate-700 dark:text-slate-400">
          <tr>{columnas.map((c) => <th key={c.encabezado} scope="col" className="px-4 py-3 font-semibold">{c.encabezado}</th>)}</tr>
        </thead>
        <tbody className="divide-y divide-slate-200 dark:divide-slate-700">
          {filas.map((f) => <tr key={clave(f)}>{columnas.map((c) => <td key={c.encabezado} className="px-4 py-3">{c.celda(f)}</td>)}</tr>)}
        </tbody>
      </table>
    </div>
  )
}
