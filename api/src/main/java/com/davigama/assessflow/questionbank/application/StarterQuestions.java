package com.davigama.assessflow.questionbank.application;

import com.davigama.assessflow.identity.domain.User;
import com.davigama.assessflow.organization.application.OrganizationAccess;
import com.davigama.assessflow.questionbank.api.dto.QuestionDtos.CreateQuestionRequest;
import com.davigama.assessflow.questionbank.api.dto.QuestionDtos.OptionInput;
import com.davigama.assessflow.questionbank.domain.QuestionDifficulty;
import com.davigama.assessflow.questionbank.domain.QuestionStatus;
import com.davigama.assessflow.questionbank.domain.QuestionType;
import com.davigama.assessflow.shared.Slug;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
public class StarterQuestions {
    private record Sample(String category, String text, QuestionType type, QuestionDifficulty difficulty,
                          String explanation, List<OptionInput> options) {}

    private final JdbcTemplate jdbc;
    private final OrganizationAccess access;
    private final QuestionCategoryService categories;
    private final QuestionService questions;
    private final Sample[] samples;

    public StarterQuestions(JdbcTemplate jdbc, OrganizationAccess access, QuestionCategoryService categories,
                            QuestionService questions, JsonMapper mapper) throws IOException {
        this.jdbc = jdbc;
        this.access = access;
        this.categories = categories;
        this.questions = questions;
        try (var input = new ClassPathResource("questions/informatica.json").getInputStream()) {
            samples = mapper.readValue(input, Sample[].class);
        }
    }

    @Transactional
    public int install(User actor, UUID organizationId) {
        access.requireInstructor(organizationId, actor.getId());
        int claimed = jdbc.update("""
                INSERT INTO question_collection_imports (organization_id, collection)
                VALUES (?, 'informatica-v1') ON CONFLICT DO NOTHING
                """, organizationId);
        if (claimed == 0) return 0;

        var categoryIds = new HashMap<String, UUID>();
        categories.list(actor, organizationId).forEach(category -> categoryIds.put(category.slug(), category.id()));
        for (var sample : samples) {
            var categoryId = categoryIds.computeIfAbsent(Slug.normalize(sample.category()), slug ->
                    categories.create(actor, organizationId, sample.category(), slug).id());
            questions.create(actor, organizationId, new CreateQuestionRequest(sample.text(), sample.type(),
                    sample.difficulty(), categoryId, sample.explanation(), QuestionStatus.ACTIVE, sample.options()));
        }
        return samples.length;
    }
}
