-- ============================================================================
-- PULSEPASS | Datos de demostración para PostgreSQL (rama main, migraciones V1-V4)
-- Archivo: src/main/resources/db/seed/V5__seed_datos_prueba.sql
-- Uso: SOLO DESARROLLO LOCAL, NUNCA PRODUCCIÓN.
-- Migración de datos exclusiva para el perfil dev. Ejecutar luego de V1 a V4.
-- La ejecución no elimina registros existentes y los códigos/correos son únicos.
-- Idempotente ante ejecución manual: ON CONFLICT DO NOTHING; Flyway la aplica una sola vez.
-- Todos los usuarios sembrados usan contraseña: PulsePass2026!
-- Contraseñas almacenadas como hashes BCrypt compatibles con Spring Security.
-- IMPORTANTE: no subir credenciales de prueba a una base accesible públicamente.
-- Eventos con fecha relativa a CURRENT_DATE para seguir siendo futuros al ejecutar.
-- Los artistas de la migración V2 se reutilizan, sin volver a insertarlos.
-- No incluye CREATE TABLE: Flyway es dueño del esquema.
-- Requiere que el perfil dev incluya classpath:db/seed en spring.flyway.locations.
-- ============================================================================
BEGIN;

-- 1) Sedes
INSERT INTO venues (code, name, city, address, capacity, active) VALUES
    ('VEN-PP-BOG-01', 'Auditorio Capital Pulse', 'Bogotá', 'Calle 26 # 45-10', 1800, TRUE),
    ('VEN-PP-BOG-02', 'Centro de Convenciones Sabana', 'Bogotá', 'Carrera 7 # 62-30', 950, TRUE),
    ('VEN-PP-MED-01', 'Teatro Aurora', 'Medellín', 'Carrera 43A # 18-50', 1200, TRUE),
    ('VEN-PP-CAL-01', 'Arena del Valle', 'Cali', 'Avenida 3N # 21-44', 1100, TRUE),
    ('VEN-PP-BAQ-01', 'Foro Caribe', 'Barranquilla', 'Calle 84 # 53-20', 700, TRUE),
    ('VEN-PP-CTG-01', 'Casa Cultural Murallas', 'Cartagena', 'Calle del Arsenal # 8-15', 420, TRUE),
    ('VEN-PP-BGA-01', 'Sala Mirador', 'Bucaramanga', 'Carrera 27 # 36-12', 350, TRUE),
    ('VEN-PP-PER-01', 'Centro Cultural Cafetero', 'Pereira', 'Avenida Circunvalar # 15-12', 500, TRUE),
    ('VEN-PP-MAN-01', 'Aula Magna Nevado', 'Manizales', 'Calle 65 # 23-20', 300, TRUE),
    ('VEN-PP-TEST-01', 'Sala Cupo Único de Pruebas', 'Bogotá', 'Calle 1 # 1-01', 1, TRUE),
    ('VEN-PP-INACT-01', 'Sede Inactiva de Pruebas', 'Bogotá', 'Calle 99 # 99-99', 250, FALSE)
ON CONFLICT (code) DO NOTHING;


-- 2) Artistas adicionales (V2 ya inserta 5 artistas iniciales)
INSERT INTO artists (stage_name, country, genre, active) VALUES
    ('Bitácora Andina', 'Colombia', 'Folk andino', TRUE),
    ('Cumbia Cuántica', 'Colombia', 'Cumbia electrónica', TRUE),
    ('Páramo Eléctrico', 'Colombia', 'Rock alternativo', TRUE),
    ('Atlas Interactivo', 'Chile', 'Indie pop', TRUE),
    ('Pulsos Urbanos', 'Colombia', 'Hip hop', TRUE),
    ('Laboratorio Sonoro', 'México', 'Experimental', TRUE),
    ('Nómadas de Neón', 'Argentina', 'Synthwave', TRUE),
    ('Orquesta Prisma', 'Colombia', 'Sinfónico', TRUE),
    ('Circuito Norte', 'Perú', 'Techno', TRUE),
    ('Boreal Jazz', 'Colombia', 'Jazz fusión', TRUE)
ON CONFLICT (stage_name) DO NOTHING;


