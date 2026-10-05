-- =========================================================
-- PulsePass — V1__create_schema.sql
-- Crea el esquema completo del MVP: venues, events, artists,
-- event_artists, users, user_profiles y tickets.
-- Cubre: NFR-001, NFR-002, NFR-003, seccion 7 y seccion 8 del PRD.
-- =========================================================

-- ---------------------------------------------------------
-- venues  (FR-VEN-001, FR-VEN-002, FR-VEN-003)
-- ---------------------------------------------------------
CREATE TABLE venues (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL,
    name        VARCHAR(150) NOT NULL,
    city        VARCHAR(100) NOT NULL,
    address     VARCHAR(255),
    capacity    INTEGER      NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT uk_venues_code UNIQUE (code),
    CONSTRAINT chk_venues_capacity CHECK (capacity > 0)
);

-- ---------------------------------------------------------
-- events  (FR-EVT-001..005, BR-001, BR-002, R-005)
-- streaming_url NO se define aqui: lo agrega V3 (FR-EVT-006).
-- ---------------------------------------------------------
CREATE TABLE events (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_code   VARCHAR(50)  NOT NULL,
    name         VARCHAR(150) NOT NULL,
    description  TEXT,
    category     VARCHAR(30)  NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    event_date   TIMESTAMP    NOT NULL,
    minimum_age  INTEGER      NOT NULL DEFAULT 0,
    venue_id     BIGINT       NOT NULL,

    CONSTRAINT uk_events_event_code UNIQUE (event_code),
    CONSTRAINT fk_events_venue
        FOREIGN KEY (venue_id) REFERENCES venues (id),
    CONSTRAINT chk_events_category CHECK (
        category IN ('MUSIC', 'SPORTS', 'TECHNOLOGY',
                     'EDUCATION', 'CULTURE', 'ENTERTAINMENT')
    ),
    CONSTRAINT chk_events_status CHECK (
        status IN ('DRAFT', 'PUBLISHED', 'SOLD_OUT',
                   'CANCELLED', 'FINISHED')
    ),
    CONSTRAINT chk_events_minimum_age CHECK (minimum_age >= 0)
);

-- ---------------------------------------------------------
-- artists  (FR-ART-001, FR-ART-002)
-- ---------------------------------------------------------
CREATE TABLE artists (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    stage_name  VARCHAR(150) NOT NULL,
    country     VARCHAR(100),
    genre       VARCHAR(100),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT uk_artists_stage_name UNIQUE (stage_name)
);

-- ---------------------------------------------------------
-- event_artists  (FR-ART-003, FR-ART-004, BR-003)
-- PK compuesta: impide duplicar el mismo par evento-artista.
-- ---------------------------------------------------------
CREATE TABLE event_artists (
    event_id   BIGINT NOT NULL,
    artist_id  BIGINT NOT NULL,

    CONSTRAINT pk_event_artists PRIMARY KEY (event_id, artist_id),
    CONSTRAINT fk_event_artists_event
        FOREIGN KEY (event_id) REFERENCES events (id),
    CONSTRAINT fk_event_artists_artist
        FOREIGN KEY (artist_id) REFERENCES artists (id)
);

-- ---------------------------------------------------------
-- users  (FR-USR-001, FR-USR-002, R-004)
-- "users" y no "user": USER es palabra reservada en PostgreSQL.
-- ---------------------------------------------------------
CREATE TABLE users (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username  VARCHAR(100) NOT NULL,
    email     VARCHAR(180) NOT NULL,
    active    BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
);

-- ---------------------------------------------------------
-- user_profiles  (FR-USR-003, FR-USR-004, BR-004)
-- user_id es FK + UNIQUE => garantiza el 1:1 a nivel de BD.
-- ---------------------------------------------------------
CREATE TABLE user_profiles (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    phone       VARCHAR(30),
    city        VARCHAR(100),
    birth_date  DATE,

    CONSTRAINT uk_user_profiles_user_id UNIQUE (user_id),
    CONSTRAINT fk_user_profiles_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

-- ---------------------------------------------------------
-- tickets  (FR-TKT-001..005, BR-005, BR-006, BR-007, NFR-008)
-- price es NUMERIC (nunca float/double).
-- ---------------------------------------------------------
CREATE TABLE tickets (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticket_code    VARCHAR(50)    NOT NULL,
    type           VARCHAR(20)    NOT NULL,
    price          NUMERIC(12, 2) NOT NULL,
    status         VARCHAR(20)    NOT NULL,
    purchase_date  TIMESTAMP      NOT NULL,
    user_id        BIGINT         NOT NULL,
    event_id       BIGINT         NOT NULL,

    CONSTRAINT uk_tickets_ticket_code UNIQUE (ticket_code),
    CONSTRAINT fk_tickets_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_tickets_event
        FOREIGN KEY (event_id) REFERENCES events (id),
    CONSTRAINT chk_tickets_type CHECK (
        type IN ('GENERAL', 'VIP', 'BACKSTAGE', 'STUDENT')
    ),
    CONSTRAINT chk_tickets_status CHECK (
        status IN ('RESERVED', 'PAID', 'CANCELLED', 'USED')
    ),
    CONSTRAINT chk_tickets_price CHECK (price >= 0)
);

-- ---------------------------------------------------------
-- Indices de apoyo a las consultas frecuentes del negocio.
-- Las PK y las UNIQUE ya generan su propio indice.
-- ---------------------------------------------------------
CREATE INDEX idx_venues_city            ON venues (city);

CREATE INDEX idx_events_venue_id        ON events (venue_id);
CREATE INDEX idx_events_status          ON events (status);
CREATE INDEX idx_events_event_date      ON events (event_date);
CREATE INDEX idx_events_category        ON events (category);

-- event_id ya es la columna lider de la PK compuesta;
-- artist_id necesita su propio indice para la navegacion inversa.
CREATE INDEX idx_event_artists_artist_id ON event_artists (artist_id);

CREATE INDEX idx_tickets_user_id        ON tickets (user_id);
CREATE INDEX idx_tickets_event_id       ON tickets (event_id);
CREATE INDEX idx_tickets_status         ON tickets (status);
