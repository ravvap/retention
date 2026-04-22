package gov.fdic.tip.governance.controller;

import gov.fdic.tip.governance.config.ApplicationConstants.OpenApi;
import gov.fdic.tip.governance.config.ApplicationConstants.Roles;
import gov.fdic.tip.governance.dto.RetentionPolicyRequest;
import gov.fdic.tip.governance.dto.RetentionPolicyResponse;
import gov.fdic.tip.governance.service.RetentionPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Epic 1 — Retention Policy Management
 *
 * GOV-001  Create a retention policy          → TIP Admin / Manager
 * GOV-002  Activate a retention policy        → TIP Admin / Manager
 * GOV-003  Retire a retention policy          → TIP Admin / Manager
 * GOV-004  View / List retention policies     → TIP Admin / Manager / Sr. Analyst
 * GOV-020  Edit a draft retention policy      → TIP Admin / Manager
 * GOV-021  Delete a draft retention policy    → TIP Admin / Manager
 */
@RestController
@RequestMapping("/api/v1/retention-policies")
@RequiredArgsConstructor
@Tag(name = OpenApi.Tags.POLICIES,
     description = "GOV-001, GOV-002, GOV-003, GOV-004, GOV-020, GOV-021 — Retention Policy Management")
@SecurityRequirement(name = OpenApi.SECURITY_SCHEME_NAME)
public class RetentionPolicyController {

    private final RetentionPolicyService policyService;

    // ── GOV-001: Create ──────────────────────────────────────────────────────
    @PostMapping
    @PreAuthorize(Roles.Expr.ADMIN_OR_MANAGER)
    @Operation(
        summary = "GOV-001: Create a retention policy",
        description = "Creates a new retention policy in Draft status. " +
                      "A Draft policy is visible but cannot be assigned to records until activated."
    )
    public ResponseEntity<RetentionPolicyResponse> create(
            @Valid @RequestBody RetentionPolicyRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(policyService.create(req, user.getUsername()));
    }

    // ── GOV-002: Activate ────────────────────────────────────────────────────
    @PostMapping("/{id}/activate")
    @PreAuthorize(Roles.Expr.ADMIN_OR_MANAGER)
    @Operation(
        summary = "GOV-002: Activate a retention policy",
        description = "Transitions a Draft policy to Active. " +
                      "Only Active policies can be assigned to governed items. " +
                      "Already-Active and Retired policies cannot be activated."
    )
    public ResponseEntity<RetentionPolicyResponse> activate(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(policyService.activate(id, user.getUsername()));
    }

    // ── GOV-003: Retire ──────────────────────────────────────────────────────
    @PostMapping("/{id}/retire")
    @PreAuthorize(Roles.Expr.ADMIN_OR_MANAGER)
    @Operation(
        summary = "GOV-003: Retire a retention policy",
        description = "Transitions an Active policy to Retired. " +
                      "Retired policies cannot be assigned to new records but continue to govern " +
                      "existing items. Draft policies have never been active and do not need to be retired."
    )
    public ResponseEntity<RetentionPolicyResponse> retire(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(policyService.retire(id, user.getUsername()));
    }

    // ── GOV-004: View ────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    @PreAuthorize(Roles.Expr.POLICY_VIEWERS)
    @Operation(
        summary = "GOV-004: View a retention policy",
        description = "Returns the full details of a retention policy. " +
                      "Viewing does not create an audit trail entry."
    )
    public ResponseEntity<RetentionPolicyResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(policyService.getById(id));
    }

    // ── GOV-004: List ────────────────────────────────────────────────────────
    @GetMapping
    @PreAuthorize(Roles.Expr.POLICY_VIEWERS)
    @Operation(
        summary = "GOV-004: List all retention policies",
        description = "Returns all retention policies regardless of status. " +
                      "Listing does not create an audit trail entry."
    )
    public ResponseEntity<List<RetentionPolicyResponse>> getAll() {
        return ResponseEntity.ok(policyService.getAll());
    }

    // ── GOV-020: Edit ────────────────────────────────────────────────────────
    @PutMapping("/{id}")
    @PreAuthorize(Roles.Expr.ADMIN_OR_MANAGER)
    @Operation(
        summary = "GOV-020: Edit a draft retention policy",
        description = "Updates any field on a Draft policy. " +
                      "All create-time validation rules apply equally when editing. " +
                      "Only Draft policies may be edited — " +
                      "Active policies are immutable (create a new policy instead), " +
                      "Retired policies are preserved for audit purposes and cannot be modified. " +
                      "The policy status remains Draft after a successful edit."
    )
    public ResponseEntity<RetentionPolicyResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody RetentionPolicyRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(policyService.update(id, req, user.getUsername()));
    }

    // ── GOV-021: Delete ──────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize(Roles.Expr.ADMIN_OR_MANAGER)
    @Operation(
        summary = "GOV-021: Delete a draft retention policy",
        description = "Permanently removes a Draft policy. " +
                      "Because a Draft policy cannot be assigned to any governed items, " +
                      "deletion has no effect on existing items, retention timelines, or legal holds. " +
                      "Active policies must be retired instead of deleted. " +
                      "Retired policies are preserved for audit purposes and cannot be deleted. " +
                      "The deletion is recorded in the audit trail with the policy's name and " +
                      "key attributes so the action remains investigable after the record is gone."
    )
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        policyService.delete(id, user.getUsername());
        return ResponseEntity.noContent().build();
    }
}
