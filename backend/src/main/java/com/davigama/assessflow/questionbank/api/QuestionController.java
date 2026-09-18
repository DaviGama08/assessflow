package com.davigama.assessflow.questionbank.api;

import com.davigama.assessflow.identity.domain.User;
import com.davigama.assessflow.questionbank.api.dto.QuestionDtos.CreateQuestionRequest;
import com.davigama.assessflow.questionbank.api.dto.QuestionDtos.QuestionPageResponse;
import com.davigama.assessflow.questionbank.api.dto.QuestionDtos.QuestionResponse;
import com.davigama.assessflow.questionbank.api.dto.QuestionDtos.UpdateQuestionRequest;
import com.davigama.assessflow.questionbank.application.QuestionService;
import com.davigama.assessflow.questionbank.domain.QuestionDifficulty;
import com.davigama.assessflow.questionbank.domain.QuestionStatus;
import com.davigama.assessflow.questionbank.domain.QuestionType;
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
@RequestMapping("/api/v1/organizations/{organizationId}/questions")
public class QuestionController {
    private final QuestionService service;

    public QuestionController(QuestionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<QuestionResponse> create(@PathVariable UUID organizationId,
                                                   @Valid @RequestBody CreateQuestionRequest request,
                                                   Authentication authentication) {
        QuestionResponse response = service.create(current(authentication), organizationId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public QuestionPageResponse list(@PathVariable UUID organizationId,
                                     @RequestParam(defaultValue = "0") @Min(0) int page,
                                     @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                     @RequestParam(required = false) String search,
                                     @RequestParam(required = false) QuestionType type,
                                     @RequestParam(required = false) QuestionDifficulty difficulty,
                                     @RequestParam(required = false) QuestionStatus status,
                                     @RequestParam(required = false) UUID category,
                                     Authentication authentication) {
        return QuestionPageResponse.from(service.list(current(authentication), organizationId, page, size, search,
                type, difficulty, status, category));
    }

    @GetMapping("/{questionId}")
    public QuestionResponse get(@PathVariable UUID organizationId, @PathVariable UUID questionId,
                                Authentication authentication) {
        return service.get(current(authentication), organizationId, questionId);
    }

    @PutMapping("/{questionId}")
    public QuestionResponse update(@PathVariable UUID organizationId, @PathVariable UUID questionId,
                                   @Valid @RequestBody UpdateQuestionRequest request,
                                   Authentication authentication) {
        return service.update(current(authentication), organizationId, questionId, request);
    }

    @DeleteMapping("/{questionId}")
    public ResponseEntity<Void> delete(@PathVariable UUID organizationId, @PathVariable UUID questionId,
                                       Authentication authentication) {
        service.delete(current(authentication), organizationId, questionId);
        return ResponseEntity.noContent().build();
    }

    private User current(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }
}
