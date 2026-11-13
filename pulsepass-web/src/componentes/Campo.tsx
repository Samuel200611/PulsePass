import { useId, useState, type ComponentProps } from 'react'
import clsx from 'clsx'

interface CampoProps extends ComponentProps<'input'> {
  etiqueta: string
  error?: string
}

export function Campo({ etiqueta, error, className, type, ...props }: CampoProps) {
  const id = useId()
  const [visible, setVisible] = useState(false)
  const esClave = type === 'password'
  return (
    <div>
      <label htmlFor={id} className="mb-1 block text-sm font-medium">{etiqueta}</label>
      <div className="relative">
        <input
          {...props}
          id={id}
          type={esClave && visible ? 'text' : type}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? `${id}-error` : undefined}
          className={clsx('min-h-11 w-full rounded-lg border bg-white px-3 text-sm focus-visible:outline-2 focus-visible:outline-marca-600 dark:bg-slate-900',
            error ? 'border-red-500' : 'border-slate-300 dark:border-slate-700', esClave && 'pr-20', className)}
        />
        {esClave && (
          <button type="button" onClick={() => setVisible((v) => !v)} aria-pressed={visible}
            className="absolute inset-y-0 right-2 px-2 text-xs font-semibold text-marca-600 dark:text-marca-500 focus-visible:outline-2 focus-visible:outline-marca-600">
            {visible ? 'Ocultar' : 'Mostrar'}
          </button>
        )}
      </div>
      {error && <p id={`${id}-error`} className="mt-1 text-sm text-red-700 dark:text-red-400">{error}</p>}
    </div>
  )
}
