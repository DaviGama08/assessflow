package com.davigama.assessflow.questionbank.infrastructure;

import com.davigama.assessflow.questionbank.domain.Question;
import com.davigama.assessflow.questionbank.domain.QuestionDifficulty;
import com.davigama.assessflow.questionbank.domain.QuestionStatus;
import com.davigama.assessflow.questionbank.domain.QuestionType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, UUID> {
    @EntityGraph(attributePaths = {"category", "options"})
    @Query("select q from Question q where q.id = :id and q.organizationId = :organizationId")
    Optional<Question> findDetailedByIdAndOrganizationId(@Param("id") UUID id,
                                                         @Param("organizationId") UUID organizationId);

    Optional<Question> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @EntityGraph(attributePaths = "category")
    @Query("""
            select q from Question q
            where q.organizationId = :organizationId
              and (:type is null or q.type = :type)
              and (:difficulty is null or q.difficulty = :difficulty)
              and (:status is null or q.status = :status)
              and (:categoryId is null or q.category.id = :categoryId)
              and (:search is null or lower(q.text) like :search)
            """)
    Page<Question> search(@Param("organizationId") UUID organizationId,
                          @Param("type") QuestionType type,
                          @Param("difficulty") QuestionDifficulty difficulty,
                          @Param("status") QuestionStatus status,
                          @Param("categoryId") UUID categoryId,
                          @Param("search") String search,
                          Pageable pageable);

    @EntityGraph(attributePaths = "category")
    List<Question> findByIdInAndOrganizationId(Collection<UUID> ids, UUID organizationId);

    @EntityGraph(attributePaths = "options")
    @Query("select q from Question q where q.id in :ids and q.organizationId = :organizationId")
    List<Question> findWithOptionsByIdInAndOrganizationId(@Param("ids") Collection<UUID> ids,
                                                          @Param("organizationId") UUID organizationId);

    long countByOrganizationId(UUID organizationId);

    long countByCategoryIdAndOrganizationId(UUID categoryId, UUID organizationId);
}
