package gov.fdic.tip.governance.config;

/**
 * Central constants file for the TIP Governance Service.
 *
 * Usage:
 *   import static gov.fdic.tip.governance.config.ApplicationConstants.*;
 *
 * Never hardcode strings directly in @PreAuthorize, @Table, @Column,
 * exception messages, or audit context keys — reference these constants instead.
 */
public final class ApplicationConstants {

    private ApplicationConstants() {}

    // =========================================================================
    // SECURITY — Roles
    // =========================================================================

    public static final class Roles {

        private Roles() {}

        public static final String TIP_ADMIN           = "ROLE_TIP_ADMIN";
        public static final String MANAGER             = "ROLE_MANAGER";
        public static final String SR_ANALYST          = "ROLE_SR_ANALYST";
        public static final String ANALYST             = "ROLE_ANALYST";
        public static final String COMPLIANCE_ANALYST  = "ROLE_COMPLIANCE_ANALYST";
        public static final String CASH_MGMT           = "ROLE_CASH_MGMT";
        public static final String AUDITOR             = "ROLE_AUDITOR";
        public static final String SCHEDULER           = "ROLE_SCHEDULER";

        /** SpEL expression — used directly in @PreAuthorize */
        public static final class Expr {
            private Expr() {}

            public static final String ADMIN_OR_MANAGER =
                    "hasAnyRole('TIP_ADMIN', 'MANAGER')";

            public static final String POLICY_VIEWERS =
                    "hasAnyRole('TIP_ADMIN', 'MANAGER', 'SR_ANALYST')";

            public static final String ITEM_REGISTRARS =
                    "hasAnyRole('TIP_ADMIN', 'MANAGER', 'SR_ANALYST', 'CASH_MGMT', 'COMPLIANCE_ANALYST')";

            public static final String ITEM_VIEWERS =
                    "hasAnyRole('TIP_ADMIN', 'MANAGER', 'SR_ANALYST', 'ANALYST', 'CASH_MGMT', 'COMPLIANCE_ANALYST', 'AUDITOR')";

            public static final String DISPOSITION_OPERATORS =
                    "hasAnyRole('TIP_ADMIN', 'MANAGER', 'SCHEDULER')";

            public static final String HOLD_MANAGERS =
                    "hasAnyRole('TIP_ADMIN', 'MANAGER')";

            public static final String HOLD_VIEWERS =
                    "hasAnyRole('TIP_ADMIN', 'MANAGER', 'CASH_MGMT', 'COMPLIANCE_ANALYST', 'AUDITOR')";

            public static final String AUDIT_VIEWERS =
                    "hasAnyRole('TIP_ADMIN', 'MANAGER', 'AUDITOR', 'CASH_MGMT', 'COMPLIANCE_ANALYST')";

            public static final String BATCH_OPERATORS =
                    "hasAnyRole('TIP_ADMIN', 'SCHEDULER')";

            public static final String BATCH_VIEWERS =
                    "hasAnyRole('TIP_ADMIN', 'MANAGER', 'AUDITOR')";
        }
    }

    // =========================================================================
    // DATABASE — Table names
    // =========================================================================

    public static final class Tables {

        private Tables() {}

        public static final String RETENTION_POLICY      = "retention_policy";
        public static final String GOVERNED_ITEM         = "governed_item";
        public static final String GOVERNED_ITEM_EVENT   = "governed_item_event";
        public static final String LEGAL_HOLD            = "legal_hold";
        public static final String LEGAL_HOLD_ITEM       = "legal_hold_item";
        public static final String AUDIT_EVENT           = "audit_event";
        public static final String BATCH_DISPOSITION_RUN = "batch_disposition_run";
    }

    // =========================================================================
    // DATABASE — Column names
    // =========================================================================

    public static final class Columns {

        private Columns() {}