-- 3) Usuarios con correo reservado .test (no son personas reales)
INSERT INTO users (username, email, active) VALUES
    ('pp_admin', 'admin@pulsepass.test', TRUE),
    ('pp_staff_bog', 'staff.bogota@pulsepass.test', TRUE),
    ('pp_staff_med', 'staff.medellin@pulsepass.test', TRUE),
    ('pp_ana', 'ana@pulsepass.test', TRUE),
    ('pp_carlos', 'carlos@pulsepass.test', TRUE),
    ('pp_valentina', 'valentina@pulsepass.test', TRUE),
    ('pp_mateo', 'mateo@pulsepass.test', TRUE),
    ('pp_luisa', 'luisa@pulsepass.test', TRUE),
    ('pp_sofia', 'sofia@pulsepass.test', TRUE),
    ('pp_inactivo', 'inactivo@pulsepass.test', FALSE)
ON CONFLICT DO NOTHING;


-- 4) Perfiles 1:1: se enlazan por usuario/correo; sin asumir ID autogenerado
INSERT INTO user_profiles (user_id, first_name, last_name, phone, city, birth_date)
SELECT u.id, d.first_name, d.last_name, d.phone, d.city, d.birth_date::date
FROM (VALUES
    ('pp_admin', 'admin@pulsepass.test', 'Andrea', 'Administrador', '3001000001', 'Bogotá', '1992-02-12'),
    ('pp_staff_bog', 'staff.bogota@pulsepass.test', 'David', 'Torres', '3001000002', 'Bogotá', '1996-07-18'),
    ('pp_staff_med', 'staff.medellin@pulsepass.test', 'Juliana', 'Rendón', '3001000003', 'Medellín', '1994-03-06'),
    ('pp_ana', 'ana@pulsepass.test', 'Ana', 'Martínez', '3001000004', 'Bogotá', '2001-05-14'),
    ('pp_carlos', 'carlos@pulsepass.test', 'Carlos', 'Ramírez', '3001000005', 'Medellín', '1999-09-22'),
    ('pp_valentina', 'valentina@pulsepass.test', 'Valentina', 'Gómez', '3001000006', 'Cali', '2003-11-08'),
    ('pp_mateo', 'mateo@pulsepass.test', 'Mateo', 'Herrera', '3001000007', 'Barranquilla', '1998-01-17'),
    ('pp_luisa', 'luisa@pulsepass.test', 'Luisa', 'Fernández', '3001000008', 'Bucaramanga', '2005-06-30'),
    ('pp_sofia', 'sofia@pulsepass.test', 'Sofía', 'Morales', '3001000009', 'Bogotá', '2012-04-20'),
    ('pp_inactivo', 'inactivo@pulsepass.test', 'Prueba', 'Inactiva', '3001000010', 'Bogotá', '1990-08-25')
) AS d(username, email, first_name, last_name, phone, city, birth_date)
JOIN users u ON u.username = d.username AND u.email = d.email
ON CONFLICT (user_id) DO NOTHING;


