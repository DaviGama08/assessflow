package com.davigama.distributedquiz.assessment.application;

import com.davigama.distributedquiz.assessment.api.dto.AssessmentResponse;
import com.davigama.distributedquiz.assessment.api.dto.CreateAssessmentRequest;
import com.davigama.distributedquiz.assessment.api.dto.UpdateAssessmentRequest;
import com.davigama.distributedquiz.assessment.domain.Assessment;
import com.davigama.distributedquiz.assessment.infrastructure.AssessmentRepository;
import com.davigama.distributedquiz.shared.exception.AssessmentNotFoundException;
import java.time.Clock;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssessmentService {
    private final AssessmentRepository repository;
    private final Clock clock;

    public AssessmentService(AssessmentRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public AssessmentResponse create(CreateAssessmentRequest request) {
        return AssessmentResponse.from(repository.save(new Assessment(request.title(), request.description(), clock.instant())));
    }

    @Transactional(readOnly = true)
    public Page<AssessmentResponse> list(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
        return repository.findAll(pageable).map(AssessmentResponse::from);
    }

    @Transactional(readOnly = true)
    public AssessmentResponse get(UUID id) {
        return AssessmentResponse.from(find(id));
    }

    @Transactional
    public AssessmentResponse update(UUID id, UpdateAssessmentRequest request) {
        Assessment assessment = find(id);
        assessment.update(request.title(), request.description(), clock.instant());
        return AssessmentResponse.from(assessment);
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(find(id));
    }

    private Assessment find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new AssessmentNotFoundException(id));
    }
}
