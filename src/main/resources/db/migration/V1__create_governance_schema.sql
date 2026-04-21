-- ─── Extensions ──────────────────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ─── Prefixed Enums ──────────────────────────────────────────────────────
DO $$ BEGIN
    CREATE TYPE gov_policy_status       AS ENUM ('Draft', 'Active', 'Retired');
    CREATE TYPE gov_content_type        AS ENUM ('Document', 'DatabaseRecord', 'Any');
    CREATE TYPE gov_clock_start_trigger AS ENUM ('CreationDate', 'EventDate');
    CREATE TYPE gov_disposition_action  AS ENUM ('ArchiveOnly', 'PurgeOnly', 'ArchiveThenPurge');
    CREATE TYPE gov_time_unit           AS ENUM ('Days', 'Months', 'Years');
    CREATE TYPE gov_governance_status   AS ENUM ('Active', 'OnHold', 'Archived', 'Purged', 'Error');
    CREATE TYPE gov_item_type           AS ENUM ('Document', 'DatabaseRecord');
    CREATE TYPE gov_hold_status         AS ENUM ('Active', 'Released');
EXCEPTION WHEN duplicate_object THEN null; END $$;

-- ─── 1. Retention Policy ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS gov_retention_policy (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    name                    VARCHAR(255)    NOT NULL,
    description             TEXT,
    content_classification  VARCHAR(255),
    content_type            gov_content_type    NOT NULL,
    clock_start_trigger     gov_clock_start_trigger NOT NULL,
    disposition_action      gov_disposition_action  NOT NULL,
    archive_period_value    INTEGER         CHECK (archive_period_value > 0),
    archive_period_unit     gov_time_unit,
    purge_period_value      INTEGER         CHECK (purge_period_value > 0),
    purge_period_unit       gov_time_unit,
    status                  gov_policy_status   NOT NULL DEFAULT 'Draft',
    
    -- Business Lifecycle (GOV-001 - GOV-004)
    activated_by            VARCHAR(255),
    activated_at            TIMESTAMPTZ,
    retired_by              VARCHAR(255),
    retired_at              TIMESTAMPTZ,

    -- Technical Audit (Spring Boot / JPA)
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by              VARCHAR(255)    NOT NULL,
    updated_at              TIMESTAMPTZ,
    updated_by              VARCHAR(255),
    deleted_at              TIMESTAMPTZ,
    deleted_by              VARCHAR(255),

    CONSTRAINT chk_archive_period_paired CHECK ((archive_period_value IS NULL) = (archive_period_unit IS NULL)),
    CONSTRAINT chk_purge_period_paired   CHECK ((purge_period_value IS NULL) = (purge_period_unit IS NULL))
);

CREATE UNIQUE INDEX IF NOT EXISTS uix_gov_policy_name 
    ON gov_retention_policy (name) WHERE status <> 'Retired';

-- ─── 2. Governed Item ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS gov_governed_item (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    item_type               gov_item_type       NOT NULL,
    document_reference      VARCHAR(500),
    db_table_name           VARCHAR(255),
    db_record_key           JSONB,
    source_system           VARCHAR(255)    NOT NULL,
    business_content_class  VARCHAR(255),
    retention_policy_id     UUID            NOT NULL REFERENCES gov_retention_policy(id),
    retention_start_date    DATE            NOT NULL,
    archive_eligibility_date DATE,
    purge_eligibility_date  DATE,
    governance_status       gov_governance_status NOT NULL DEFAULT 'Active',
    active_hold_count       INTEGER           NOT NULL DEFAULT 0 CHECK (active_hold_count >= 0),
    
    -- Technical Audit
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by              VARCHAR(255)    NOT NULL,
    updated_at              TIMESTAMPTZ,
    updated_by              VARCHAR(255),
    deleted_at              TIMESTAMPTZ,
    deleted_by              VARCHAR(255)
);

-- ─── 3. Governed Item Event ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS gov_governed_item_event (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    governed_item_id  UUID        NOT NULL REFERENCES gov_governed_item(id),
    event_type        VARCHAR(255) NOT NULL,
    event_date        DATE        NOT NULL,
    
    -- Technical Audit
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(255) NOT NULL,
    updated_at        TIMESTAMPTZ,
    updated_by        VARCHAR(255),
    deleted_at        TIMESTAMPTZ,
    deleted_by        VARCHAR(255)
);

-- ─── 4. Legal Hold ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS gov_legal_hold (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name              VARCHAR(255) NOT NULL UNIQUE,
    matter_reference  VARCHAR(255),
    reason            TEXT,
    status            gov_hold_status NOT NULL DEFAULT 'Active',
    
    -- Business Lifecycle
    released_by       VARCHAR(255),
    released_at       TIMESTAMPTZ,

    -- Technical Audit
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(255) NOT NULL,
    updated_at        TIMESTAMPTZ,
    updated_by        VARCHAR(255),
    deleted_at        TIMESTAMPTZ,
    deleted_by        VARCHAR(255)
);

-- ─── 5. Legal Hold Item ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS gov_legal_hold_item (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_hold_id     UUID        NOT NULL REFERENCES gov_legal_hold(id),
    governed_item_id  UUID        NOT NULL REFERENCES gov_governed_item(id),
    status            gov_hold_status NOT NULL DEFAULT 'Active',
    
    -- Technical Audit
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(255) NOT NULL,
    updated_at        TIMESTAMPTZ,
    updated_by        VARCHAR(255),
    deleted_at        TIMESTAMPTZ,
    deleted_by        VARCHAR(255),

    CONSTRAINT uq_gov_hold_item UNIQUE (legal_hold_id, governed_item_id)
);

-- ─── 6. Batch Disposition Run (GOV-014) ──────────────────────────────────
CREATE TABLE IF NOT EXISTS gov_batch_disposition_run (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    run_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    items_evaluated   INTEGER     NOT NULL DEFAULT 0,
    items_archived    INTEGER     NOT NULL DEFAULT 0,
    items_purged      INTEGER     NOT NULL DEFAULT 0,
    items_skipped     INTEGER     NOT NULL DEFAULT 0,
    items_error       INTEGER     NOT NULL DEFAULT 0,
    completed_at      TIMESTAMPTZ,
    summary           TEXT,
    
    -- Technical Audit
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(255) NOT NULL,
    updated_at        TIMESTAMPTZ,
    updated_by        VARCHAR(255),
    deleted_at        TIMESTAMPTZ,
    deleted_by        VARCHAR(255)
);

-- ─── 7. Error Log ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS gov_error_log (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    governed_item_id  UUID        REFERENCES gov_governed_item(id),
    batch_run_id      UUID        REFERENCES gov_batch_disposition_run(id),
    error_message     TEXT,
    stack_trace       TEXT,
    occurred_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─── 8. Audit Event (GOV-012, GOV-013) ────────────────────────────────────
-- Append-only for compliance.
CREATE TABLE IF NOT EXISTS gov_audit_event (
    id              BIGSERIAL       PRIMARY KEY,
    action          VARCHAR(100)    NOT NULL,
    record_type     VARCHAR(100)    NOT NULL,
    record_id       UUID            NOT NULL,
    performed_by    VARCHAR(255)    NOT NULL,
    performed_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    context         JSONB
);