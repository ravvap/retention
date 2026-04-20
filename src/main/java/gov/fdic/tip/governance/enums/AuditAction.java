package gov.fdic.tip.governance.enums;
public enum AuditAction {
    PolicyCreated, PolicyActivated, PolicyRetired,
    ItemRegistered, LegalHoldCreated, LegalHoldApplied,
    LegalHoldReleased, ItemArchived, ItemPurged,
    BatchRunCompleted, ItemError
}
