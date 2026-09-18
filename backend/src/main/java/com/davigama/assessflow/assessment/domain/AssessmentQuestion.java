package com.davigama.assessflow.assessment.domain;

import com.davigama.assessflow.shared.exception.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "assessment_questions")
public class AssessmentQuestion {
    @Id
    private UUID id;

    @Column(name = "assessment_id", nullable = false)
    private UUID assessmentId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(nullable = false)
    private int points;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected AssessmentQuestion() {}

    public AssessmentQuestion(UUID assessmentId, UUID questionId, int points, int displayOrder) {
        this.id = UUID.randomUUID();
        this.assessmentId = assessmentId;
        this.questionId = questionId;
        setPoints(points);
        this.displayOrder = displayOrder;
    }

    public void setPoints(int points) {
        if (points <= 0) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_POINTS",
                    "Points must be greater than 0.");
        }
        this.points = points;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public UUID getId() { return id; }
    public UUID getAssessmentId() { return assessmentId; }
    public UUID getQuestionId() { return questionId; }
    public int getPoints() { return points; }
    public int getDisplayOrder() { return displayOrder; }
}
