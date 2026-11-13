const PATHS = {
  musica: 'M9 18V5l12-2v13M9 18a3 3 0 11-6 0 3 3 0 016 0zm12-2a3 3 0 11-6 0 3 3 0 016 0z',
  pantalla: 'M3 5h18v11H3zM8 20h8M12 16v4',
  balon: 'M12 3a9 9 0 100 18 9 9 0 000-18zM3 12h18M12 3v18',
  edificio: 'M4 21h16M5 21V10l7-5 7 5v11M9 21v-8h6v8',
  birrete: 'M3 9l9-5 9 5-9 5-9-5zM7 11.5V16c0 1 2.5 2.5 5 2.5s5-1.5 5-2.5v-4.5',
  estrella: 'M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1L3.2 9.5l6.1-.9z',
  calendario: 'M4 6h16v14H4zM4 10h16M8 3v4M16 3v4',
  pin: 'M12 21s7-5.5 7-11a7 7 0 10-14 0c0 5.5 7 11 7 11zM12 12.5a2.5 2.5 0 100-5 2.5 2.5 0 000 5z',
  alerta: 'M12 3l10 18H2L12 3zM12 10v5M12 18h.01',
  check: 'M5 12l5 5 9-10',
  menu: 'M4 6h16M4 12h16M4 18h16',
  cerrar: 'M6 6l12 12M18 6L6 18',
  luna: 'M21 12.8A9 9 0 1111.2 3a7 7 0 009.8 9.8z',
  sol: 'M12 16a4 4 0 100-8 4 4 0 000 8zM12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4',
  bandeja: 'M3 13l3-8h12l3 8v6H3zM3 13h5l1 3h6l1-3h5',
} as const
export type NombreIcono = keyof typeof PATHS

export function Icono({ nombre, className = 'size-5' }: { nombre: NombreIcono; className?: string }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" className={className}>
      <path d={PATHS[nombre]} />
    </svg>
  )
}
