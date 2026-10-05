# PulsePass — Capa de persistencia

Plataforma de eventos, artistas y entradas. Este repositorio implementa
únicamente la **capa de persistencia** definida en el PRD: modelo relacional,
migraciones Flyway, entidades JPA, repositories Spring Data, consultas
derivadas y JPQL, y pruebas de integración contra PostgreSQL real.

Fuera de alcance: API REST, capa Service, autenticación, pagos, QR y frontend.

**Stack:** Java 21 · Spring Boot 4.x · Spring Data JPA / Hibernate · Flyway ·
PostgreSQL · Testcontainers · Maven.

---

## 1. Modelo de datos

| Entidad | Tabla | Atributos |
|---|---|---|
| `Venue` | `venues` | id, code, name, city, address, capacity, active |
| `Event` | `events` | id, eventCode, name, description, category, status, eventDate, minimumAge, streamingUrl, venue |
| `Artist` | `artists` | id, stageName, country, genre, active |
| `User` | `users` | id, username, email, active |
| `UserProfile` | `user_profiles` | id, user, firstName, lastName, phone, city, birthDate |
| `Ticket` | `tickets` | id, ticketCode, type, price, status, purchaseDate, user, event |
| — | `event_artists` | event_id, artist_id (PK compuesta) |

La tabla se llama `users` y no `user` porque `USER` es palabra reservada en
PostgreSQL.

### Enums

Todos se persisten con `@Enumerated(EnumType.STRING)` (BR-008): se guarda el
nombre estable, nunca el ordinal, que se rompería al reordenar el enum.

| Enum | Valores |
|---|---|
| `EventCategory` | MUSIC, SPORTS, TECHNOLOGY, EDUCATION, CULTURE, ENTERTAINMENT |
| `EventStatus` | DRAFT, PUBLISHED, SOLD_OUT, CANCELLED, FINISHED |
| `TicketType` | GENERAL, VIP, BACKSTAGE, STUDENT |
| `TicketStatus` | RESERVED, PAID, CANCELLED, USED |

---

## 2. Relaciones

```text
Venue  1 ──────── N  Event
Event  N ──────── M  Artist     (vía event_artists)
User   1 ──────── 1  UserProfile
User   1 ──────── N  Ticket
Event  1 ──────── N  Ticket
```

Propietario de cada relación (lado que tiene la FK física):

| Relación | Propietario | Columna |
|---|---|---|
| Venue ↔ Event | `Event` | `venue_id` |
| Event ↔ Artist | `Event` | `@JoinTable event_artists` |
| User ↔ UserProfile | `UserProfile` | `user_id` (FK + UNIQUE) |
| User ↔ Ticket | `Ticket` | `user_id` |
| Event ↔ Ticket | `Ticket` | `event_id` |

`Ticket` es una **entidad** y no un `@ManyToMany` entre `User` y `Event`
(BR-006) porque la relación tiene datos propios: código, tipo, precio, estado
y fecha de compra. Una tabla de unión simple no podría almacenarlos.

---

## 3. Integridad reforzada en PostgreSQL

| Regla | Mecanismo |
|---|---|
| `venues.code`, `events.event_code`, `artists.stage_name`, `users.username`, `users.email`, `tickets.ticket_code` | `UNIQUE` |
| Un solo perfil por usuario | `UNIQUE (user_id)` en `user_profiles` |
| Sin pares evento-artista repetidos | PK compuesta en `event_artists` |
| `capacity > 0` | `CHECK` |
| `price >= 0` | `CHECK` |
| Categorías, estados y tipos válidos | `CHECK ... IN (...)` |
| Precio monetario | `NUMERIC(12,2)` + `BigDecimal`, nunca float/double |

---

## 4. Ejecutar el proyecto

### Prerrequisitos

- Java 21
- Maven
- Docker corriendo (en Windows: Docker Desktop con backend WSL2)

### Compilar

```bash
mvn clean install
```

### Levantar contra una base local

`application.yml` usa variables de entorno con valores por defecto:

```yaml
url: ${DB_URL:jdbc:postgresql://localhost:5432/pulsepass}
username: ${DB_USER:postgres}
password: ${DB_PASSWORD:postgres}
```

```bash
mvn spring-boot:run
```

Flyway creará el esquema en la primera ejecución.

---

## 5. Ejecutar las pruebas

```bash
mvn clean test
```

Debe finalizar en `BUILD SUCCESS` (QT-010). Requiere Docker activo:
Testcontainers levanta un PostgreSQL real, Flyway aplica V1, V2 y V3 sobre esa
base vacía, Hibernate valida el esquema y luego corren los repositories.

Mientras corren, en otra terminal:

```bash
docker ps
```

se ve el contenedor `postgres` creado y destruido automáticamente.

