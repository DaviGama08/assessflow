package com.davigama.assessflow.questionbank.infrastructure;

import com.davigama.assessflow.questionbank.domain.QuestionCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionCategoryRepository extends JpaRepository<QuestionCategory, UUID> {
    Optional<QuestionCategory> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<QuestionCategory> findByOrganizationIdOrderByNameAsc(UUID organizationId);

    boolean existsByOrganizationIdAndSlug(UUID organizationId, String slug);

    boolean existsByOrganizationIdAndSlugAndIdNot(UUID organizationId, String slug, UUID id);
}
