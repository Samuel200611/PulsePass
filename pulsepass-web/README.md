# PulsePass Web (Fase 5 — Frontend)

SPA en React 19 + TypeScript (strict) + Tailwind v4 + React Router v8 + TanStack Query + React Hook Form + zod.

## Comandos
```
npm install
npm run dev      # http://localhost:5173, el proxy envía /api → http://localhost:8080
npm run build    # tsc estricto + vite build
npm run lint
npm test         # Vitest + Testing Library + MSW (sin backend real)
```
Requiere Node 22.22+. Variable pública: `VITE_API_BASE_URL` (ver `.env.example`).
Para usarlo con el backend, arranca la API en el puerto 8080 (ver tests/e2e/README.md del backend).

## Estado
Las 20 operaciones de la Fase 3 y los 3 endpoints de la Fase 4 son alcanzables desde la interfaz.

Implementado: layout y navbar por rol, inicio, cartelera con filtros en la URL, detalle de evento, artistas y detalle de artista, sedes y detalle de sede,
registro, login, sesión JWT (sessionStorage), cuenta, guardas (`RutaProtegida`, `RutaPorRol`, `SoloAnonimo`), 403/404/errorElement,
cliente HTTP con `ApiError`, compra de tickets (con barra de compra fija en móvil), confirmación, mis tickets (filtros y cancelación), detalle de ticket,
STAFF (validación de ingreso, tickets pagados), ADMIN (buscar, crear, publicar, asociar artista) y modo claro/oscuro con interruptor
(respeta `prefers-color-scheme`; la preferencia se guarda en `localStorage` y se valida con zod).

Notas: se añadió `/staff/eventos` (sin código) para el enlace "Tickets pagados"; la lista "Creados en esta sesión" vive en `sessionStorage` (GAP-01);
la descripción del evento admite hasta 1000 caracteres (límite del backend; el diseño mostraba 500).

Pendiente (fuera del código): copiar los 32 PNG de diseño a `docs/diseno/`, comparar cada pantalla con su imagen y pasar Lighthouse/axe (QT-FE-011).
