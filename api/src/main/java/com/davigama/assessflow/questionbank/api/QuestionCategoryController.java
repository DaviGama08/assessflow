package com.davigama.assessflow.questionbank.api;

import com.davigama.assessflow.identity.domain.User;
import com.davigama.assessflow.questionbank.api.dto.QuestionDtos.CategoryResponse;
import com.davigama.assessflow.questionbank.api.dto.QuestionDtos.CreateCategoryRequest;
import com.davigama.assessflow.questionbank.api.dto.QuestionDtos.UpdateCategoryRequest;
import com.davigama.assessflow.questionbank.application.QuestionCategoryService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/question-categories")
public class QuestionCategoryController {
    private final QuestionCategoryService service;

    public QuestionCategoryController(QuestionCategoryService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@PathVariable UUID organizationId,
                                                   @Valid @RequestBody CreateCategoryRequest request,
                                                   Authentication authentication) {
        CategoryResponse response = service.create(current(authentication), organizationId, request.name(),
                request.slug());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public List<CategoryResponse> list(@PathVariable UUID organizationId, Authentication authentication) {
        return service.list(current(authentication), organizationId);
    }

    @GetMapping("/{categoryId}")
    public CategoryResponse get(@PathVariable UUID organizationId, @PathVariable UUID categoryId,
                                Authentication authentication) {
        return service.get(current(authentication), organizationId, categoryId);
    }

    @PutMapping("/{categoryId}")
    public CategoryResponse update(@PathVariable UUID organizationId, @PathVariable UUID categoryId,
                                   @Valid @RequestBody UpdateCategoryRequest request,
                                   Authentication authentication) {
        return service.update(current(authentication), organizationId, categoryId, request.name(), request.slug());
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> delete(@PathVariable UUID organizationId, @PathVariable UUID categoryId,
                                       Authentication authentication) {
        service.delete(current(authentication), organizationId, categoryId);
        return ResponseEntity.noContent().build();
    }

    private User current(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }
}