También puedes correr `PulsePassPersistenceIT` desde el IDE con el botón ▶️.

---

## 6. Flyway

Flyway es el **único responsable** del esquema (NFR-002, NFR-003). Las
migraciones viven en `src/main/resources/db/migration` y se aplican en orden
según el número de versión del nombre del archivo, no según su ubicación:

| Migración | Objetivo |
|---|---|
| `V1__create_schema.sql` | Crea las 7 tablas con PK, FK, UNIQUE, CHECK e índices. |
| `V2__insert_initial_artists.sql` | Inserta el catálogo inicial: Solar Beat, Neon Waves, Caribbean Sound, Ocean Drive, Digital Pulse. |
| `V3__add_streaming_url_to_event.sql` | Agrega `streaming_url VARCHAR(500)` nullable a `events`, sin tocar V1. |

Por eso `spring.jpa.hibernate.ddl-auto` está en `validate`: Hibernate nunca
crea ni modifica tablas, solo compara las entidades contra el esquema real y
falla el arranque si algo no coincide. Esto evita que el esquema de cada
desarrollador diverja y hace que una base vacía pueda reconstruirse siempre
igual.

Una migración ya aplicada **no se modifica**: Flyway guarda su checksum en
`flyway_schema_history` y detectaría el cambio como una inconsistencia. Los
cambios estructurales se agregan como una migración nueva (V4, V5, ...).

---

## 7. Testcontainers

Las pruebas usan PostgreSQL real en un contenedor Docker, no H2 (NFR-004,
NFR-005):

```java
@Container
@ServiceConnection
static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:18-alpine")
                .withDatabaseName("pulsepass_test")
                .withUsername("pulsepass")
                .withPassword("pulsepass");
```

`@ServiceConnection` conecta el `DataSource` de Spring al contenedor
automáticamente (URL, usuario, password), sin configurar propiedades a mano.

Esto importa porque H2 no reproduce fielmente el comportamiento de PostgreSQL:
las violaciones de `CHECK`, el manejo de `UNIQUE` sobre la FK del 1:1, los
tipos `NUMERIC` y las palabras reservadas se comportan distinto. Una prueba
verde en H2 puede fallar en producción.

El contenedor es estático y se declara en `AbstractIntegrationTest`, de modo
que arranca una sola vez para toda la suite.

---

## 8. Query Methods implementados

Criterio (NFR-007): filtros directos y navegación simple de relaciones se
resuelven por nombre de método; lo demás usa JPQL.

### `VenueRepository`

| Método | Requisito |
|---|---|
| `findByCode(String)` | FR-VEN-001, AC-001 |
| `findByCityIgnoreCaseAndActiveTrueOrderByNameAsc(String)` | — |
| `existsByCode(String)` | FR-VEN-002 |

### `EventRepository`

| Método | Requisito |
|---|---|
| `findByEventCode(String)` | FR-EVT-002, AC-002 |
| `findByStatusOrderByEventDateAsc(EventStatus)` | FR-EVT-005, AC-006 |
| `findByVenue_CodeOrderByEventDateAsc(String)` | FR-VEN-004 (navega Event → Venue) |
| `findByVenue_CityIgnoreCaseAndStatusOrderByEventDateAsc(String, EventStatus)` | — |
| `findByCategoryAndStatusOrderByEventDateAsc(EventCategory, EventStatus)` | FR-EVT-004 |
| `findByStatusAndEventDateAfterOrderByEventDateAsc(EventStatus, LocalDateTime)` | — |
| `findByNameContainingIgnoreCaseOrderByEventDateAsc(String)` | — |
| `existsByEventCode(String)` | FR-EVT-002 |

### `ArtistRepository`

| Método | Requisito |
|---|---|
| `findByStageNameIgnoreCase(String)` | FR-ART-002 |
| `findByActiveTrueOrderByStageNameAsc()` | FR-ART-001 |
| `findByStageNameContainingIgnoreCase(String)` | — |

### `UserRepository`

| Método | Requisito |
|---|---|
| `findByEmailIgnoreCase(String)` | sección 14 del PRD |
| `findByUsername(String)` | FR-USR-001 |
| `findByActiveTrueOrderByUsernameAsc()` | — |
| `existsByEmailIgnoreCase(String)` / `existsByUsername(String)` | FR-USR-002 |

### `UserProfileRepository`

| Método | Requisito |
|---|---|
| `findByUser_EmailIgnoreCase(String)` | FR-USR-004 (navega Profile → User) |
| `findByUser_Username(String)` | — |
| `findByCityIgnoreCaseOrderByLastNameAsc(String)` | — |

### `TicketRepository`

