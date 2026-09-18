package com.davigama.assessflow.assessment.api;

import com.davigama.assessflow.assessment.api.dto.AssessmentQuestionDtos.AddAssessmentQuestionRequest;
import com.davigama.assessflow.assessment.api.dto.AssessmentQuestionDtos.AssessmentQuestionResponse;
import com.davigama.assessflow.assessment.api.dto.AssessmentQuestionDtos.ReorderAssessmentQuestionsRequest;
import com.davigama.assessflow.assessment.api.dto.AssessmentQuestionDtos.UpdateAssessmentQuestionRequest;
import com.davigama.assessflow.assessment.application.AssessmentQuestionService;
import com.davigama.assessflow.identity.domain.User;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/assessments/{assessmentId}/questions")
public class AssessmentQuestionController {
    private final AssessmentQuestionService service;

    public AssessmentQuestionController(AssessmentQuestionService service) {
        this.service = service;
    }

    @PostMapping("/{questionId}")
    public ResponseEntity<AssessmentQuestionResponse> add(@PathVariable UUID organizationId,
                                                          @PathVariable UUID assessmentId,
                                                          @PathVariable UUID questionId,
                                                          @Valid @RequestBody AddAssessmentQuestionRequest request,
                                                          Authentication authentication) {
        return ResponseEntity.status(201).body(
                service.add(current(authentication), organizationId, assessmentId, questionId, request));
    }

    @GetMapping
    public List<AssessmentQuestionResponse> list(@PathVariable UUID organizationId,
                                                 @PathVariable UUID assessmentId,
                                                 Authentication authentication) {
        return service.list(current(authentication), organizationId, assessmentId);
    }

    @PatchMapping("/{questionId}")
    public AssessmentQuestionResponse updatePoints(@PathVariable UUID organizationId,
                                                   @PathVariable UUID assessmentId,
                                                   @PathVariable UUID questionId,
                                                   @Valid @RequestBody UpdateAssessmentQuestionRequest request,
                                                   Authentication authentication) {
        return service.updatePoints(current(authentication), organizationId, assessmentId, questionId,
                request.points());
    }

    @PutMapping("/order")
    public List<AssessmentQuestionResponse> reorder(@PathVariable UUID organizationId,
                                                    @PathVariable UUID assessmentId,
                                                    @Valid @RequestBody ReorderAssessmentQuestionsRequest request,
                                                    Authentication authentication) {
        return service.reorder(current(authentication), organizationId, assessmentId, request.questionIds());
    }

    @DeleteMapping("/{questionId}")
    public ResponseEntity<Void> remove(@PathVariable UUID organizationId,
                                       @PathVariable UUID assessmentId,
                                       @PathVariable UUID questionId,
                                       Authentication authentication) {
        service.remove(current(authentication), organizationId, assessmentId, questionId);
        return ResponseEntity.noContent().build();
    }

    private User current(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }
}
