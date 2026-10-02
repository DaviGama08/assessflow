package com.davigama.assessflow.assessment.infrastructure;

import com.davigama.assessflow.assessment.domain.AssessmentQuestion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentQuestionRepository extends JpaRepository<AssessmentQuestion, UUID> {
    Optional<AssessmentQuestion> findByAssessmentIdAndQuestionId(UUID assessmentId, UUID questionId);

    List<AssessmentQuestion> findByAssessmentIdOrderByDisplayOrderAscIdAsc(UUID assessmentId);

    boolean existsByAssessmentIdAndQuestionId(UUID assessmentId, UUID questionId);

    long countByAssessmentId(UUID assessmentId);
}
