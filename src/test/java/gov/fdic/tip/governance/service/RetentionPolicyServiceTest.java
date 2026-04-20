package gov.fdic.tip.governance.service;

import gov.fdic.tip.governance.dto.RetentionPolicyRequest;
import gov.fdic.tip.governance.dto.RetentionPolicyResponse;
import gov.fdic.tip.governance.entity.RetentionPolicy;
import gov.fdic.tip.governance.enums.*;
import gov.fdic.tip.governance.exception.GovernanceBusinessException;
import gov.fdic.tip.governance.exception.ResourceNotFoundException;
import gov.fdic.tip.governance.repository.RetentionPolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RetentionPolicyService Tests — GOV-001 to GOV-004")
class RetentionPolicyServiceTest {

    @Mock private RetentionPolicyRepository policyRepository;
    @Mock private AuditService auditService;

    @InjectMocks private RetentionPolicyService policyService;

    private RetentionPolicyRequest validRequest;
    private RetentionPolicy draftPolicy;
    private RetentionPolicy activePolicy;

    @BeforeEach
    void setUp() {
        validRequest = new RetentionPolicyRequest();
        validRequest.setName("Test Policy");
        validRequest.setContentType(ContentType.Document);
        validRequest.setClockStartTrigger(ClockStartTrigger.CreationDate);
        validRequest.setDispositionAction(DispositionAction.ArchiveThenPurge);
        validRequest.setArchivePeriodValue(3);
        validRequest.setArchivePeriodUnit(TimeUnit.Years);
        validRequest.setPurgePeriodValue(7);
        validRequest.setPurgePeriodUnit(TimeUnit.Years);

        draftPolicy = RetentionPolicy.builder()
                .id(UUID.randomUUID())
                .name("Test Policy")
                .status(PolicyStatus.Draft)
                .contentType(ContentType.Document)
                .clockStartTrigger(ClockStartTrigger.CreationDate)
                .dispositionAction(DispositionAction.ArchiveThenPurge)
                .archivePeriodValue(3).archivePeriodUnit(TimeUnit.Years)
                .purgePeriodValue(7).purgePeriodUnit(TimeUnit.Years)
                .createdBy("admin")
                .build();

        activePolicy = RetentionPolicy.builder()
                .id(UUID.randomUUID())
                .name("Active Policy")
                .status(PolicyStatus.Active)
                .contentType(ContentType.Document)
                .clockStartTrigger(ClockStartTrigger.CreationDate)
                .dispositionAction(DispositionAction.PurgeOnly)
                .purgePeriodValue(5).purgePeriodUnit(TimeUnit.Years)
                .createdBy("admin")
                .build();
    }

    // ── GOV-001 ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GOV-001: Create policy saves and returns Draft status")
    void create_validRequest_returnsDraft() {
        when(policyRepository.save(any())).thenReturn(draftPolicy);

        RetentionPolicyResponse result = policyService.create(validRequest, "admin");

        assertThat(result.getStatus()).isEqualTo(PolicyStatus.Draft);
        verify(policyRepository).save(any());
        verify(auditService).record(eq(AuditAction.PolicyCreated), any(), any(), any(), any());
    }

    @Test
    @DisplayName("GOV-001: Missing archive period unit throws business exception")
    void create_archivePeriodMissingUnit_throwsException() {
        validRequest.setArchivePeriodUnit(null); // value set, unit missing

        assertThatThrownBy(() -> policyService.create(validRequest, "admin"))
                .isInstanceOf(GovernanceBusinessException.class)
                .hasMessageContaining("archive period");
    }

    @Test
    @DisplayName("GOV-001: PurgeOnly action without purge period throws exception")
    void create_purgeOnlyMissingPurgePeriod_throwsException() {
        validRequest.setDispositionAction(DispositionAction.PurgeOnly);
        validRequest.setPurgePeriodValue(null);
        validRequest.setPurgePeriodUnit(null);

        assertThatThrownBy(() -> policyService.create(validRequest, "admin"))
                .isInstanceOf(GovernanceBusinessException.class)
                .hasMessageContaining("purge period");
    }

    // ── GOV-002 ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GOV-002: Activate Draft policy sets status to Active")
    void activate_draftPolicy_setsActive() {
        UUID id = draftPolicy.getId();
        when(policyRepository.findById(id)).thenReturn(Optional.of(draftPolicy));
        when(policyRepository.save(any())).thenReturn(draftPolicy);

        RetentionPolicyResponse result = policyService.activate(id, "admin");

        assertThat(draftPolicy.getStatus()).isEqualTo(PolicyStatus.Active);
        verify(auditService).record(eq(AuditAction.PolicyActivated), any(), any(), any());
    }

    @Test
    @DisplayName("GOV-002: Activating an already-Active policy throws exception")
    void activate_alreadyActive_throwsException() {
        UUID id = activePolicy.getId();
        when(policyRepository.findById(id)).thenReturn(Optional.of(activePolicy));

        assertThatThrownBy(() -> policyService.activate(id, "admin"))
                .isInstanceOf(GovernanceBusinessException.class)
                .hasMessageContaining("already active");
    }

    @Test
    @DisplayName("GOV-002: Activating a Retired policy throws exception")
    void activate_retiredPolicy_throwsException() {
        RetentionPolicy retired = RetentionPolicy.builder()
                .id(UUID.randomUUID()).status(PolicyStatus.Retired).build();
        when(policyRepository.findById(retired.getId())).thenReturn(Optional.of(retired));

        assertThatThrownBy(() -> policyService.activate(retired.getId(), "admin"))
                .isInstanceOf(GovernanceBusinessException.class)
                .hasMessageContaining("retired");
    }

    // ── GOV-003 ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GOV-003: Retire an Active policy sets status to Retired")
    void retire_activePolicy_setsRetired() {
        UUID id = activePolicy.getId();
        when(policyRepository.findById(id)).thenReturn(Optional.of(activePolicy));
        when(policyRepository.save(any())).thenReturn(activePolicy);

        policyService.retire(id, "admin");

        assertThat(activePolicy.getStatus()).isEqualTo(PolicyStatus.Retired);
        verify(auditService).record(eq(AuditAction.PolicyRetired), any(), any(), any());
    }

    @Test
    @DisplayName("GOV-003: Retiring a Draft policy throws exception")
    void retire_draftPolicy_throwsException() {
        UUID id = draftPolicy.getId();
        when(policyRepository.findById(id)).thenReturn(Optional.of(draftPolicy));

        assertThatThrownBy(() -> policyService.retire(id, "admin"))
                .isInstanceOf(GovernanceBusinessException.class)
                .hasMessageContaining("Draft");
    }

    // ── GOV-004 ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GOV-004: getById returns policy when found")
    void getById_exists_returnsResponse() {
        UUID id = activePolicy.getId();
        when(policyRepository.findById(id)).thenReturn(Optional.of(activePolicy));

        RetentionPolicyResponse result = policyService.getById(id);

        assertThat(result.getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("GOV-004: getById throws ResourceNotFoundException when not found")
    void getById_notFound_throwsException() {
        UUID id = UUID.randomUUID();
        when(policyRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> policyService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
