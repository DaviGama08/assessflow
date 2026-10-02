package com.davigama.assessflow.assessment.api.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record AssessmentPageResponse(List<AssessmentResponse> content, int number, int totalPages,
                                     long totalElements, boolean first, boolean last) {
    public static AssessmentPageResponse from(Page<AssessmentResponse> page) {
        return new AssessmentPageResponse(page.getContent(), page.getNumber(), page.getTotalPages(),
                page.getTotalElements(), page.isFirst(), page.isLast());
    }
}
