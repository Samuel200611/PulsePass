-- =========================================================
-- PulsePass — V2__insert_initial_artists.sql
-- Catalogo inicial de artistas de prueba (seccion 15 del PRD).
-- No se especifica id: la columna es IDENTITY.
-- =========================================================

INSERT INTO artists (stage_name, country, genre, active) VALUES
    ('Solar Beat',      'Colombia', 'Electronic',  TRUE),
    ('Neon Waves',      'Mexico',   'Synth Pop',   TRUE),
    ('Caribbean Sound', 'Colombia', 'Tropical',    TRUE),
    ('Ocean Drive',     'Spain',    'Indie Rock',  TRUE),
    ('Digital Pulse',   'Chile',    'Techno',      TRUE);