-- 5) Credenciales BCrypt; evita contraseñas en texto plano
--    Si ya existe credencial, NO cambia su contraseña.
INSERT INTO user_credentials (user_id, password_hash, password_changed_at)
SELECT u.id, d.password_hash, CURRENT_TIMESTAMP
FROM (VALUES
    ('pp_admin', 'admin@pulsepass.test', '$2y$10$MPPpFoSJHnkh5f/m6oJUpuHMVUY51pMHqHCoBrwWabnofGVCzgXUy'),
    ('pp_staff_bog', 'staff.bogota@pulsepass.test', '$2y$10$GDPSrItv52vtkN5KLvUKRuPZdF/9AeGaLJzEtd4nTJk5woyGHE/A.'),
    ('pp_staff_med', 'staff.medellin@pulsepass.test', '$2y$10$BNkqBvtCjgtxvUSiMe6H/uw8zQ3rlyNryKWK5CKwThPyhfo8.LZ9S'),
    ('pp_ana', 'ana@pulsepass.test', '$2y$10$QeP2XqwSbnUJdEu4RYD74.RpLNU4ySBI2Xr0F8VGTc6rqeycCqfw.'),
    ('pp_carlos', 'carlos@pulsepass.test', '$2y$10$Jfe89wVK78LFH4LYlpZpjeGVHoNEITrLRU29/Xg0nrGlNCbNd7Hqm'),
    ('pp_valentina', 'valentina@pulsepass.test', '$2y$10$R.7Xo7ksqJx8I4VqXIf4Qu8UtLHg/PrF/9zJEMTXiIzEa67CKSA0O'),
    ('pp_mateo', 'mateo@pulsepass.test', '$2y$10$e4nlMGWsQlgbIKGsw4os2OOULABXGEf0FdiGoDW.nZ8n4SjQot6dS'),
    ('pp_luisa', 'luisa@pulsepass.test', '$2y$10$QMRx.mB0jQH.fJE9rABrZe9W9TU2Lv/cmahtpGgUDMYKnYOzEnZue'),
    ('pp_sofia', 'sofia@pulsepass.test', '$2y$10$pyNnrsbP.gx0GzHtZSKwauQQibYH7RSGVbXpibGCAiQgAVcg.Lys6'),
    ('pp_inactivo', 'inactivo@pulsepass.test', '$2y$10$QfbMPJWQTlOWSzw3QuJt2..RIQxBa3owrKC8cyWuHHbJQ0ELQczE.')
) AS d(username, email, password_hash)
JOIN users u ON u.username = d.username AND u.email = d.email
ON CONFLICT (user_id) DO NOTHING;


-- 6) Roles usados por la interfaz: ADMIN, STAFF y USER
INSERT INTO user_roles (user_id, role)
SELECT u.id, d.role
FROM (VALUES
    ('pp_admin', 'admin@pulsepass.test', 'ADMIN'),
    ('pp_staff_bog', 'staff.bogota@pulsepass.test', 'STAFF'),
    ('pp_staff_med', 'staff.medellin@pulsepass.test', 'STAFF'),
    ('pp_ana', 'ana@pulsepass.test', 'USER'),
    ('pp_carlos', 'carlos@pulsepass.test', 'USER'),
    ('pp_valentina', 'valentina@pulsepass.test', 'USER'),
    ('pp_mateo', 'mateo@pulsepass.test', 'USER'),
    ('pp_luisa', 'luisa@pulsepass.test', 'USER'),
    ('pp_sofia', 'sofia@pulsepass.test', 'USER'),
    ('pp_inactivo', 'inactivo@pulsepass.test', 'USER')
) AS d(username, email, role)
JOIN users u ON u.username = d.username AND u.email = d.email
ON CONFLICT (user_id, role) DO NOTHING;


-- 7) Eventos: positivos = días futuros, negativos = días pasados
--    Las fechas son relativas a la ejecución; una repetición no actualiza fechas.
INSERT INTO events (event_code, name, description, category, status, event_date, minimum_age, venue_id, streaming_url)
SELECT d.event_code, d.name, d.description, d.category, d.status,
       CURRENT_DATE::timestamp + (d.day_offset * INTERVAL '1 day') + (d.hour_of_day * INTERVAL '1 hour'),
       d.minimum_age, v.id, d.streaming_url
