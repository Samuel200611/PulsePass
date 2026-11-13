import { createBrowserRouter, type RouteObject } from 'react-router'
import Layout from './layouts/Layout'
import RutaProtegida from './layouts/RutaProtegida'
import RutaPorRol from './layouts/RutaPorRol'
import SoloAnonimo from './layouts/SoloAnonimo'
import Inicio from './pages/Inicio'
import ErrorPagina from './pages/ErrorPagina'

const perezosa = (cargar: () => Promise<{ default: React.ComponentType }>) => ({
  lazy: async () => ({ Component: (await cargar()).default }),
})

export const rutas: RouteObject[] = [
  {
    path: '/',
    Component: Layout,
    errorElement: <ErrorPagina />,
    children: [
      { index: true, Component: Inicio },
      { path: 'eventos', ...perezosa(() => import('./pages/CatalogoEventos')) },
      { path: 'eventos/:eventCode', ...perezosa(() => import('./pages/DetalleEvento')) },
      { path: 'artistas', ...perezosa(() => import('./pages/ListaArtistas')) },
      { path: 'artistas/:id', ...perezosa(() => import('./pages/DetalleArtista')) },
      { path: 'sedes', ...perezosa(() => import('./pages/ListaSedes')) },
      { path: 'sedes/:code', ...perezosa(() => import('./pages/DetalleSede')) },
      {
        Component: SoloAnonimo,
        children: [
          { path: 'login', ...perezosa(() => import('./pages/Login')) },
          { path: 'registro', ...perezosa(() => import('./pages/Registro')) },
        ],
      },
      {
        Component: RutaProtegida,
        children: [
          { path: 'cuenta', ...perezosa(() => import('./pages/Cuenta')) },
          {
            element: <RutaPorRol roles={['USER']} />,
            children: [
              { path: 'mis-tickets', ...perezosa(() => import('./pages/MisTickets')) },
              { path: 'mis-tickets/:ticketCode', ...perezosa(() => import('./pages/DetalleTicket')) },
              { path: 'compra/confirmacion/:ticketCode', ...perezosa(() => import('./pages/ConfirmacionCompra')) },
            ],
          },
          {
            element: <RutaPorRol roles={['STAFF', 'ADMIN']} />,
            children: [
              { path: 'staff/validacion', ...perezosa(() => import('./pages/ValidacionIngreso')) },
              { path: 'staff/eventos', ...perezosa(() => import('./pages/TicketsPagadosEvento')) },
              { path: 'staff/eventos/:eventCode/tickets', ...perezosa(() => import('./pages/TicketsPagadosEvento')) },
            ],
          },
          {
            element: <RutaPorRol roles={['ADMIN']} />,
            children: [
              { path: 'admin/eventos', ...perezosa(() => import('./pages/AdminEventos')) },
              { path: 'admin/eventos/nuevo', ...perezosa(() => import('./pages/AdminCrearEvento')) },
              { path: 'admin/eventos/:eventCode', ...perezosa(() => import('./pages/AdminGestionEvento')) },
            ],
          },
        ],
      },
      { path: '403', ...perezosa(() => import('./pages/SinPermisos')) },
      { path: '*', ...perezosa(() => import('./pages/NoEncontrada')) },
    ],
  },
]

export const router = createBrowserRouter(rutas)
