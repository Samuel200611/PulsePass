import type { ReactElement } from 'react'
import { render } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router'
import { AuthProvider } from '../funcionalidades/auth/AuthContext'

/** Renderiza una ruta real con Router + Query + Auth; `otras` son destinos de redirección. */
export function renderConProveedores(ui: ReactElement, opciones: { ruta?: string; path?: string; otras?: Record<string, string> } = {}) {
  const cliente = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const { ruta = '/', path = '/', otras = {} } = opciones
  return render(
    <QueryClientProvider client={cliente}>
      <AuthProvider>
        <MemoryRouter initialEntries={[ruta]}>
          <Routes>
            <Route path={path} element={ui} />
            {Object.entries(otras).map(([p, texto]) => <Route key={p} path={p} element={<p>{texto}</p>} />)}
          </Routes>
        </MemoryRouter>
      </AuthProvider>
    </QueryClientProvider>,
  )
}