FROM (VALUES
    ('EVT-PP-001', 'Festival Pulso Urbano', 'Una noche de electrónica, hip hop y experiencias musicales.', 'MUSIC', 'PUBLISHED', 21, 19, 16, 'VEN-PP-BOG-01', NULL),
    ('EVT-PP-002', 'Noches de Synth Pop', 'Presentaciones de synth pop y luces de neón.', 'MUSIC', 'PUBLISHED', 35, 20, 14, 'VEN-PP-MED-01', NULL),
    ('EVT-PP-003', 'Cumbre de Desarrollo Web', 'Charlas sobre frontend, backend y arquitectura de software.', 'TECHNOLOGY', 'PUBLISHED', 17, 9, 0, 'VEN-PP-BOG-02', 'https://example.com/pulsepass/demo-stream'),
    ('EVT-PP-004', 'Expo Ciencia para Todos', 'Experimentos y divulgación científica para familias.', 'EDUCATION', 'PUBLISHED', 45, 10, 0, 'VEN-PP-CAL-01', NULL),
    ('EVT-PP-005', 'Copa Deportiva Capital', 'Encuentro deportivo y actividades para aficionados.', 'SPORTS', 'PUBLISHED', 50, 16, 0, 'VEN-PP-BOG-01', NULL),
    ('EVT-PP-006', 'Encuentro Cultural del Caribe', 'Música tradicional, gastronomía y actividades culturales.', 'CULTURE', 'PUBLISHED', 28, 17, 0, 'VEN-PP-BAQ-01', NULL),
    ('EVT-PP-007', 'Festival de Cine Alternativo', 'Proyecciones y conversatorios de cine independiente.', 'ENTERTAINMENT', 'PUBLISHED', 18, 18, 12, 'VEN-PP-CTG-01', NULL),
    ('EVT-PP-008', 'Concierto Íntimo Prisma', 'Presentación acústica para público adulto.', 'MUSIC', 'PUBLISHED', 9, 20, 18, 'VEN-PP-BGA-01', NULL),
    ('EVT-PP-009', 'Jornada de IA Responsable', 'Paneles sobre inteligencia artificial y ética tecnológica.', 'TECHNOLOGY', 'PUBLISHED', 65, 9, 0, 'VEN-PP-MED-01', 'https://example.com/pulsepass/ia-stream'),
    ('EVT-PP-010', 'Carrera Recreativa del Valle', 'Competencia recreativa con salida y llegada en la arena.', 'SPORTS', 'PUBLISHED', 12, 7, 0, 'VEN-PP-CAL-01', NULL),
    ('EVT-PP-011', 'Feria de Lectura y Cultura', 'Libros, arte y talleres para todas las edades.', 'CULTURE', 'PUBLISHED', 75, 10, 0, 'VEN-PP-PER-01', NULL),
    ('EVT-PP-012', 'Taller de Innovación Creativa', 'Laboratorios de diseño y prototipado.', 'EDUCATION', 'PUBLISHED', 42, 14, 0, 'VEN-PP-MAN-01', NULL),
    ('EVT-PP-013', 'Concierto Secreto', 'Borrador sin publicar para probar permisos de administrador.', 'MUSIC', 'DRAFT', 80, 21, 18, 'VEN-PP-BOG-01', NULL),
    ('EVT-PP-014', 'Hackathon PulsePass', 'Borrador para probar publicación y gestión de eventos.', 'TECHNOLOGY', 'DRAFT', 55, 8, 0, 'VEN-PP-BOG-02', NULL),
    ('EVT-PP-015', 'Festival Suspendido', 'Evento cancelado para comprobar los diferentes estados.', 'ENTERTAINMENT', 'CANCELLED', 15, 18, 0, 'VEN-PP-CTG-01', NULL),
    ('EVT-PP-016', 'Show de Fin de Temporada', 'Evento finalizado para consultar entradas históricas.', 'MUSIC', 'FINISHED', -30, 20, 0, 'VEN-PP-MED-01', NULL),
    ('EVT-PP-017', 'Show Casa Llena', 'Evento agotado con capacidad exactamente igual a una entrada pagada.', 'MUSIC', 'SOLD_OUT', 10, 20, 0, 'VEN-PP-TEST-01', NULL),
    ('EVT-PP-018', 'Encuentro de Fans', 'Evento de entretenimiento con reservas y cancelaciones de prueba.', 'ENTERTAINMENT', 'PUBLISHED', 90, 19, 0, 'VEN-PP-BOG-02', NULL)
) AS d(event_code, name, description, category, status, day_offset, hour_of_day, minimum_age, venue_code, streaming_url)
JOIN venues v ON v.code = d.venue_code
ON CONFLICT (event_code) DO NOTHING;


-- 8) Relación N:M eventos-artistas, incluyendo los 5 del catálogo Flyway V2
INSERT INTO event_artists (event_id, artist_id)
SELECT e.id, a.id
FROM (VALUES
    ('EVT-PP-001', 'Solar Beat'),
    ('EVT-PP-001', 'Pulsos Urbanos'),
    ('EVT-PP-001', 'Cumbia Cuántica'),
    ('EVT-PP-002', 'Neon Waves'),
    ('EVT-PP-002', 'Nómadas de Neón'),
    ('EVT-PP-006', 'Caribbean Sound'),
    ('EVT-PP-006', 'Bitácora Andina'),
    ('EVT-PP-008', 'Orquesta Prisma'),
    ('EVT-PP-008', 'Boreal Jazz'),
    ('EVT-PP-013', 'Páramo Eléctrico'),
    ('EVT-PP-016', 'Ocean Drive'),
    ('EVT-PP-017', 'Digital Pulse'),
    ('EVT-PP-017', 'Circuito Norte'),
    ('EVT-PP-018', 'Atlas Interactivo')
) AS d(event_code, stage_name)
JOIN events e ON e.event_code = d.event_code
JOIN artists a ON a.stage_name = d.stage_name
ON CONFLICT (event_id, artist_id) DO NOTHING;


