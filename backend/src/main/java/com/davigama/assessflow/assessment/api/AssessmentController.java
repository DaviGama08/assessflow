package com.davigama.assessflow.assessment.api;

import com.davigama.assessflow.assessment.api.dto.AssessmentPageResponse;
import com.davigama.assessflow.assessment.api.dto.AssessmentResponse;
import com.davigama.assessflow.assessment.api.dto.CreateAssessmentRequest;
import com.davigama.assessflow.assessment.api.dto.UpdateAssessmentRequest;
import com.davigama.assessflow.assessment.application.AssessmentService;
import com.davigama.assessflow.identity.domain.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@Validated
@RequestMapping("/api/v1/organizations/{organizationId}/assessments")
public class AssessmentController {
    private final AssessmentService service;

    public AssessmentController(AssessmentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AssessmentResponse> create(@PathVariable UUID organizationId,
                                                     @Valid @RequestBody CreateAssessmentRequest request,
                                                     Authentication authentication) {
        AssessmentResponse response = service.create(current(authentication), organizationId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public AssessmentPageResponse list(@PathVariable UUID organizationId,
                                       @RequestParam(defaultValue = "0") @Min(0) int page,
                                       @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                       Authentication authentication) {
        return AssessmentPageResponse.from(service.list(current(authentication), organizationId, page, size));
    }

    @GetMapping("/{assessmentId}")
    public AssessmentResponse get(@PathVariable UUID organizationId, @PathVariable UUID assessmentId,
                                  Authentication authentication) {
        return service.get(current(authentication), organizationId, assessmentId);
    }

    @PutMapping("/{assessmentId}")
    public AssessmentResponse update(@PathVariable UUID organizationId, @PathVariable UUID assessmentId,
                                     @Valid @RequestBody UpdateAssessmentRequest request,
                                     Authentication authentication) {
        return service.update(current(authentication), organizationId, assessmentId, request);
    }

    @DeleteMapping("/{assessmentId}")
    public ResponseEntity<Void> delete(@PathVariable UUID organizationId, @PathVariable UUID assessmentId,
                                       Authentication authentication) {
        service.delete(current(authentication), organizationId, assessmentId);
        return ResponseEntity.noContent().build();
    }

    private User current(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }
}
