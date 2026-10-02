package com.davigama.assessflow.locallive;

import static org.assertj.core.api.Assertions.assertThat;

import com.davigama.assessflow.locallive.api.dto.AssessmentPackageV1;
import com.davigama.assessflow.locallive.api.dto.AssessmentPackageV1.PackageAssessment;
import com.davigama.assessflow.locallive.api.dto.AssessmentPackageV1.PackageOption;
import com.davigama.assessflow.locallive.api.dto.AssessmentPackageV1.PackageQuestion;
import com.davigama.assessflow.locallive.application.PackageChecksum;
import com.davigama.assessflow.questionbank.domain.QuestionDifficulty;
import com.davigama.assessflow.questionbank.domain.QuestionType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PackageChecksumTest {
    private static final UUID ORG = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ASSESSMENT = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID QUESTION = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final Instant EXPORTED_AT = Instant.parse("2026-03-01T12:00:00Z");

    @Test
    void hashesCanonicalJsonWithoutChecksumField() {
        String json = PackageChecksum.canonicalJson(sample().content());
        assertThat(json).doesNotContain("checksumSha256");
        assertThat(json).doesNotContain("\n");
        assertThat(PackageChecksum.matches(sample())).isTrue();
        assertThat(PackageChecksum.sha256(sample().content())).isEqualTo(sample().checksumSha256());
    }

    @Test
    void changesWhenSettingsQuestionsOrOptionsChange() {
        String baseline = PackageChecksum.sha256(sample().content());
        assertThat(PackageChecksum.sha256(withAssessment(sample(),
                new PackageAssessment(ASSESSMENT, "Title", "Desc", 45, 3, 10, true, true, false)).content()))
                .isNotEqualTo(baseline);
        assertThat(PackageChecksum.sha256(withAssessment(sample(),
                new PackageAssessment(ASSESSMENT, "Title", "Desc", 45, 2, 70, false, true, false)).content()))
                .isNotEqualTo(baseline);
        assertThat(PackageChecksum.sha256(withOrg(sample(), UUID.fromString("44444444-4444-4444-4444-444444444444"))
                .content())).isNotEqualTo(baseline);
        PackageQuestion alteredQuestion = new PackageQuestion(QUESTION, "Q1", QuestionType.SINGLE_CHOICE,
                QuestionDifficulty.HARD, "Because", "Core", 2, 0, sample().questions().getFirst().options());
        assertThat(PackageChecksum.sha256(withQuestions(sample(), List.of(alteredQuestion)).content()))
                .isNotEqualTo(baseline);
        PackageQuestion alteredOption = new PackageQuestion(QUESTION, "Q1", QuestionType.SINGLE_CHOICE,
                QuestionDifficulty.MEDIUM, "Because", "Core", 2, 0,
                List.of(new PackageOption("Yes", false, 0), new PackageOption("No", true, 1)));
        assertThat(PackageChecksum.sha256(withQuestions(sample(), List.of(alteredOption)).content()))
                .isNotEqualTo(baseline);
    }

    private AssessmentPackageV1 sample() {
        PackageQuestion question = new PackageQuestion(QUESTION, "Q1", QuestionType.SINGLE_CHOICE,
                QuestionDifficulty.MEDIUM, "Because", "Core", 2, 0,
                List.of(new PackageOption("Yes", true, 0), new PackageOption("No", false, 1)));
        AssessmentPackageV1 unsigned = new AssessmentPackageV1(1, EXPORTED_AT, ORG, "Org",
                new PackageAssessment(ASSESSMENT, "Title", "Desc", 45, 2, 70, true, true, false),
                List.of(question), "pending");
        return new AssessmentPackageV1(unsigned.schemaVersion(), unsigned.exportedAt(), unsigned.sourceOrganizationId(),
                unsigned.organizationName(), unsigned.assessment(), unsigned.questions(),
                PackageChecksum.sha256(unsigned.content()));
    }

    private AssessmentPackageV1 withAssessment(AssessmentPackageV1 pack, PackageAssessment assessment) {
        return new AssessmentPackageV1(pack.schemaVersion(), pack.exportedAt(), pack.sourceOrganizationId(),
                pack.organizationName(), assessment, pack.questions(), pack.checksumSha256());
    }

    private AssessmentPackageV1 withOrg(AssessmentPackageV1 pack, UUID organizationId) {
        return new AssessmentPackageV1(pack.schemaVersion(), pack.exportedAt(), organizationId,
                pack.organizationName(), pack.assessment(), pack.questions(), pack.checksumSha256());
    }

    private AssessmentPackageV1 withQuestions(AssessmentPackageV1 pack, List<PackageQuestion> questions) {
        return new AssessmentPackageV1(pack.schemaVersion(), pack.exportedAt(), pack.sourceOrganizationId(),
                pack.organizationName(), pack.assessment(), questions, pack.checksumSha256());
    }
}