        // retention_policy
        public static final String POLICY_ID              = "id";
        public static final String POLICY_NAME            = "name";
        public static final String POLICY_STATUS          = "status";
        public static final String CONTENT_TYPE           = "content_type";
        public static final String CLOCK_START_TRIGGER    = "clock_start_trigger";
        public static final String DISPOSITION_ACTION     = "disposition_action";
        public static final String ARCHIVE_PERIOD_VALUE   = "archive_period_value";
        public static final String ARCHIVE_PERIOD_UNIT    = "archive_period_unit";
        public static final String PURGE_PERIOD_VALUE     = "purge_period_value";
        public static final String PURGE_PERIOD_UNIT      = "purge_period_unit";
        public static final String CREATED_BY             = "created_by";
        public static final String CREATED_AT             = "created_at";
        public static final String ACTIVATED_BY           = "activated_by";
        public static final String ACTIVATED_AT           = "activated_at";
        public static final String RETIRED_BY             = "retired_by";
        public static final String RETIRED_AT             = "retired_at";

        // governed_item
        public static final String ITEM_TYPE                 = "item_type";
        public static final String DOCUMENT_REFERENCE        = "document_reference";
        public static final String DB_TABLE_NAME             = "db_table_name";
        public static final String DB_RECORD_KEY             = "db_record_key";
        public static final String SOURCE_SYSTEM             = "source_system";
        public static final String BUSINESS_CONTENT_CLASS    = "business_content_class";
        public static final String RETENTION_POLICY_ID       = "retention_policy_id";
        public static final String RETENTION_START_DATE      = "retention_start_date";
        public static final String ARCHIVE_ELIGIBILITY_DATE  = "archive_eligibility_date";
        public static final String PURGE_ELIGIBILITY_DATE    = "purge_eligibility_date";
        public static final String GOVERNANCE_STATUS         = "governance_status";
        public static final String ACTIVE_HOLD_COUNT         = "active_hold_count";
        public static final String REGISTERED_BY             = "registered_by";
        public static final String REGISTERED_AT             = "registered_at";

        // governed_item_event
        public static final String GOVERNED_ITEM_ID  = "governed_item_id";
        public static final String EVENT_TYPE        = "event_type";
        public static final String EVENT_DATE        = "event_date";
        public static final String RECORDED_BY       = "recorded_by";
        public static final String RECORDED_AT       = "recorded_at";

        // legal_hold
        public static final String HOLD_NAME          = "name";
        public static final String MATTER_REFERENCE   = "matter_reference";
        public static final String REASON             = "reason";
        public static final String HOLD_STATUS        = "status";
        public static final String RELEASED_BY        = "released_by";
        public static final String RELEASED_AT        = "released_at";

        // legal_hold_item
        public static final String LEGAL_HOLD_ID  = "legal_hold_id";
        public static final String APPLIED_BY     = "applied_by";
        public static final String APPLIED_AT     = "applied_at";

        // audit_event
        public static final String AUDIT_ACTION      = "action";
        public static final String AUDIT_RECORD_TYPE = "record_type";
        public static final String AUDIT_RECORD_ID   = "record_id";
        public static final String PERFORMED_BY      = "performed_by";
        public static final String PERFORMED_AT      = "performed_at";
        public static final String CONTEXT           = "context";

        // batch_disposition_run
        public static final String RUN_AT           = "run_at";
        public static final String ITEMS_EVALUATED  = "items_evaluated";
        public static final String ITEMS_ARCHIVED   = "items_archived";
        public static final String ITEMS_PURGED     = "items_purged";
        public static final String ITEMS_SKIPPED    = "items_skipped";
        public static final String ITEMS_ERROR      = "items_error";
        public static final String COMPLETED_AT     = "completed_at";
        public static final String SUMMARY          = "summary";
    }

    // =========================================================================
    // API — URL paths
    // =========================================================================

    public static final class Api {

        private Api() {}

        public static final String BASE                 = "/api/v1";

        public static final String AUTH                 = BASE + "/auth";
        public static final String AUTH_LOGIN           = AUTH + "/login";

        public static final String RETENTION_POLICIES  = BASE + "/retention-policies";
        public static final String POLICY_ACTIVATE     = "/{id}/activate";
        public static final String POLICY_RETIRE       = "/{id}/retire";

