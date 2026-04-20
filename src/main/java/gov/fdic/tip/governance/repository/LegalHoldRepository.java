package gov.fdic.tip.governance.repository;

import gov.fdic.tip.governance.entity.LegalHold;
import gov.fdic.tip.governance.enums.HoldStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LegalHoldRepository extends JpaRepository<LegalHold, UUID> {
    Optional<LegalHold> findByName(String name);
    List<LegalHold> findByStatus(HoldStatus status);
}
