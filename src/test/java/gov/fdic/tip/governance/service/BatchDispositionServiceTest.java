package gov.fdic.tip.governance.service;

import gov.fdic.tip.governance.dto.BatchRunResponse;
import gov.fdic.tip.governance.entity.BatchDispositionRun;
import gov.fdic.tip.governance.entity.GovernedItem;
import gov.fdic.tip.governance.entity.RetentionPolicy;
import gov.fdic.tip.governance.enums.*;
import gov.fdic.tip.governance.repository.BatchDispositionRunRepository;
import gov.fdic.tip.governance.repository.GovernedItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BatchDispositionService Tests — GOV-014")
class BatchDispositionServiceTest {

    @Mock private GovernedItemRepository governedItemRepository;
    @Mock private BatchDispositionRunRepository runRepository;
    @Mock private GovernedItemService governedItemService;
    @Mock private AuditService auditService;

    @InjectMocks private BatchDispositionService batchService;

    private RetentionPolicy policy;
    private GovernedItem activeItemDueForPurge;
    private GovernedItem activeItemDueForArchive;
    private GovernedItem activeItemRetain;
    private GovernedItem itemOnHold;
    private BatchDispositionRun savedRun;

    @BeforeEach
    void setUp() {
        policy = RetentionPolicy.builder()
                .id(UUID.randomUUID()).name("Policy")
                .status(PolicyStatus.Active)
                .contentType(ContentType.Document)
                .clockStartTrigger(ClockStartTrigger.CreationDate)
                .dispositionAction(DispositionAction.ArchiveThenPurge)
                .archivePeriodValue(3).archivePeriodUnit(TimeUnit.Years)
                .purgePeriodValue(7).purgePeriodUnit(TimeUnit.Years)
                .createdBy("admin").build();

        activeItemDueForPurge = GovernedItem.builder()
                .id(UUID.randomUUID()).itemType(ItemType.Document)
                .documentReference("DOC-PURGE").retentionPolicy(policy)
                .retentionStartDate(LocalDate.of(2015, 1, 1))
                .archiveEligibilityDate(LocalDate.of(2018, 1, 1))
                .purgeEligibilityDate(LocalDate.of(2022, 1, 1))   // past
                .governanceStatus(GovernanceStatus.Active).activeHoldCount(0)
                .registeredBy("a").registeredAt(OffsetDateTime.now()).build();

        activeItemDueForArchive = GovernedItem.builder()
                .id(UUID.randomUUID()).itemType(ItemType.Document)
                .documentReference("DOC-ARCHIVE").retentionPolicy(policy)
                .retentionStartDate(LocalDate.of(2019, 1, 1))
                .archiveEligibilityDate(LocalDate.of(2022, 1, 1))   // past
                .purgeEligibilityDate(LocalDate.of(2026, 1, 1))     // future
                .governanceStatus(GovernanceStatus.Active).activeHoldCount(0)
                .registeredBy("a").registeredAt(OffsetDateTime.now()).build();

        activeItemRetain = GovernedItem.builder()
                .id(UUID.randomUUID()).itemType(ItemType.Document)
                .documentReference("DOC-RETAIN").retentionPolicy(policy)
                .retentionStartDate(LocalDate.now())
                .archiveEligibilityDate(LocalDate.now().plusYears(3))
                .purgeEligibilityDate(LocalDate.now().plusYears(7))
                .governanceStatus(GovernanceStatus.Active).activeHoldCount(0)
                .registeredBy("a").registeredAt(OffsetDateTime.now()).build();

        itemOnHold = GovernedItem.builder()
                .id(UUID.randomUUID()).itemType(ItemType.Document)
                .documentReference("DOC-HOLD").retentionPolicy(policy)
                .retentionStartDate(LocalDate.of(2015, 1, 1))
                .purgeEligibilityDate(LocalDate.of(2018, 1, 1))   // past but held
                .governanceStatus(GovernanceStatus.OnHold).activeHoldCount(1)
                .registeredBy("a").registeredAt(OffsetDateTime.now()).build();

        savedRun = BatchDispositionRun.builder().id(UUID.randomUUID()).build();
    }

