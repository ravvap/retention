package gov.fdic.tip.governance.repository;

import gov.fdic.tip.governance.entity.GovernedItemEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface GovernedItemEventRepository extends JpaRepository<GovernedItemEvent, UUID> {
    List<GovernedItemEvent> findByGovernedItemIdOrderByEventDateDesc(UUID itemId);
}
