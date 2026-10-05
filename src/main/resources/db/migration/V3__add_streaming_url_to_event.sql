-- =========================================================
-- PulsePass — V3__add_streaming_url_to_event.sql
-- FR-EVT-006: evento hibrido con URL de streaming opcional.
-- Migracion incremental: NO se modifica V1 (NFR-002/NFR-003).
-- =========================================================

ALTER TABLE events
    ADD COLUMN streaming_url VARCHAR(500);

COMMENT ON COLUMN events.streaming_url IS
    'URL opcional de transmision en vivo. NULL = evento solo presencial.';