        public static final String GOVERNED_ITEMS      = BASE + "/governed-items";
        public static final String DOCUMENTS           = "/documents";
        public static final String DATABASE_RECORDS    = "/database-records";
        public static final String DISPOSITION_REC     = "/{id}/disposition-recommendation";
        public static final String ARCHIVE_DOCUMENT    = "/{id}/archive-document";
        public static final String ARCHIVE_DB_RECORD   = "/{id}/archive-database-record";
        public static final String PURGE_DOCUMENT      = "/{id}/purge-document";
        public static final String PURGE_DB_RECORD     = "/{id}/purge-database-record";
        public static final String ITEM_EVENTS         = "/{itemId}/events";

        public static final String LEGAL_HOLDS         = BASE + "/legal-holds";
        public static final String HOLD_APPLY          = "/{holdId}/apply";
        public static final String HOLD_RELEASE        = "/{holdId}/release";

        public static final String AUDIT               = BASE + "/audit";

        public static final String BATCH               = BASE + "/disposition/batch";
        public static final String BATCH_RUN           = "/run";
        public static final String BATCH_RUNS          = "/runs";

        /** Paths permitted without authentication (Swagger + auth endpoints) */
        public static final String[] PUBLIC_PATHS = {
            AUTH + "/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/api-docs",
            "/api-docs/**",
            "/webjars/**"
        };
    }

    // =========================================================================
    // JWT
    // =========================================================================

    public static final class Jwt {

        private Jwt() {}

        public static final String BEARER_PREFIX       = "Bearer ";
        public static final String AUTHORIZATION_HEADER = "Authorization";
        public static final String ROLES_CLAIM         = "roles";
    }

    // =========================================================================
    // Audit — context map keys
    // =========================================================================

    public static final class AuditKeys {

        private AuditKeys() {}

        public static final String POLICY_NAME           = "policyName";
        public static final String DOCUMENT_REFERENCE    = "documentReference";
        public static final String TABLE                  = "table";
        public static final String KEY                    = "key";
        public static final String HOLD_NAME              = "holdName";
        public static final String MATTER_REFERENCE       = "matterReference";
        public static final String LEGAL_HOLD_ID          = "legalHoldId";
        public static final String PRE_DELETION_STATUS    = "preDeletionStatus";
        public static final String PURGE_ELIGIBILITY_DATE = "purgeEligibilityDate";
        public static final String ARCHIVE_ELIGIBILITY_DATE = "archiveEligibilityDate";
        public static final String TRIGGER                = "trigger";
        public static final String ERROR                  = "error";
        public static final String EVALUATED              = "evaluated";
        public static final String ARCHIVED               = "archived";
        public static final String PURGED                 = "purged";
        public static final String SKIPPED                = "skipped";
        public static final String ERRORS                 = "errors";
    }

    // =========================================================================
    // Batch — sweep trigger labels
    // =========================================================================

    public static final class Batch {

        private Batch() {}

        public static final String TRIGGER_BATCH_SWEEP = "BatchSweep";
        public static final String CRON_DAILY_1AM      = "0 0 1 * * *";
        public static final String SCHEDULER_USER      = "scheduler";
    }

    // =========================================================================
    // Validation — error messages
    // =========================================================================

    public static final class ValidationMessages {

        private ValidationMessages() {}

        public static final String NAME_REQUIRED              = "Name is required.";
        public static final String CONTENT_TYPE_REQUIRED      = "Content type is required.";
        public static final String CLOCK_TRIGGER_REQUIRED     = "Clock start trigger is required.";
        public static final String DISPOSITION_ACTION_REQUIRED = "Disposition action is required.";
        public static final String POLICY_ID_REQUIRED         = "Retention policy ID is required.";
        public static final String START_DATE_REQUIRED        = "Retention start date is required.";
        public static final String SOURCE_SYSTEM_REQUIRED     = "Source system is required.";
        public static final String TABLE_NAME_REQUIRED        = "Database table name is required.";
        public static final String RECORD_KEY_REQUIRED        = "Database record key is required.";
        public static final String ITEM_IDS_REQUIRED          = "At least one governed item ID is required.";
        public static final String PERIOD_VALUE_POSITIVE      = "Period value must be greater than zero.";
    }

    // =========================================================================
    // Business rule — exception messages
    // =========================================================================

    public static final class ErrorMessages {

        private ErrorMessages() {}

