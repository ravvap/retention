package gov.fdic.tip.governance.service;

import gov.fdic.tip.governance.dto.ApplyHoldRequest;
import gov.fdic.tip.governance.dto.LegalHoldRequest;
import gov.fdic.tip.governance.dto.LegalHoldResponse;
import gov.fdic.tip.governance.entity.GovernedItem;
import gov.fdic.tip.governance.entity.LegalHold;
import gov.fdic.tip.governance.entity.LegalHoldItem;
import gov.fdic.tip.governance.entity.RetentionPolicy;
import gov.fdic.tip.governance.enums.*;
import gov.fdic.tip.governance.exception.GovernanceBusinessException;
import gov.fdic.tip.governance.exception.ResourceNotFoundException;
import gov.fdic.tip.governance.repository.GovernedItemRepository;
import gov.fdic.tip.governance.repository.LegalHoldItemRepository;
import gov.fdic.tip.governance.repository.LegalHoldRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LegalHoldService Tests — GOV-008 to GOV-010, GOV-019")
class LegalHoldServiceTest {

    @Mock private LegalHoldRepository holdRepository;
    @Mock private LegalHoldItemRepository holdItemRepository;
    @Mock private GovernedItemRepository governedItemRepository;
    @Mock private AuditService auditService;

    @InjectMocks private LegalHoldService legalHoldService;

    private LegalHold activeHold;
    private LegalHold releasedHold;
    private GovernedItem activeItem;

    @BeforeEach
    void setUp() {
        activeHold = LegalHold.builder()
                .id(UUID.randomUUID()).name("Hold A")
                .matterReference("MAT-001").reason("Litigation")
                .status(HoldStatus.Active).createdBy("admin")
                .createdAt(OffsetDateTime.now()).build();

        releasedHold = LegalHold.builder()
                .id(UUID.randomUUID()).name("Hold B")
                .status(HoldStatus.Released).createdBy("admin")
                .createdAt(OffsetDateTime.now())
                .releasedBy("admin").releasedAt(OffsetDateTime.now()).build();

        RetentionPolicy policy = RetentionPolicy.builder()
                .id(UUID.randomUUID()).name("P1")
                .status(PolicyStatus.Active)
                .contentType(ContentType.Document)
                .clockStartTrigger(ClockStartTrigger.CreationDate)
                .dispositionAction(DispositionAction.PurgeOnly)
                .purgePeriodValue(5).purgePeriodUnit(TimeUnit.Years)
                .createdBy("admin").build();

        activeItem = GovernedItem.builder()
                .id(UUID.randomUUID())
                .itemType(ItemType.Document)
                .documentReference("DOC-001")
                .retentionPolicy(policy)
                .retentionStartDate(LocalDate.now())
                .sourceSystem("TIP")
                .governanceStatus(GovernanceStatus.Active)
                .activeHoldCount(0)
                .registeredBy("analyst")
                .registeredAt(OffsetDateTime.now()).build();
    }

    // ── GOV-008 ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GOV-008: Create legal hold starts as Active")
    void create_validRequest_holdsActive() {
        LegalHoldRequest req = new LegalHoldRequest();
        req.setName("New Hold");
        req.setMatterReference("MAT-002");
        req.setReason("Regulatory inquiry");

        when(holdRepository.save(any())).thenReturn(activeHold);

        LegalHoldResponse resp = legalHoldService.create(req, "admin");

        assertThat(resp.getStatus()).isEqualTo(HoldStatus.Active);
        verify(auditService).record(eq(AuditAction.LegalHoldCreated), any(), any(), any(), any());
    }

    @Test
    @DisplayName("GOV-008: Create hold without name throws exception")
    void create_blankName_throwsException() {
        LegalHoldRequest req = new LegalHoldRequest();
        req.setName("");

        assertThatThrownBy(() -> legalHoldService.create(req, "admin"))
                .isInstanceOf(GovernanceBusinessException.class)
                .hasMessageContaining("name is required");
    }

