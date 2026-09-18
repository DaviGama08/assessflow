package com.davigama.assessflow.questionbank.application;

import com.davigama.assessflow.identity.domain.User;
import com.davigama.assessflow.organization.application.OrganizationAccess;
import com.davigama.assessflow.organization.application.OrganizationException;
import com.davigama.assessflow.organization.infrastructure.OrganizationRepository;
import com.davigama.assessflow.questionbank.api.dto.QuestionDtos.CategoryResponse;
import com.davigama.assessflow.questionbank.domain.QuestionCategory;
import com.davigama.assessflow.questionbank.infrastructure.QuestionCategoryRepository;
import com.davigama.assessflow.questionbank.infrastructure.QuestionRepository;
import com.davigama.assessflow.shared.Slug;
import com.davigama.assessflow.shared.exception.DomainException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionCategoryService {
    private final QuestionCategoryRepository categories;
    private final QuestionRepository questions;
    private final OrganizationRepository organizations;
    private final OrganizationAccess access;
    private final Clock clock;

    public QuestionCategoryService(QuestionCategoryRepository categories, QuestionRepository questions,
                                   OrganizationRepository organizations, OrganizationAccess access, Clock clock) {
        this.categories = categories;
        this.questions = questions;
        this.organizations = organizations;
        this.access = access;
        this.clock = clock;
    }

    @Transactional
    public CategoryResponse create(User actor, UUID organizationId, String name, String slug) {
        requireOrganization(organizationId);
        access.requireInstructor(organizationId, actor.getId());
        String normalized = Slug.normalize(slug);
        if (categories.existsByOrganizationIdAndSlug(organizationId, normalized)) throw duplicate();
        QuestionCategory category = new QuestionCategory(organizationId, name, normalized, clock.instant());
        try {
            return CategoryResponse.from(categories.saveAndFlush(category));
        } catch (DataIntegrityViolationException ex) {
            throw duplicate();
        }
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(User actor, UUID organizationId) {
        requireOrganization(organizationId);
        access.requireInstructor(organizationId, actor.getId());
        return categories.findByOrganizationIdOrderByNameAsc(organizationId).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(User actor, UUID organizationId, UUID categoryId) {
        requireOrganization(organizationId);
        access.requireInstructor(organizationId, actor.getId());
        return CategoryResponse.from(find(organizationId, categoryId));
    }

    @Transactional
    public CategoryResponse update(User actor, UUID organizationId, UUID categoryId, String name, String slug) {
        requireOrganization(organizationId);
        access.requireInstructor(organizationId, actor.getId());
        QuestionCategory category = find(organizationId, categoryId);
        String normalized = Slug.normalize(slug);
        if (categories.existsByOrganizationIdAndSlugAndIdNot(organizationId, normalized, categoryId)) throw duplicate();
        category.rename(name, normalized);
        try {
            categories.flush();
        } catch (DataIntegrityViolationException ex) {
            throw duplicate();
        }
        return CategoryResponse.from(category);
    }

    @Transactional
    public void delete(User actor, UUID organizationId, UUID categoryId) {
        requireOrganization(organizationId);
        access.requireInstructor(organizationId, actor.getId());
        QuestionCategory category = find(organizationId, categoryId);
        if (questions.countByCategoryIdAndOrganizationId(categoryId, organizationId) > 0) {
            throw new DomainException(HttpStatus.CONFLICT, "CATEGORY_IN_USE",
                    "Category cannot be deleted while questions still use it.");
        }
        categories.delete(category);
    }

    QuestionCategory requireOwned(UUID organizationId, UUID categoryId) {
        return find(organizationId, categoryId);
    }

    private QuestionCategory find(UUID organizationId, UUID categoryId) {
        return categories.findByIdAndOrganizationId(categoryId, organizationId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND",
                        "Category not found."));
    }

    private void requireOrganization(UUID organizationId) {
        if (!organizations.existsById(organizationId)) {
            throw new OrganizationException(HttpStatus.NOT_FOUND, "ORGANIZATION_NOT_FOUND",
                    "Organization not found.");
        }
    }

    private DomainException duplicate() {
        return new DomainException(HttpStatus.CONFLICT, "CATEGORY_SLUG_EXISTS",
                "Category slug is already in use in this organization.");
    }
}