        // Retention policy
        public static final String POLICY_ALREADY_ACTIVE =
                "Policy is already active.";
        public static final String POLICY_ALREADY_RETIRED =
                "Policy is already retired.";
        public static final String POLICY_RETIRED_CANNOT_ACTIVATE =
                "Policy has been retired and can no longer be activated.";
        public static final String POLICY_DRAFT_CANNOT_RETIRE =
                "Policy is in Draft status and has never been active; it does not need to be retired.";
        public static final String POLICY_MUST_BE_ACTIVE =
                "Policy must be Active to register items under it.";
        public static final String POLICY_NOT_FOR_DOCUMENTS =
                "Selected policy does not cover documents.";
        public static final String POLICY_NOT_FOR_DB_RECORDS =
                "Selected policy does not cover database records.";
        public static final String ARCHIVE_PERIOD_REQUIRED =
                "Both archive period value and unit are required for the selected disposition action.";
        public static final String PURGE_PERIOD_REQUIRED =
                "Both purge period value and unit are required for the selected disposition action.";
        public static final String ARCHIVE_PERIOD_UNIT_REQUIRED =
                "Archive period unit is required when value is supplied.";
        public static final String PURGE_PERIOD_UNIT_REQUIRED =
                "Purge period unit is required when value is supplied.";

        // Governed item
        public static final String ITEM_ON_HOLD_ARCHIVE =
                "Item is under active legal hold. Archive action abandoned.";
        public static final String ITEM_ON_HOLD_PURGE =
                "Item is under active legal hold. Purge action abandoned. Legal hold blocks purging under all circumstances.";
        public static final String ITEM_NOT_ACTIVE_FOR_ARCHIVE =
                "Only Active items can be archived. Current status: ";
        public static final String ITEM_TYPE_NOT_DOCUMENT =
                "This action is only applicable to documents.";
        public static final String ITEM_TYPE_NOT_DB_RECORD =
                "This action is only applicable to database records.";
        public static final String ARCHIVE_ENDPOINT_DOCUMENTS_ONLY =
                "This endpoint archives documents only. Use /archive-database-record for database records.";
        public static final String ARCHIVE_ENDPOINT_DB_ONLY =
                "This endpoint archives database records only. Use /archive-document for documents.";

        // Legal hold
        public static final String HOLD_NAME_REQUIRED =
                "A hold name is required.";
        public static final String HOLD_ALREADY_RELEASED =
                "Hold is already released. No change made.";
        public static final String HOLD_RELEASED_CANNOT_APPLY =
                "A released hold cannot be applied to items.";

        // Generic
        public static final String UNAUTHORIZED =
                "Unauthorized";
        public static final String ACCESS_DENIED =
                "Access denied";
    }

    // =========================================================================
    // OpenAPI / Swagger
    // =========================================================================

    public static final class OpenApi {

        private OpenApi() {}

        public static final String TITLE        = "TIP Governance API";
        public static final String VERSION      = "1.0.0";
        public static final String DESCRIPTION  =
                "FDIC TIP Governance Service — GOV-001 through GOV-019. CONTROLLED // FDIC INTERNAL ONLY";
        public static final String CONTACT_NAME  = "TIP Platform Team";
        public static final String CONTACT_EMAIL = "tip-platform@fdic.gov";
        public static final String SECURITY_SCHEME_NAME = "bearerAuth";
        public static final String BEARER_FORMAT = "JWT";

        public static final class Tags {
            private Tags() {}
            public static final String AUTH        = "Authentication";
            public static final String POLICIES    = "Retention Policies";
            public static final String ITEMS       = "Governed Items";
            public static final String ITEM_EVENTS = "Governed Item Events";
            public static final String HOLDS       = "Legal Holds";
            public static final String AUDIT       = "Audit Trail";
            public static final String BATCH       = "Batch Disposition";
        }
    }

    // =========================================================================
    // Scheduling
    // =========================================================================

    public static final class Schedule {

        private Schedule() {}

        /** Daily disposition sweep fires at 01:00 AM server time */
        public static final String DAILY_SWEEP_CRON = "0 0 1 * * *";
    }

    // =========================================================================
    // Misc
    // =========================================================================

    public static final class Misc {

        private Misc() {}

        public static final String DEFAULT_SCHEMA = "public";
        public static final String JSONB_TYPE     = "jsonb";
    }
}
