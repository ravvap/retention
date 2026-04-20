package gov.fdic.tip.governance.repository;

import gov.fdic.tip.governance.entity.GovernedItem;
import gov.fdic.tip.governance.enums.GovernanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GovernedItemRepository extends JpaRepository<GovernedItem, UUID> {

    Optional<GovernedItem> findByDocumentReference(String docRef);

    @Query("SELECT gi FROM GovernedItem gi WHERE gi.governanceStatus IN ('Active','Archived') " +
           "AND gi.activeHoldCount = 0 " +
           "AND (gi.archiveEligibilityDate <= :today OR gi.purgeEligibilityDate <= :today)")
    List<GovernedItem> findItemsDueForDisposition(LocalDate today);

    List<GovernedItem> findByGovernanceStatus(GovernanceStatus status);
}
