package com.davigama.assessflow.livesession.domain;

import com.davigama.assessflow.questionbank.domain.QuestionType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "live_session_questions")
public class LiveSessionQuestion {
    @Id
    private UUID id;
    @Column(name = "live_session_id", nullable = false)
    private UUID liveSessionId;
    @Column(name = "source_question_id", nullable = false)
    private UUID sourceQuestionId;
    @Column(name = "question_text", nullable = false, length = 4000)
    private String questionText;
    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 30)
    private QuestionType questionType;
    @Column(name = "display_order", nullable = false)
    private int displayOrder;
    @Column(nullable = false)
    private int points;
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    private List<LiveSessionQuestionOption> options = new ArrayList<>();

    protected LiveSessionQuestion() {}

    public LiveSessionQuestion(UUID liveSessionId, UUID sourceQuestionId, String questionText, QuestionType type,
                               int displayOrder, int points) {
        this.id = UUID.randomUUID();
        this.liveSessionId = liveSessionId;
        this.sourceQuestionId = sourceQuestionId;
        this.questionText = questionText;
        this.questionType = type;
        this.displayOrder = displayOrder;
        this.points = points;
    }

    public void addOption(LiveSessionQuestionOption option) {
        options.add(option);
    }

    public UUID getId() { return id; }
    public UUID getLiveSessionId() { return liveSessionId; }
    public UUID getSourceQuestionId() { return sourceQuestionId; }
    public String getQuestionText() { return questionText; }
    public QuestionType getQuestionType() { return questionType; }
    public int getDisplayOrder() { return displayOrder; }
    public int getPoints() { return points; }
    public List<LiveSessionQuestionOption> getOptions() { return options; }
}
