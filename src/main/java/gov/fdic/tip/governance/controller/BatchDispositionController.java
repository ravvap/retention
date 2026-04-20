package gov.fdic.tip.governance.controller;

import gov.fdic.tip.governance.dto.BatchRunResponse;
import gov.fdic.tip.governance.service.BatchDispositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Epic 6 — Batch and Execution
 *
 * GOV-014: Automatically process governed items that have reached their eligibility date.
 *          Runs on a daily schedule; can also be triggered manually by TIP_ADMIN.
 *
 * Role matrix:
 *  Trigger manual sweep → TIP_ADMIN
 *  View run history     → TIP_ADMIN, MANAGER, AUDITOR
 */
@RestController
@RequestMapping("/api/v1/disposition/batch")
@RequiredArgsConstructor
@Tag(name = "Batch Disposition", description = "GOV-014 — Automated daily disposition sweep")
@SecurityRequirement(name = "bearerAuth")
public class BatchDispositionController {

    private final BatchDispositionService batchDispositionService;

    // ── GOV-014: Trigger a manual disposition sweep ──────────────────────────
    @PostMapping("/run")
    @PreAuthorize("hasAnyRole('TIP_ADMIN', 'SCHEDULER')")
    @Operation(
        summary = "GOV-014: Manually trigger a disposition sweep",
        description = "Runs the same logic as the scheduled daily sweep immediately. " +
                      "Restricted to TIP_ADMIN and the scheduler service account."
    )
    public ResponseEntity<BatchRunResponse> triggerSweep(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(batchDispositionService.runSweep(user.getUsername()));
    }

    // ── View batch run history ───────────────────────────────────────────────
    @GetMapping("/runs")
    @PreAuthorize("hasAnyRole('TIP_ADMIN', 'MANAGER', 'AUDITOR')")
    @Operation(summary = "GOV-014: List all past batch disposition runs")
    public ResponseEntity<List<BatchRunResponse>> getAllRuns() {
        return ResponseEntity.ok(batchDispositionService.getAllRuns());
    }
}
