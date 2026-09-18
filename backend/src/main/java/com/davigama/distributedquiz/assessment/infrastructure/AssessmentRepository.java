package com.davigama.distributedquiz.assessment.infrastructure;

import com.davigama.distributedquiz.assessment.domain.Assessment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentRepository extends JpaRepository<Assessment, UUID> {}
