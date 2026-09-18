package com.davigama.assessflow.assessment.infrastructure;

import com.davigama.assessflow.assessment.domain.Assessment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentRepository extends JpaRepository<Assessment, UUID> {
    Optional<Assessment> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Page<Assessment> findByOrganizationId(UUID organizationId, Pageable pageable);

    long countByOrganizationId(UUID organizationId);
}