    // ── GOV-009 ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GOV-009: Apply hold to item increments hold count and sets OnHold")
    void applyToItems_validItem_incrementsCount() {
        ApplyHoldRequest req = new ApplyHoldRequest();
        req.setGovernedItemIds(List.of(activeItem.getId()));

        when(holdRepository.findById(activeHold.getId())).thenReturn(Optional.of(activeHold));
        when(governedItemRepository.existsById(activeItem.getId())).thenReturn(true);
        when(holdItemRepository.findByLegalHoldIdAndGovernedItemId(any(), any()))
                .thenReturn(Optional.empty());
        when(governedItemRepository.findById(activeItem.getId())).thenReturn(Optional.of(activeItem));
        when(holdItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(governedItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        legalHoldService.applyToItems(activeHold.getId(), req, "admin");

        assertThat(activeItem.getActiveHoldCount()).isEqualTo(1);
        assertThat(activeItem.getGovernanceStatus()).isEqualTo(GovernanceStatus.OnHold);
        verify(auditService).record(eq(AuditAction.LegalHoldApplied), any(), any(), any(), any());
    }

    @Test
    @DisplayName("GOV-009: Apply released hold is rejected")
    void applyToItems_releasedHold_throwsException() {
        ApplyHoldRequest req = new ApplyHoldRequest();
        req.setGovernedItemIds(List.of(UUID.randomUUID()));

        when(holdRepository.findById(releasedHold.getId())).thenReturn(Optional.of(releasedHold));

        assertThatThrownBy(() -> legalHoldService.applyToItems(releasedHold.getId(), req, "admin"))
                .isInstanceOf(GovernanceBusinessException.class)
                .hasMessageContaining("released hold");
    }

    @Test
    @DisplayName("GOV-009: If any item not found, entire operation cancelled")
    void applyToItems_missingItem_cancelsAll() {
        UUID missingId = UUID.randomUUID();
        ApplyHoldRequest req = new ApplyHoldRequest();
        req.setGovernedItemIds(List.of(activeItem.getId(), missingId));

        when(holdRepository.findById(activeHold.getId())).thenReturn(Optional.of(activeHold));
        when(governedItemRepository.existsById(activeItem.getId())).thenReturn(true);
        when(governedItemRepository.existsById(missingId)).thenReturn(false);

        assertThatThrownBy(() -> legalHoldService.applyToItems(activeHold.getId(), req, "admin"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No changes applied");

        verify(holdItemRepository, never()).save(any());
    }

    // ── GOV-010 ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GOV-010: Release hold sets hold to Released")
    void release_activeHold_setsReleased() {
        activeItem.setActiveHoldCount(1);
        activeItem.setGovernanceStatus(GovernanceStatus.OnHold);

        LegalHoldItem link = LegalHoldItem.builder()
                .id(UUID.randomUUID()).legalHold(activeHold)
                .governedItem(activeItem).status(HoldStatus.Active)
                .appliedBy("admin").build();

        when(holdRepository.findById(activeHold.getId())).thenReturn(Optional.of(activeHold));
        when(holdItemRepository.findByLegalHoldId(activeHold.getId())).thenReturn(List.of(link));
        when(holdItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(governedItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(holdRepository.save(any())).thenReturn(activeHold);

        legalHoldService.release(activeHold.getId(), "admin");

        assertThat(activeHold.getStatus()).isEqualTo(HoldStatus.Released);
        assertThat(activeItem.getActiveHoldCount()).isEqualTo(0);
        assertThat(activeItem.getGovernanceStatus()).isEqualTo(GovernanceStatus.Active);
        verify(auditService).record(eq(AuditAction.LegalHoldReleased), any(), any(), any());
    }

    @Test
    @DisplayName("GOV-010: Releasing an already-released hold throws exception")
    void release_alreadyReleased_throwsException() {
        when(holdRepository.findById(releasedHold.getId())).thenReturn(Optional.of(releasedHold));

        assertThatThrownBy(() -> legalHoldService.release(releasedHold.getId(), "admin"))
                .isInstanceOf(GovernanceBusinessException.class)
                .hasMessageContaining("already released");
    }

    // ── GOV-019 ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GOV-019: View legal hold does NOT create audit entry")
    void getById_doesNotAudit() {
        when(holdRepository.findById(activeHold.getId())).thenReturn(Optional.of(activeHold));
        when(holdItemRepository.findByLegalHoldId(any())).thenReturn(List.of());

        legalHoldService.getById(activeHold.getId());

        verifyNoInteractions(auditService);
    }
}
