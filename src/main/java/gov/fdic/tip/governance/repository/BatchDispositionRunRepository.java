package gov.fdic.tip.governance.repository;

import gov.fdic.tip.governance.entity.BatchDispositionRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface BatchDispositionRunRepository extends JpaRepository<BatchDispositionRun, UUID> {}
