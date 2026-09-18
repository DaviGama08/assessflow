package com.davigama.assessflow.assessment.infrastructure;

import com.davigama.assessflow.assessment.domain.Assessment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentRepository extends JpaRepository<Assessment, UUID> {}
