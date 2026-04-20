package gov.fdic.tip.governance.repository;

import gov.fdic.tip.governance.entity.LegalHoldItem;
import gov.fdic.tip.governance.enums.HoldStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LegalHoldItemRepository extends JpaRepository<LegalHoldItem, UUID> {
    Optional<LegalHoldItem> findByLegalHoldIdAndGovernedItemId(UUID holdId, UUID itemId);
    List<LegalHoldItem> findByLegalHoldId(UUID holdId);
    List<LegalHoldItem> findByGovernedItemIdAndStatus(UUID itemId, HoldStatus status);
}
