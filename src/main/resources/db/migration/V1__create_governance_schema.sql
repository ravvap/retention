-- ==========================================================================
-- V1__create_governance_schema.sql
-- TIP Governance Database — Initial Schema
-- CONTROLLED // FDIC INTERNAL ONLY
-- Covers GOV-001 through GOV-019
-- ==========================================================================

-- ─── Extensions ──────────────────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ─── Enums ───────────────────────────────────────────────────────────────
DO $$ BEGIN
    CREATE TYPE policy_status       AS ENUM ('Draft', 'Active', 'Retired');
EXCEPTION WHEN duplicate_object THEN null; END $$;

DO $$ BEGIN
    CREATE TYPE content_type        AS ENUM ('Document', 'DatabaseRecord', 'Any');
EXCEPTION WHEN duplicate_object THEN null; END $$;

DO $$ BEGIN
    CREATE TYPE clock_start_trigger AS ENUM ('CreationDate', 'EventDate');
EXCEPTION WHEN duplicate_object THEN null; END $$;

DO $$ BEGIN
    CREATE TYPE disposition_action  AS ENUM ('ArchiveOnly', 'PurgeOnly', 'ArchiveThenPurge');
EXCEPTION WHEN duplicate_object THEN null; END $$;

DO $$ BEGIN
    CREATE TYPE time_unit           AS ENUM ('Days', 'Months', 'Years');
EXCEPTION WHEN duplicate_object THEN null; END $$;

DO $$ BEGIN
    CREATE TYPE governance_status   AS ENUM ('Active', 'OnHold', 'Archived', 'Purged', 'Error');
EXCEPTION WHEN duplicate_object THEN null; END $$;

DO $$ BEGIN
    CREATE TYPE item_type           AS ENUM ('Document', 'DatabaseRecord');
EXCEPTION WHEN duplicate_object THEN null; END $$;

DO $$ BEGIN
    CREATE TYPE hold_status         AS ENUM ('Active', 'Released');
EXCEPTION WHEN duplicate_object THEN null; END $$;

DO $$ BEGIN
    CREATE TYPE audit_action AS ENUM (
        'PolicyCreated', 'PolicyActivated', 'PolicyRetired',
        'ItemRegistered', 'LegalHoldCreated', 'LegalHoldApplied',
        'LegalHoldReleased', 'ItemArchived', 'ItemPurged',
        'BatchRunCompleted', 'ItemError'
    );
EXCEPTION WHEN duplicate_object THEN null; END $$;

DO $$ BEGIN
    CREATE TYPE audit_record_type AS ENUM ('RetentionPolicy', 'GovernedItem', 'LegalHold');
EXCEPTION WHEN duplicate_object THEN null; END $$;

-- ─── 1. Retention Policy ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS retention_policy (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    name                    VARCHAR(255)    NOT NULL,
    description             TEXT,
    content_classification  VARCHAR(255),
    content_type            content_type    NOT NULL,
    clock_start_trigger     clock_start_trigger NOT NULL,
    disposition_action      disposition_action  NOT NULL,
    archive_period_value    INTEGER         CHECK (archive_period_value > 0),
    archive_period_unit     time_unit,
    purge_period_value      INTEGER         CHECK (purge_period_value > 0),
    purge_period_unit       time_unit,
    status                  policy_status   NOT NULL DEFAULT 'Draft',
    created_by              VARCHAR(255)    NOT NULL,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    activated_by            VARCHAR(255),
    activated_at            TIMESTAMPTZ,
    retired_by              VARCHAR(255),
    retired_at              TIMESTAMPTZ,

    CONSTRAINT chk_archive_period_paired
        CHECK ((archive_period_value IS NULL) = (archive_period_unit IS NULL)),
    CONSTRAINT chk_purge_period_paired
        CHECK ((purge_period_value IS NULL) = (purge_period_unit IS NULL)),
    CONSTRAINT chk_archive_required
        CHECK (disposition_action = 'PurgeOnly'
               OR (archive_period_value IS NOT NULL AND archive_period_unit IS NOT NULL)),
    CONSTRAINT chk_purge_required
        CHECK (disposition_action = 'ArchiveOnly'
               OR (purge_period_value IS NOT NULL AND purge_period_unit IS NOT NULL))
);

CREATE UNIQUE INDEX IF NOT EXISTS uix_policy_name
    ON retention_policy (name) WHERE status <> 'Retired';

CREATE INDEX IF NOT EXISTS ix_policy_status ON retention_policy (status);

-- ─── 2. Governed Item ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS governed_item (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    item_type               item_type       NOT NULL,
    document_reference      VARCHAR(500),
    db_table_name           VARCHAR(255),
    db_record_key           JSONB,
    source_system           VARCHAR(255)    NOT NULL,
    business_content_class  VARCHAR(255),
    retention_policy_id     UUID            NOT NULL REFERENCES retention_policy(id),
    retention_start_date    DATE            NOT NULL,
    archive_eligibility_date DATE,
    purge_eligibility_date  DATE,
    governance_status       governance_status NOT NULL DEFAULT 'Active',
    active_hold_count       INTEGER           NOT NULL DEFAULT 0 CHECK (active_hold_count >= 0),
    registered_by           VARCHAR(255)    NOT NULL,
    registered_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_document_fields
        CHECK (item_type <> 'Document'
               OR (document_reference IS NOT NULL
                   AND db_table_name IS NULL AND db_record_key IS NULL)),
    CONSTRAINT chk_db_record_fields
        CHECK (item_type <> 'DatabaseRecord'
               OR (db_table_name IS NOT NULL AND db_record_key IS NOT NULL
                   AND document_reference IS NULL))
);

