package gov.fdic.tip.governance.service;

import gov.fdic.tip.governance.dto.*;
import gov.fdic.tip.governance.entity.GovernedItem;
import gov.fdic.tip.governance.entity.RetentionPolicy;
import gov.fdic.tip.governance.enums.*;
import gov.fdic.tip.governance.exception.GovernanceBusinessException;
import gov.fdic.tip.governance.exception.ResourceNotFoundException;
import gov.fdic.tip.governance.repository.GovernedItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GovernedItemService Tests — GOV-005 to GOV-007, GOV-011, GOV-015 to GOV-018")
class GovernedItemServiceTest {

    @Mock private GovernedItemRepository governedItemRepository;
    @Mock private RetentionPolicyService policyService;
    @Mock private AuditService auditService;

    @InjectMocks private GovernedItemService governedItemService;

    private RetentionPolicy activeDocPolicy;
    private RetentionPolicy activeDbPolicy;
    private RetentionPolicy draftPolicy;
    private RetentionPolicy docOnlyPolicy;

    private GovernedItem activeDocItem;
    private GovernedItem itemOnHold;
    private GovernedItem archivedItem;

    @BeforeEach
    void setUp() {
        activeDocPolicy = RetentionPolicy.builder()
                .id(UUID.randomUUID()).name("Doc Policy")
                .status(PolicyStatus.Active)
                .contentType(ContentType.Document)
                .clockStartTrigger(ClockStartTrigger.CreationDate)
                .dispositionAction(DispositionAction.ArchiveThenPurge)
                .archivePeriodValue(3).archivePeriodUnit(TimeUnit.Years)
                .purgePeriodValue(7).purgePeriodUnit(TimeUnit.Years)
                .createdBy("admin").build();

        activeDbPolicy = RetentionPolicy.builder()
                .id(UUID.randomUUID()).name("DB Policy")
                .status(PolicyStatus.Active)
                .contentType(ContentType.DatabaseRecord)
                .clockStartTrigger(ClockStartTrigger.CreationDate)
                .dispositionAction(DispositionAction.PurgeOnly)
                .purgePeriodValue(5).purgePeriodUnit(TimeUnit.Years)
                .createdBy("admin").build();

        draftPolicy = RetentionPolicy.builder()
                .id(UUID.randomUUID()).name("Draft Policy")
                .status(PolicyStatus.Draft)
                .contentType(ContentType.Document)
                .clockStartTrigger(ClockStartTrigger.CreationDate)
                .dispositionAction(DispositionAction.PurgeOnly)
                .purgePeriodValue(3).purgePeriodUnit(TimeUnit.Years)
                .createdBy("admin").build();

        docOnlyPolicy = RetentionPolicy.builder()
                .id(UUID.randomUUID()).name("Doc Only Policy")
                .status(PolicyStatus.Active)
                .contentType(ContentType.Document)
                .clockStartTrigger(ClockStartTrigger.CreationDate)
                .dispositionAction(DispositionAction.PurgeOnly)
                .purgePeriodValue(3).purgePeriodUnit(TimeUnit.Years)
                .createdBy("admin").build();

        activeDocItem = GovernedItem.builder()
                .id(UUID.randomUUID())
                .itemType(ItemType.Document)
                .documentReference("DOC-2024-001")
                .retentionPolicy(activeDocPolicy)
                .retentionStartDate(LocalDate.of(2020, 1, 1))
                .archiveEligibilityDate(LocalDate.of(2023, 1, 1))
                .purgeEligibilityDate(LocalDate.of(2027, 1, 1))
                .sourceSystem("TIP-DMS")
                .governanceStatus(GovernanceStatus.Active)
                .activeHoldCount(0)
                .registeredBy("analyst")
                .registeredAt(OffsetDateTime.now()).build();

        itemOnHold = GovernedItem.builder()
                .id(UUID.randomUUID())
                .itemType(ItemType.Document)
                .documentReference("DOC-HOLD-001")
                .retentionPolicy(activeDocPolicy)
                .retentionStartDate(LocalDate.of(2018, 1, 1))
                .archiveEligibilityDate(LocalDate.of(2021, 1, 1))
                .purgeEligibilityDate(LocalDate.of(2025, 1, 1))
                .sourceSystem("TIP-DMS")
                .governanceStatus(GovernanceStatus.OnHold)
                .activeHoldCount(2)
                .registeredBy("analyst")
                .registeredAt(OffsetDateTime.now()).build();

        archivedItem = GovernedItem.builder()
                .id(UUID.randomUUID())
                .itemType(ItemType.Document)
                .documentReference("DOC-ARCH-001")
                .retentionPolicy(activeDocPolicy)
                .retentionStartDate(LocalDate.of(2017, 1, 1))
                .archiveEligibilityDate(LocalDate.of(2020, 1, 1))
                .purgeEligibilityDate(LocalDate.of(2024, 1, 1))
                .sourceSystem("TIP-DMS")
                .governanceStatus(GovernanceStatus.Archived)
                .activeHoldCount(0)
                .registeredBy("analyst")
                .registeredAt(OffsetDateTime.now()).build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GOV-005: Register Document
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GOV-005: Register a Document")
    class RegisterDocumentTests {

        @Test
        @DisplayName("Valid request creates governed item with computed eligibility dates")
        void registerDocument_valid_computesEligibilityDates() {
            RegisterDocumentRequest req = new RegisterDocumentRequest();
            req.setDocumentReference("DOC-NEW-001");
            req.setRetentionPolicyId(activeDocPolicy.getId());
            req.setRetentionStartDate(LocalDate.of(2021, 6, 1));
            req.setSourceSystem("TIP-DMS");

            when(policyService.findOrThrow(activeDocPolicy.getId())).thenReturn(activeDocPolicy);
            when(governedItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            GovernedItemResponse resp = governedItemService.registerDocument(req, "analyst");

            assertThat(resp.getArchiveEligibilityDate()).isEqualTo(LocalDate.of(2024, 6, 1)); // +3 years
            assertThat(resp.getPurgeEligibilityDate()).isEqualTo(LocalDate.of(2028, 6, 1));   // +7 years
            assertThat(resp.getGovernanceStatus()).isEqualTo(GovernanceStatus.Active);
            assertThat(resp.getActiveHoldCount()).isZero();
            verify(auditService).record(eq(AuditAction.ItemRegistered), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Draft policy rejected — policy must be Active")
        void registerDocument_draftPolicy_throwsException() {
            RegisterDocumentRequest req = new RegisterDocumentRequest();
            req.setDocumentReference("DOC-NEW-002");
            req.setRetentionPolicyId(draftPolicy.getId());
            req.setRetentionStartDate(LocalDate.now());
            req.setSourceSystem("TIP");

            when(policyService.findOrThrow(draftPolicy.getId())).thenReturn(draftPolicy);

            assertThatThrownBy(() -> governedItemService.registerDocument(req, "analyst"))
                    .isInstanceOf(GovernanceBusinessException.class)
                    .hasMessageContaining("Active");
        }

        @Test
        @DisplayName("DB-record-only policy rejected for document registration")
        void registerDocument_dbOnlyPolicy_throwsException() {
            RegisterDocumentRequest req = new RegisterDocumentRequest();
            req.setDocumentReference("DOC-NEW-003");
            req.setRetentionPolicyId(activeDbPolicy.getId());
            req.setRetentionStartDate(LocalDate.now());
            req.setSourceSystem("TIP");

            when(policyService.findOrThrow(activeDbPolicy.getId())).thenReturn(activeDbPolicy);

            assertThatThrownBy(() -> governedItemService.registerDocument(req, "analyst"))
                    .isInstanceOf(GovernanceBusinessException.class)
                    .hasMessageContaining("documents");
        }

        @Test
        @DisplayName("PurgeOnly policy: no archive eligibility date computed")
        void registerDocument_purgeOnlyPolicy_noArchiveDate() {
            RegisterDocumentRequest req = new RegisterDocumentRequest();
            req.setDocumentReference("DOC-PURGEONLY-001");
            req.setRetentionPolicyId(docOnlyPolicy.getId());
            req.setRetentionStartDate(LocalDate.of(2022, 1, 1));
            req.setSourceSystem("TIP");

            when(policyService.findOrThrow(docOnlyPolicy.getId())).thenReturn(docOnlyPolicy);
            when(governedItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            GovernedItemResponse resp = governedItemService.registerDocument(req, "analyst");

            assertThat(resp.getArchiveEligibilityDate()).isNull();
            assertThat(resp.getPurgeEligibilityDate()).isEqualTo(LocalDate.of(2025, 1, 1)); // +3 years
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GOV-006: Register Database Record
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GOV-006: Register a Database Record")
    class RegisterDbRecordTests {

        @Test
        @DisplayName("Valid DB record registration computes purge date")
        void registerDbRecord_valid_computesPurgeDate() {
            RegisterDbRecordRequest req = new RegisterDbRecordRequest();
            req.setDbTableName("assessment_transactions");
            req.setDbRecordKey(java.util.Map.of("transaction_id", 88742));
            req.setRetentionPolicyId(activeDbPolicy.getId());
            req.setRetentionStartDate(LocalDate.of(2021, 3, 15));
            req.setSourceSystem("TIP-CORE");

            when(policyService.findOrThrow(activeDbPolicy.getId())).thenReturn(activeDbPolicy);
            when(governedItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            GovernedItemResponse resp = governedItemService.registerDbRecord(req, "cashmgmt");

            assertThat(resp.getItemType()).isEqualTo(ItemType.DatabaseRecord);
            assertThat(resp.getPurgeEligibilityDate()).isEqualTo(LocalDate.of(2026, 3, 15)); // +5 years
            assertThat(resp.getArchiveEligibilityDate()).isNull();
        }

        @Test
        @DisplayName("Document-only policy rejected for DB record registration")
        void registerDbRecord_docOnlyPolicy_throwsException() {
            RegisterDbRecordRequest req = new RegisterDbRecordRequest();
            req.setDbTableName("some_table");
            req.setDbRecordKey(java.util.Map.of("id", 1));
            req.setRetentionPolicyId(docOnlyPolicy.getId());
            req.setRetentionStartDate(LocalDate.now());
            req.setSourceSystem("TIP");

            when(policyService.findOrThrow(docOnlyPolicy.getId())).thenReturn(docOnlyPolicy);

            assertThatThrownBy(() -> governedItemService.registerDbRecord(req, "analyst"))
                    .isInstanceOf(GovernanceBusinessException.class)
                    .hasMessageContaining("database records");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GOV-007: View Governed Item
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GOV-007: View a Governed Item")
    class ViewGovernedItemTests {

        @Test
        @DisplayName("Returns full governance details including disposition recommendation")
        void getById_active_returnsDetailsWithRecommendation() {
            UUID id = activeDocItem.getId();
            when(governedItemRepository.findById(id)).thenReturn(Optional.of(activeDocItem));

            GovernedItemResponse resp = governedItemService.getById(id);

            assertThat(resp.getId()).isEqualTo(id);
            assertThat(resp.getGovernanceStatus()).isEqualTo(GovernanceStatus.Active);
            assertThat(resp.getRetentionPolicyName()).isEqualTo("Doc Policy");
            assertThat(resp.getDispositionRecommendation()).isNotNull();
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when item not found")
        void getById_notFound_throwsException() {
            UUID id = UUID.randomUUID();
            when(governedItemRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> governedItemService.getById(id))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(id.toString());
        }

        @Test
        @DisplayName("Viewing does not produce audit entries")
        void getById_doesNotAudit() {
            UUID id = activeDocItem.getId();
            when(governedItemRepository.findById(id)).thenReturn(Optional.of(activeDocItem));

            governedItemService.getById(id);

            verifyNoInteractions(auditService);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GOV-011: Disposition Recommendation
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GOV-011: Disposition Recommendation Logic")
    class DispositionRecommendationTests {

        @Test
        @DisplayName("Item on hold → SkipOnHold regardless of dates")
        void recommendation_onHold_returnsSkipOnHold() {
            DispositionRecommendation rec = governedItemService.computeRecommendation(itemOnHold);
            assertThat(rec).isEqualTo(DispositionRecommendation.SkipOnHold);
        }

        @Test
        @DisplayName("Purge date passed → Purge (takes priority over archive)")
        void recommendation_purgeDatePassed_returnsPurge() {
            GovernedItem item = GovernedItem.builder()
                    .id(UUID.randomUUID())
                    .activeHoldCount(0)
                    .archiveEligibilityDate(LocalDate.now().minusYears(2))
                    .purgeEligibilityDate(LocalDate.now().minusDays(1))
                    .governanceStatus(GovernanceStatus.Archived)
                    .retentionPolicy(activeDocPolicy)
                    .registeredAt(OffsetDateTime.now())
                    .build();

            DispositionRecommendation rec = governedItemService.computeRecommendation(item);
            assertThat(rec).isEqualTo(DispositionRecommendation.Purge);
        }

        @Test
        @DisplayName("Archive date passed, purge not yet → Archive")
        void recommendation_archiveDatePassed_returnsArchive() {
            GovernedItem item = GovernedItem.builder()
                    .id(UUID.randomUUID())
                    .activeHoldCount(0)
                    .archiveEligibilityDate(LocalDate.now().minusDays(1))
                    .purgeEligibilityDate(LocalDate.now().plusYears(4))
                    .governanceStatus(GovernanceStatus.Active)
                    .retentionPolicy(activeDocPolicy)
                    .registeredAt(OffsetDateTime.now())
                    .build();

            DispositionRecommendation rec = governedItemService.computeRecommendation(item);
            assertThat(rec).isEqualTo(DispositionRecommendation.Archive);
        }

        @Test
        @DisplayName("No dates passed → Retain")
        void recommendation_noDatesPassed_returnsRetain() {
            GovernedItem item = GovernedItem.builder()
                    .id(UUID.randomUUID())
                    .activeHoldCount(0)
                    .archiveEligibilityDate(LocalDate.now().plusYears(1))
                    .purgeEligibilityDate(LocalDate.now().plusYears(5))
                    .governanceStatus(GovernanceStatus.Active)
                    .retentionPolicy(activeDocPolicy)
                    .registeredAt(OffsetDateTime.now())
                    .build();

            DispositionRecommendation rec = governedItemService.computeRecommendation(item);
            assertThat(rec).isEqualTo(DispositionRecommendation.Retain);
        }

        @Test
        @DisplayName("GOV-011: Recommendation does not trigger any action")
        void getRecommendation_doesNotModifyItem() {
            UUID id = activeDocItem.getId();
            when(governedItemRepository.findById(id)).thenReturn(Optional.of(activeDocItem));

            governedItemService.getRecommendation(id);

            verify(governedItemRepository, never()).save(any());
            verifyNoInteractions(auditService);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GOV-015: Archive Document
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GOV-015: Archive a Document")
    class ArchiveDocumentTests {

        @Test
        @DisplayName("Active document archived successfully")
        void archiveDocument_active_setsArchived() {
            UUID id = activeDocItem.getId();
            when(governedItemRepository.findById(id)).thenReturn(Optional.of(activeDocItem));
            when(governedItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            GovernedItemResponse resp = governedItemService.archiveDocument(id, "admin");

            assertThat(resp.getGovernanceStatus()).isEqualTo(GovernanceStatus.Archived);
            verify(auditService).record(eq(AuditAction.ItemArchived), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Document on hold cannot be archived — legal hold blocks")
        void archiveDocument_onHold_throwsException() {
            UUID id = itemOnHold.getId();
            when(governedItemRepository.findById(id)).thenReturn(Optional.of(itemOnHold));

            assertThatThrownBy(() -> governedItemService.archiveDocument(id, "admin"))
                    .isInstanceOf(GovernanceBusinessException.class)
                    .hasMessageContaining("legal hold");
        }

        @Test
        @DisplayName("Already archived item cannot be archived again")
        void archiveDocument_alreadyArchived_throwsException() {
            UUID id = archivedItem.getId();
            when(governedItemRepository.findById(id)).thenReturn(Optional.of(archivedItem));

            assertThatThrownBy(() -> governedItemService.archiveDocument(id, "admin"))
                    .isInstanceOf(GovernanceBusinessException.class)
                    .hasMessageContaining("Only Active items");
        }

        @Test
        @DisplayName("DB record cannot be archived via document endpoint")
        void archiveDocument_dbRecord_throwsException() {
            GovernedItem dbItem = GovernedItem.builder()
                    .id(UUID.randomUUID()).itemType(ItemType.DatabaseRecord)
                    .retentionPolicy(activeDbPolicy)
                    .governanceStatus(GovernanceStatus.Active).activeHoldCount(0)
                    .registeredAt(OffsetDateTime.now()).build();

            when(governedItemRepository.findById(dbItem.getId())).thenReturn(Optional.of(dbItem));

            assertThatThrownBy(() -> governedItemService.archiveDocument(dbItem.getId(), "admin"))
                    .isInstanceOf(GovernanceBusinessException.class)
                    .hasMessageContaining("documents");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GOV-017: Purge Document
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GOV-017: Purge a Document")
    class PurgeDocumentTests {

        @Test
        @DisplayName("Active document purged and pre-deletion audit record written")
        void purgeDocument_active_writePreDeletionAuditAndSetsPurged() {
            UUID id = activeDocItem.getId();
            when(governedItemRepository.findById(id)).thenReturn(Optional.of(activeDocItem));
            when(governedItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            GovernedItemResponse resp = governedItemService.purgeDocument(id, "admin");

            assertThat(resp.getGovernanceStatus()).isEqualTo(GovernanceStatus.Purged);
            // Pre-deletion audit must be written BEFORE status update
            verify(auditService).record(eq(AuditAction.ItemPurged), any(), eq(id), any(),
                    argThat(ctx -> ctx.containsKey("preDeletionStatus")));
        }

        @Test
        @DisplayName("Document on hold cannot be purged — legal hold blocks under all circumstances")
        void purgeDocument_onHold_throwsException() {
            UUID id = itemOnHold.getId();
            when(governedItemRepository.findById(id)).thenReturn(Optional.of(itemOnHold));

            assertThatThrownBy(() -> governedItemService.purgeDocument(id, "admin"))
                    .isInstanceOf(GovernanceBusinessException.class)
                    .hasMessageContaining("legal hold");

            verify(governedItemRepository, never()).save(any());
        }

        @Test
        @DisplayName("Archived document can be purged when purge date reached")
        void purgeDocument_archived_successfullyPurged() {
            archivedItem.setActiveHoldCount(0);
            UUID id = archivedItem.getId();
            when(governedItemRepository.findById(id)).thenReturn(Optional.of(archivedItem));
            when(governedItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            GovernedItemResponse resp = governedItemService.purgeDocument(id, "admin");

            assertThat(resp.getGovernanceStatus()).isEqualTo(GovernanceStatus.Purged);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GOV-016 / GOV-018: Archive/Purge DB Record
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GOV-016/018: Archive and Purge Database Records")
    class DbRecordDispositionTests {

        private GovernedItem activeDbItem;

        @BeforeEach
        void setUpDbItem() {
            activeDbItem = GovernedItem.builder()
                    .id(UUID.randomUUID())
                    .itemType(ItemType.DatabaseRecord)
                    .dbTableName("assessment_transactions")
                    .dbRecordKey(java.util.Map.of("id", 99))
                    .retentionPolicy(activeDbPolicy)
                    .retentionStartDate(LocalDate.of(2019, 1, 1))
                    .purgeEligibilityDate(LocalDate.of(2024, 1, 1))
                    .sourceSystem("TIP-CORE")
                    .governanceStatus(GovernanceStatus.Active)
                    .activeHoldCount(0)
                    .registeredBy("cashmgmt")
                    .registeredAt(OffsetDateTime.now()).build();
        }

        @Test
        @DisplayName("GOV-018: DB record purged with pre-deletion audit record")
        void purgeDbRecord_active_setPurged() {
            UUID id = activeDbItem.getId();
            when(governedItemRepository.findById(id)).thenReturn(Optional.of(activeDbItem));
            when(governedItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            GovernedItemResponse resp = governedItemService.purgeDbRecord(id, "admin");

            assertThat(resp.getGovernanceStatus()).isEqualTo(GovernanceStatus.Purged);
            verify(auditService).record(eq(AuditAction.ItemPurged), any(), eq(id), any(),
                    argThat(ctx -> ctx.containsKey("table")));
        }

        @Test
        @DisplayName("GOV-016: Document item rejected from DB record archive endpoint")
        void archiveDbRecord_documentItem_throwsException() {
            UUID id = activeDocItem.getId();
            when(governedItemRepository.findById(id)).thenReturn(Optional.of(activeDocItem));

            assertThatThrownBy(() -> governedItemService.archiveDbRecord(id, "admin"))
                    .isInstanceOf(GovernanceBusinessException.class)
                    .hasMessageContaining("database records");
        }
    }
}