-- 9) Tickets: PAID, CANCELLED, RESERVED y USED
--    Precios iguales a TicketPricingPolicy: GENERAL=100000, STUDENT=50000, VIP=200000, BACKSTAGE=350000.
--    TCK-PP-0028 es el único PAID del evento agotado EVT-PP-017 (sede de capacidad 1).
INSERT INTO tickets (ticket_code, type, price, status, purchase_date, user_id, event_id)
SELECT d.ticket_code, d.ticket_type, d.price::numeric(12,2), d.ticket_status,
       CURRENT_TIMESTAMP - (d.purchased_days_ago * INTERVAL '1 day'), u.id, e.id
FROM (VALUES
    ('TCK-PP-0001', 'GENERAL', '100000.00', 'PAID', 3, 'ana@pulsepass.test', 'EVT-PP-001'),
    ('TCK-PP-0002', 'VIP', '200000.00', 'PAID', 2, 'ana@pulsepass.test', 'EVT-PP-002'),
    ('TCK-PP-0003', 'STUDENT', '50000.00', 'PAID', 4, 'ana@pulsepass.test', 'EVT-PP-003'),
    ('TCK-PP-0004', 'GENERAL', '100000.00', 'PAID', 2, 'ana@pulsepass.test', 'EVT-PP-007'),
    ('TCK-PP-0005', 'GENERAL', '100000.00', 'CANCELLED', 1, 'ana@pulsepass.test', 'EVT-PP-018'),
    ('TCK-PP-0006', 'VIP', '200000.00', 'PAID', 7, 'carlos@pulsepass.test', 'EVT-PP-001'),
    ('TCK-PP-0007', 'GENERAL', '100000.00', 'PAID', 2, 'carlos@pulsepass.test', 'EVT-PP-005'),
    ('TCK-PP-0008', 'BACKSTAGE', '350000.00', 'PAID', 5, 'carlos@pulsepass.test', 'EVT-PP-008'),
    ('TCK-PP-0009', 'GENERAL', '100000.00', 'PAID', 1, 'carlos@pulsepass.test', 'EVT-PP-009'),
    ('TCK-PP-0010', 'GENERAL', '100000.00', 'RESERVED', 1, 'carlos@pulsepass.test', 'EVT-PP-018'),
    ('TCK-PP-0011', 'STUDENT', '50000.00', 'PAID', 2, 'valentina@pulsepass.test', 'EVT-PP-003'),
    ('TCK-PP-0012', 'GENERAL', '100000.00', 'PAID', 4, 'valentina@pulsepass.test', 'EVT-PP-004'),
    ('TCK-PP-0013', 'VIP', '200000.00', 'PAID', 2, 'valentina@pulsepass.test', 'EVT-PP-006'),
    ('TCK-PP-0014', 'GENERAL', '100000.00', 'PAID', 1, 'valentina@pulsepass.test', 'EVT-PP-011'),
    ('TCK-PP-0015', 'GENERAL', '100000.00', 'CANCELLED', 1, 'valentina@pulsepass.test', 'EVT-PP-018'),
    ('TCK-PP-0016', 'GENERAL', '100000.00', 'PAID', 2, 'mateo@pulsepass.test', 'EVT-PP-001'),
    ('TCK-PP-0017', 'VIP', '200000.00', 'PAID', 6, 'mateo@pulsepass.test', 'EVT-PP-006'),
    ('TCK-PP-0018', 'STUDENT', '50000.00', 'PAID', 1, 'mateo@pulsepass.test', 'EVT-PP-012'),
    ('TCK-PP-0019', 'GENERAL', '100000.00', 'PAID', 2, 'mateo@pulsepass.test', 'EVT-PP-010'),
    ('TCK-PP-0020', 'GENERAL', '100000.00', 'PAID', 3, 'luisa@pulsepass.test', 'EVT-PP-003'),
    ('TCK-PP-0021', 'STUDENT', '50000.00', 'PAID', 2, 'luisa@pulsepass.test', 'EVT-PP-004'),
    ('TCK-PP-0022', 'VIP', '200000.00', 'PAID', 1, 'luisa@pulsepass.test', 'EVT-PP-008'),
    ('TCK-PP-0023', 'GENERAL', '100000.00', 'PAID', 2, 'luisa@pulsepass.test', 'EVT-PP-011'),
    ('TCK-PP-0024', 'GENERAL', '100000.00', 'PAID', 1, 'sofia@pulsepass.test', 'EVT-PP-004'),
    ('TCK-PP-0025', 'STUDENT', '50000.00', 'PAID', 3, 'sofia@pulsepass.test', 'EVT-PP-010'),
    ('TCK-PP-0026', 'GENERAL', '100000.00', 'PAID', 2, 'sofia@pulsepass.test', 'EVT-PP-011'),
    ('TCK-PP-0027', 'GENERAL', '100000.00', 'PAID', 2, 'sofia@pulsepass.test', 'EVT-PP-012'),
    ('TCK-PP-0028', 'GENERAL', '100000.00', 'PAID', 1, 'ana@pulsepass.test', 'EVT-PP-017'),
    ('TCK-PP-0029', 'GENERAL', '100000.00', 'USED', 45, 'ana@pulsepass.test', 'EVT-PP-016'),
    ('TCK-PP-0030', 'VIP', '200000.00', 'USED', 44, 'carlos@pulsepass.test', 'EVT-PP-016'),
    ('TCK-PP-0031', 'GENERAL', '100000.00', 'CANCELLED', 40, 'mateo@pulsepass.test', 'EVT-PP-016'),
    ('TCK-PP-0032', 'GENERAL', '100000.00', 'CANCELLED', 3, 'luisa@pulsepass.test', 'EVT-PP-015'),
    ('TCK-PP-0033', 'GENERAL', '100000.00', 'RESERVED', 1, 'ana@pulsepass.test', 'EVT-PP-018'),
    ('TCK-PP-0034', 'VIP', '200000.00', 'PAID', 1, 'mateo@pulsepass.test', 'EVT-PP-018'),
    ('TCK-PP-0035', 'GENERAL', '100000.00', 'PAID', 2, 'valentina@pulsepass.test', 'EVT-PP-002'),
    ('TCK-PP-0036', 'STUDENT', '50000.00', 'PAID', 2, 'carlos@pulsepass.test', 'EVT-PP-003'),
    ('TCK-PP-0037', 'GENERAL', '100000.00', 'PAID', 3, 'luisa@pulsepass.test', 'EVT-PP-005'),
    ('TCK-PP-0038', 'GENERAL', '100000.00', 'PAID', 1, 'ana@pulsepass.test', 'EVT-PP-009'),
    ('TCK-PP-0039', 'GENERAL', '100000.00', 'PAID', 1, 'valentina@pulsepass.test', 'EVT-PP-010'),
    ('TCK-PP-0040', 'VIP', '200000.00', 'PAID', 1, 'carlos@pulsepass.test', 'EVT-PP-018')
) AS d(ticket_code, ticket_type, price, ticket_status, purchased_days_ago, email, event_code)
JOIN users u ON u.email = d.email
JOIN events e ON e.event_code = d.event_code
ON CONFLICT (ticket_code) DO NOTHING;


COMMIT;

-- Verificación de conteos de DEMOSTRACIÓN (siempre idempotentes):
SELECT 'venues' AS tabla, COUNT(*) AS datos_demo FROM venues WHERE code LIKE 'VEN-PP-%'
UNION ALL SELECT 'events', COUNT(*) FROM events WHERE event_code LIKE 'EVT-PP-%'
UNION ALL SELECT 'users', COUNT(*) FROM users WHERE username LIKE 'pp_%' AND email LIKE '%@pulsepass.test'
UNION ALL SELECT 'tickets', COUNT(*) FROM tickets WHERE ticket_code LIKE 'TCK-PP-%';