CREATE UNIQUE INDEX IF NOT EXISTS uix_governed_item_document
    ON governed_item (document_reference) WHERE item_type = 'Document';

CREATE UNIQUE INDEX IF NOT EXISTS uix_governed_item_db_record
    ON governed_item (db_table_name, db_record_key) WHERE item_type = 'DatabaseRecord';

CREATE INDEX IF NOT EXISTS ix_gi_archive_sweep
    ON governed_item (archive_eligibility_date)
    WHERE governance_status = 'Active' AND archive_eligibility_date IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_gi_purge_sweep
    ON governed_item (purge_eligibility_date)
    WHERE governance_status IN ('Active', 'Archived') AND purge_eligibility_date IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_gi_governance_status ON governed_item (governance_status);

-- ─── 3. Governed Item Event ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS governed_item_event (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    governed_item_id  UUID        NOT NULL REFERENCES governed_item(id),
    event_type        VARCHAR(255) NOT NULL,
    event_date        DATE        NOT NULL,
    recorded_by       VARCHAR(255) NOT NULL,
    recorded_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_gie_item ON governed_item_event (governed_item_id);

-- ─── 4. Legal Hold ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS legal_hold (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name              VARCHAR(255) NOT NULL UNIQUE,
    matter_reference  VARCHAR(255),
    reason            TEXT,
    status            hold_status NOT NULL DEFAULT 'Active',
    created_by        VARCHAR(255) NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    released_by       VARCHAR(255),
    released_at       TIMESTAMPTZ,

    CONSTRAINT chk_release_paired
        CHECK ((released_by IS NULL) = (released_at IS NULL))
);

CREATE INDEX IF NOT EXISTS ix_legal_hold_status ON legal_hold (status);

-- ─── 5. Legal Hold Item ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS legal_hold_item (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_hold_id     UUID        NOT NULL REFERENCES legal_hold(id),
    governed_item_id  UUID        NOT NULL REFERENCES governed_item(id),
    status            hold_status NOT NULL DEFAULT 'Active',
    applied_by        VARCHAR(255) NOT NULL,
    applied_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    released_by       VARCHAR(255),
    released_at       TIMESTAMPTZ,

    CONSTRAINT uq_hold_item UNIQUE (legal_hold_id, governed_item_id),
    CONSTRAINT chk_lhi_release_paired
        CHECK ((released_by IS NULL) = (released_at IS NULL))
);

CREATE INDEX IF NOT EXISTS ix_lhi_hold    ON legal_hold_item (legal_hold_id);
CREATE INDEX IF NOT EXISTS ix_lhi_item    ON legal_hold_item (governed_item_id);
CREATE INDEX IF NOT EXISTS ix_lhi_status  ON legal_hold_item (status);

-- ─── 6. Audit Event ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS audit_event (
    id              BIGSERIAL       PRIMARY KEY,
    action          audit_action    NOT NULL,
    record_type     audit_record_type NOT NULL,
    record_id       UUID            NOT NULL,
    performed_by    VARCHAR(255)    NOT NULL,
    performed_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    context         JSONB
);

CREATE INDEX IF NOT EXISTS ix_audit_record
    ON audit_event (record_type, record_id);
CREATE INDEX IF NOT EXISTS ix_audit_performed_at
    ON audit_event (performed_at DESC);

-- Append-only enforcement via RLS
ALTER TABLE audit_event ENABLE ROW LEVEL SECURITY;

 
CREATE POLICY audit_insert_only ON audit_event
    AS PERMISSIVE FOR INSERT TO PUBLIC WITH CHECK (true);
-- ─── 7. Batch Disposition Run ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS batch_disposition_run (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    run_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    items_evaluated   INTEGER     NOT NULL DEFAULT 0,
    items_archived    INTEGER     NOT NULL DEFAULT 0,
    items_purged      INTEGER     NOT NULL DEFAULT 0,
    items_skipped     INTEGER     NOT NULL DEFAULT 0,
    items_error       INTEGER     NOT NULL DEFAULT 0,
    completed_at      TIMESTAMPTZ,
    summary           TEXT
);

-- ==========================================================================
-- COMMENTS
-- ==========================================================================
COMMENT ON TABLE retention_policy      IS 'GOV-001 to GOV-004 — Defines reusable retention rules.';
COMMENT ON TABLE governed_item         IS 'GOV-005 to GOV-007 — One document or DB record under governance.';
COMMENT ON TABLE governed_item_event   IS 'Business events triggering EventDate clock start.';
COMMENT ON TABLE legal_hold            IS 'GOV-008, GOV-010, GOV-019 — Formal preservation order.';
COMMENT ON TABLE legal_hold_item       IS 'GOV-009, GOV-010 — Link between a hold and a governed item.';
COMMENT ON TABLE audit_event           IS 'GOV-012, GOV-013 — Tamper-evident append-only audit trail.';
COMMENT ON TABLE batch_disposition_run IS 'GOV-014 — Daily automated sweep run summary.';
