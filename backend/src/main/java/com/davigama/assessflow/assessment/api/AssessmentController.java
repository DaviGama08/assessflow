package com.davigama.assessflow.assessment.api;

import com.davigama.assessflow.assessment.api.dto.AssessmentResponse;
import com.davigama.assessflow.assessment.api.dto.AssessmentPageResponse;
import com.davigama.assessflow.assessment.api.dto.CreateAssessmentRequest;
import com.davigama.assessflow.assessment.api.dto.UpdateAssessmentRequest;
import com.davigama.assessflow.assessment.application.AssessmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1/assessments")
public class AssessmentController {
    private final AssessmentService service;

    public AssessmentController(AssessmentService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<AssessmentResponse> create(@Valid @RequestBody CreateAssessmentRequest request) {
        AssessmentResponse response = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public AssessmentPageResponse list(@RequestParam(defaultValue = "0") @Min(0) int page,
                                         @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return AssessmentPageResponse.from(service.list(page, size));
    }

    @GetMapping("/{id}")
    public AssessmentResponse get(@PathVariable UUID id) { return service.get(id); }

    @PutMapping("/{id}")
    public AssessmentResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateAssessmentRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