| Método | Requisito |
|---|---|
| `findByTicketCode(String)` | FR-TKT-002 |
| `findByUser_EmailIgnoreCaseOrderByPurchaseDateDesc(String)` | FR-TKT-006 |
| `findByUser_EmailIgnoreCaseAndStatusOrderByPurchaseDateDesc(String, TicketStatus)` | FR-TKT-006 |
| `findByEvent_EventCodeAndStatusOrderByPurchaseDateAsc(String, TicketStatus)` | FR-TKT-007 |
| `findByEvent_EventCodeAndType(String, TicketType)` | FR-TKT-004 |
| `findByEvent_EventDateAfterOrderByEvent_EventDateAsc(LocalDateTime)` | FR-SRC-004 |
| `existsByTicketCode(String)` | FR-TKT-002 |

---

## 9. Consultas JPQL implementadas

### `EventRepository`

| Método | Requisito | Por qué JPQL |
|---|---|---|
| `findEventsByArtistStageName(String)` | FR-SRC-001, FR-ART-004, AC-007 | `JOIN` sobre N:M + `DISTINCT` para no repetir eventos. |
| `findEventsByCityAndArtist(String, String)` | FR-SRC-002 | Cruza dos asociaciones distintas (Venue y Artist). |
| `findRecommendedEvents(EventStatus, LocalDateTime, String, String)` | FR-SRC-003 | Cuatro filtros, `LOWER` + `LIKE`, `DISTINCT` y orden. |
| `findByVenueCodeWithArtists(String)` | — | `LEFT JOIN FETCH` para evitar N+1. |

### `ArtistRepository`

| Método | Requisito |
|---|---|
| `findArtistsByEventCode(String)` | FR-ART-003 — `JOIN` sobre el N:M desde el lado inverso. |

### `TicketRepository`

| Método | Requisito | Por qué JPQL |
|---|---|---|
| `countPaidTicketsByEventCode(String)` | FR-TKT-008, AC-008 | `COUNT`: solo se necesita el número, no las entidades. |
| `countByEventCodeAndStatus(String, TicketStatus)` | FR-TKT-008 | Igual, parametrizado por estado. |
| `sumPaidRevenueByEventCode(String)` | — | `SUM` + `COALESCE` para no devolver `null`. |
| `findUserTicketsWithEventDetails(String)` | — | `JOIN FETCH` de Event y Venue en una sola consulta. |

---

## 10. Capa de servicio

Implementa las reglas de negocio del PRD de Capa de Servicios: `VenueService`,
`EventService`, `ArtistService`, `UserService` y `TicketService`, cada uno
con su interfaz y su `@Service` en `service/impl`.

Principios aplicados (sección 7 del PRD):

- **SRV-001**: los servicios nunca retornan entidades JPA, solo DTOs (`record`).
- **SRV-002**: inyección por constructor en todas las implementaciones, sin `@Autowired` en campos.
- **SRV-003**: cada servicio tiene interfaz + implementación.
- **SRV-004**: lecturas con `@Transactional(readOnly = true)` a nivel de clase; las escrituras (`create`, `publish`, `addArtist`, `register`, `purchase`, `cancel`, `markAsUsed`) sobrescriben con `@Transactional`.
- **SRV-005**: las reglas de negocio viven en el Service; los repositories solo acceden a datos.

### Mapeo Entity → DTO

MapStruct genera las implementaciones en tiempo de compilación:

| Mapper | Transforma | Nota |
|---|---|---|
| `VenueMapper` | Venue → VenueResponse | Mapeo directo por nombre |
| `ArtistMapper` | Artist → ArtistResponse | Mapeo directo por nombre |
| `EventMapper` | Event → EventResponse / EventSummaryResponse | `venueCode`/`venueName` explícitos; usa `ArtistMapper` para `Set<Artist> → List<ArtistResponse>` |
| `UserMapper` | User → UserResponse | Navega `profile.firstName`, `profile.lastName`, `profile.phone`, `profile.city`, `profile.birthDate` |
| `TicketMapper` | Ticket → TicketResponse | Navega `user.email`, `event.eventCode`, `event.name` |

### Excepciones de dominio

| Excepción | Cuándo | Ejemplo |
|---|---|---|
| `ResourceNotFoundException` | El recurso no existe | `Event not found: CMF-2026` |
| `DuplicateResourceException` | Conflicto de unicidad | `Username already exists: andrea` |
| `BusinessRuleException` | El recurso existe pero la operación no es válida | `User does not meet minimum age for event CMF-2026` |

### Reglas de negocio implementadas

**EventService.create** (BR-EVENT-001..006): código único → venue existente →
venue activo → fecha futura → `minimumAge >= 0` → el estado inicial es
**siempre** `DRAFT`, sin importar lo que traiga el request.

**EventService.publish** (BR-EVENT-007..009): solo desde `DRAFT`, la fecha
debe seguir siendo futura y el venue debe seguir activo.