    @Test
    @DisplayName("GOV-014: Purge-eligible item is purged in sweep")
    void runSweep_purgeEligibleItem_getsPurged() {
        when(governedItemRepository.findByGovernanceStatus(GovernanceStatus.Active))
                .thenReturn(List.of(activeItemDueForPurge));
        when(runRepository.save(any())).thenReturn(savedRun);
        when(governedItemService.computeRecommendation(activeItemDueForPurge))
                .thenReturn(DispositionRecommendation.Purge);

        BatchRunResponse result = batchService.runSweep("scheduler");

        verify(governedItemService).saveItem(argThat(i ->
                i.getGovernanceStatus() == GovernanceStatus.Purged));
        verify(auditService).record(eq(AuditAction.ItemPurged), any(), any(), any(), any());
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("GOV-014: Archive-eligible item is archived in sweep")
    void runSweep_archiveEligibleItem_getsArchived() {
        when(governedItemRepository.findByGovernanceStatus(GovernanceStatus.Active))
                .thenReturn(List.of(activeItemDueForArchive));
        when(runRepository.save(any())).thenReturn(savedRun);
        when(governedItemService.computeRecommendation(activeItemDueForArchive))
                .thenReturn(DispositionRecommendation.Archive);

        batchService.runSweep("scheduler");

        verify(governedItemService).saveItem(argThat(i ->
                i.getGovernanceStatus() == GovernanceStatus.Archived));
        verify(auditService).record(eq(AuditAction.ItemArchived), any(), any(), any(), any());
    }

    @Test
    @DisplayName("GOV-014: Items on hold are skipped — hold blocks all disposition")
    void runSweep_itemOnHold_skipped() {
        when(governedItemRepository.findByGovernanceStatus(GovernanceStatus.Active))
                .thenReturn(List.of(itemOnHold));
        when(runRepository.save(any())).thenReturn(savedRun);
        when(governedItemService.computeRecommendation(itemOnHold))
                .thenReturn(DispositionRecommendation.SkipOnHold);

        batchService.runSweep("scheduler");

        verify(governedItemService, never()).saveItem(any());
    }

    @Test
    @DisplayName("GOV-014: Retain items are skipped without error")
    void runSweep_retainItem_skipped() {
        when(governedItemRepository.findByGovernanceStatus(GovernanceStatus.Active))
                .thenReturn(List.of(activeItemRetain));
        when(runRepository.save(any())).thenReturn(savedRun);
        when(governedItemService.computeRecommendation(activeItemRetain))
                .thenReturn(DispositionRecommendation.Retain);

        batchService.runSweep("scheduler");

        verify(governedItemService, never()).saveItem(any());
    }

    @Test
    @DisplayName("GOV-014: Error on one item does not stop sweep — item marked Error")
    void runSweep_itemThrowsError_marksErrorAndContinues() {
        when(governedItemRepository.findByGovernanceStatus(GovernanceStatus.Active))
                .thenReturn(List.of(activeItemDueForPurge, activeItemRetain));
        when(runRepository.save(any())).thenReturn(savedRun);

        // First item throws, second should still be evaluated
        when(governedItemService.computeRecommendation(activeItemDueForPurge))
                .thenThrow(new RuntimeException("Simulated failure"));
        when(governedItemService.computeRecommendation(activeItemRetain))
                .thenReturn(DispositionRecommendation.Retain);

        batchService.runSweep("scheduler");

        // Error item saved with Error status
        verify(governedItemService).saveItem(argThat(i ->
                i.getGovernanceStatus() == GovernanceStatus.Error));
        verify(auditService).record(eq(AuditAction.ItemError), any(), any(), any(), any());
    }

    @Test
    @DisplayName("GOV-014: Batch run summary recorded in audit trail at end of sweep")
    void runSweep_writesAuditSummaryAtEnd() {
        when(governedItemRepository.findByGovernanceStatus(GovernanceStatus.Active))
                .thenReturn(List.of());
        when(runRepository.save(any())).thenReturn(savedRun);

        batchService.runSweep("scheduler");

        verify(auditService).record(eq(AuditAction.BatchRunCompleted), any(), any(), any(),
                argThat(ctx -> ctx.containsKey("evaluated") && ctx.containsKey("purged")));
    }

    @Test
    @DisplayName("GOV-014: Mixed sweep produces correct counts")
    void runSweep_mixedItems_correctCounts() {
        when(governedItemRepository.findByGovernanceStatus(GovernanceStatus.Active))
                .thenReturn(List.of(activeItemDueForPurge, activeItemDueForArchive,
                        activeItemRetain, itemOnHold));
        when(runRepository.save(any())).thenReturn(savedRun);
        when(governedItemService.computeRecommendation(activeItemDueForPurge))
                .thenReturn(DispositionRecommendation.Purge);
        when(governedItemService.computeRecommendation(activeItemDueForArchive))
                .thenReturn(DispositionRecommendation.Archive);
        when(governedItemService.computeRecommendation(activeItemRetain))
                .thenReturn(DispositionRecommendation.Retain);
        when(governedItemService.computeRecommendation(itemOnHold))
                .thenReturn(DispositionRecommendation.SkipOnHold);

        batchService.runSweep("scheduler");

        // Audit: 1 purge + 1 archive + 1 batch-complete = 3 audit calls
        ArgumentCaptor<AuditAction> actionCaptor = ArgumentCaptor.forClass(AuditAction.class);
        verify(auditService, atLeast(3)).record(actionCaptor.capture(), any(), any(), any(), any());

        List<AuditAction> actions = actionCaptor.getAllValues();
        assertThat(actions).contains(AuditAction.ItemPurged, AuditAction.ItemArchived,
                AuditAction.BatchRunCompleted);
    }
}
