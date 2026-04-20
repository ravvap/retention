package gov.fdic.tip.governance.repository;

import gov.fdic.tip.governance.entity.RetentionPolicy;
import gov.fdic.tip.governance.enums.PolicyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RetentionPolicyRepository extends JpaRepository<RetentionPolicy, UUID> {
    Optional<RetentionPolicy> findByName(String name);
    List<RetentionPolicy> findByStatus(PolicyStatus status);
    boolean existsByNameAndStatusNot(String name, PolicyStatus status);
}