**EventService.addArtist** (BR-EVENT-010..011): evento y artista deben
existir, el evento no puede estar `CANCELLED` ni `FINISHED`, y no se permite
asociar el mismo artista dos veces.

**UserService.register** (BR-USER-001..005): `username` y `email` únicos,
`active = true` por defecto, `User` y `UserProfile` se guardan en la misma
transacción (`user` primero, porque `user_id` es la FK en `user_profiles`), y
`birthDate` no puede ser futura ni nula (se exige para poder validar la edad
mínima más adelante en `TicketService`).

**TicketService.purchase** (BR-TICKET-001..009), en una única transacción:
usuario existente y activo → evento existente, `PUBLISHED` y con fecha
futura → si `minimumAge > 0`, la edad se calcula con `Period.between(
profile.birthDate, event.eventDate)` → hay cupo (`paidTickets < capacity`) →
se calcula el precio con `TicketPricingPolicy` → se crea el ticket en estado
`PAID` → si esta compra agota la capacidad, el evento pasa a `SOLD_OUT` en la
misma transacción (BR-TICKET-008).

**TicketService.cancel** (BR-TICKET-010..012): solo un ticket `PAID` puede
cancelarse (esto excluye `USED` y `CANCELLED` de una vez), y no después de la
fecha del evento.

**TicketService.markAsUsed** (BR-TICKET-013..014): solo un ticket `PAID`
puede marcarse como usado (esto excluye `CANCELLED` de una vez).

### Estrategia de precios

`TicketPricingPolicy` (`service/pricing`) encapsula el cálculo de precio
(sección 28 del PRD) como una clase utilitaria sin estado — no tiene
colaboradores (no llama repositories ni hace I/O), así que no hay nada que un
mock le aportaría a la prueba:

| Tipo | Factor sobre el precio base ($100.000) |
|---|---|
| `GENERAL` | 1× → $100.000 |
| `STUDENT` | 0.5× → $50.000 |
| `VIP` | 2× → $200.000 |
| `BACKSTAGE` | 3.5× → $350.000 |

El precio **nunca** viaja desde el cliente: `PurchaseTicketRequest` no tiene
campo `price`.

### Unit tests de la capa de servicio

Todos bajo `src/test/java/com/pulsepass/service`, con
`@ExtendWith(MockitoExtension.class)` — **sin** `@SpringBootTest`, sin
PostgreSQL, sin Testcontainers (NFR-001). `TicketPricingPolicyTest` es la
única excepción: ni siquiera necesita Mockito, porque la clase que prueba no
tiene colaboradores.

| Archivo | Cubre |
|---|---|
| `VenueServiceImplTest` | BR-VENUE-001, BR-VENUE-002 |
| `ArtistServiceImplTest` | BR-ARTIST-001, BR-ARTIST-002 |
| `EventServiceImplTest` | TEST-EVENT-001..008, BR-EVENT-010/011 (`addArtist`) |
| `UserServiceImplTest` | TEST-USER-001..004 |
| `TicketServiceImplTest` | TEST-TICKET-001..012, escenario de aceptación (sección 47 del PRD) |
| `TicketPricingPolicyTest` | Estrategia de precios, sin mocks |

Patrón Arrange-Act-Assert en todos; los caminos inválidos verifican con
`verify(repository, never()).save(any())` que una operación que viola una
regla de negocio nunca llega a persistirse.

Ejecutar:

```bash
mvn clean test
```

---

## 11. Trazabilidad de las pruebas de persistencia

Las pruebas de integración están separadas por repository, un archivo por
clase, todas bajo `src/test/java/com/pulsepass` y heredando de
`PersistenceTestSupport` (que a su vez extiende `AbstractIntegrationTest`,
donde vive el contenedor Testcontainers):

| Archivo | Cubre |
|---|---|
| `FlywayMigrationIT` | QT-001, QT-002, FR-EVT-006 |
| `VenuePersistenceTest` | QT-003, AC-001, FR-VEN-002, FR-VEN-003, FR-VEN-004 |
| `EventRepositoryIT` | AC-002, AC-006, FR-EVT-002, FR-EVT-005, FR-EVT-006 |
| `EventArtistIT` | QT-005, AC-003, FR-ART-002, FR-ART-003, FR-ART-004 |
| `UserProfileIT` | QT-004, QT-009, AC-004, FR-USR-001, FR-USR-002 |
| `TicketRepositoryIT` | QT-006, QT-008, AC-005, AC-008, FR-TKT-001, FR-TKT-003 |
| `EventSearchIT` | AC-007, FR-SRC-002, FR-SRC-003, FR-SRC-004 |

Las violaciones de constraints se prueban con `saveAndFlush` (QT-009): sin el
flush explícito, la sentencia podría no llegar a PostgreSQL antes de que
termine la prueba y la excepción nunca se lanzaría.
