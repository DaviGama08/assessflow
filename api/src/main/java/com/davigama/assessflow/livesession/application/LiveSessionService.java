package com.davigama.assessflow.livesession.application;

import com.davigama.assessflow.assessment.application.AssessmentService;
import com.davigama.assessflow.assessment.domain.Assessment;
import com.davigama.assessflow.assessment.domain.AssessmentQuestion;
import com.davigama.assessflow.assessment.domain.AssessmentStatus;
import com.davigama.assessflow.assessment.infrastructure.AssessmentQuestionRepository;
import com.davigama.assessflow.identity.domain.User;
import com.davigama.assessflow.livesession.api.dto.LiveDtos;
import com.davigama.assessflow.livesession.api.dto.LiveDtos.AnswerRequest;
import com.davigama.assessflow.livesession.api.dto.LiveDtos.JoinRequest;
import com.davigama.assessflow.livesession.api.dto.LiveDtos.JoinResponse;
import com.davigama.assessflow.livesession.api.dto.LiveDtos.OptionResult;
import com.davigama.assessflow.livesession.api.dto.LiveDtos.ParticipantResponse;
import com.davigama.assessflow.livesession.api.dto.LiveDtos.ParticipantState;
import com.davigama.assessflow.livesession.api.dto.LiveDtos.PreviewResponse;
import com.davigama.assessflow.livesession.api.dto.LiveDtos.PublicOption;
import com.davigama.assessflow.livesession.api.dto.LiveDtos.PublicQuestion;
import com.davigama.assessflow.livesession.api.dto.LiveDtos.QuestionResults;
import com.davigama.assessflow.livesession.api.dto.LiveDtos.Score;
import com.davigama.assessflow.livesession.api.dto.LiveDtos.SessionResponse;
import com.davigama.assessflow.livesession.domain.AnswerSelections;
import com.davigama.assessflow.livesession.domain.JoinCodes;
import com.davigama.assessflow.livesession.domain.LiveAnswer;
import com.davigama.assessflow.livesession.domain.LiveEvent;
import com.davigama.assessflow.livesession.domain.LiveEventType;
import com.davigama.assessflow.livesession.domain.LiveParticipant;
import com.davigama.assessflow.livesession.domain.LiveParticipantStatus;
import com.davigama.assessflow.livesession.domain.LiveSession;
import com.davigama.assessflow.livesession.domain.LiveSessionQuestion;
import com.davigama.assessflow.livesession.domain.LiveSessionQuestionOption;
import com.davigama.assessflow.livesession.domain.LiveSessionStatus;
import com.davigama.assessflow.livesession.infrastructure.LiveAnswerRepository;
import com.davigama.assessflow.livesession.infrastructure.LiveParticipantRepository;
import com.davigama.assessflow.livesession.infrastructure.LiveSessionQuestionRepository;
import com.davigama.assessflow.livesession.infrastructure.LiveSessionRepository;
import com.davigama.assessflow.livesession.realtime.LiveSessionNotifier;
import com.davigama.assessflow.organization.application.OrganizationAccess;
import com.davigama.assessflow.shared.observability.AssessFlowMetrics;
import com.davigama.assessflow.organization.application.OrganizationException;
import com.davigama.assessflow.organization.infrastructure.OrganizationRepository;
import com.davigama.assessflow.questionbank.domain.AnswerOption;
import com.davigama.assessflow.questionbank.domain.Question;
import com.davigama.assessflow.questionbank.infrastructure.QuestionRepository;
import com.davigama.assessflow.shared.exception.DomainException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LiveSessionService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final LiveSessionRepository sessions;
    private final LiveSessionQuestionRepository snapshots;
    private final LiveParticipantRepository participants;
    private final LiveAnswerRepository answers;
    private final AssessmentService assessments;
    private final AssessmentQuestionRepository assessmentQuestions;
    private final QuestionRepository questions;
    private final OrganizationRepository organizations;
    private final OrganizationAccess access;
    private final LiveSessionNotifier notifier;
    private final ParticipantAuthService participantAuth;
    private final LiveSettings settings;
    private final AssessFlowMetrics metrics;
    private final Clock clock;

    public LiveSessionService(LiveSessionRepository sessions, LiveSessionQuestionRepository snapshots,
                              LiveParticipantRepository participants, LiveAnswerRepository answers,
                              AssessmentService assessments, AssessmentQuestionRepository assessmentQuestions,
                              QuestionRepository questions, OrganizationRepository organizations,
                              OrganizationAccess access, LiveSessionNotifier notifier,
                              ParticipantAuthService participantAuth, LiveSettings settings,
                              AssessFlowMetrics metrics, Clock clock) {
        this.sessions = sessions;
        this.snapshots = snapshots;
        this.participants = participants;
        this.answers = answers;
        this.assessments = assessments;
        this.assessmentQuestions = assessmentQuestions;
        this.questions = questions;
        this.organizations = organizations;
        this.access = access;
        this.notifier = notifier;
        this.participantAuth = participantAuth;
        this.settings = settings;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public SessionResponse create(User actor, UUID organizationId, UUID assessmentId) {
        requireOrganization(organizationId);
        access.requireInstructor(organizationId, actor.getId());
        Assessment assessment = assessments.requireOwned(organizationId, assessmentId);
        if (assessment.getStatus() != AssessmentStatus.PUBLISHED) {
            throw new DomainException(HttpStatus.CONFLICT, "ASSESSMENT_NOT_PUBLISHED",
                    "Live sessions can only be created from a published assessment.");
        }
        List<AssessmentQuestion> links = assessmentQuestions
                .findByAssessmentIdOrderByDisplayOrderAscIdAsc(assessment.getId());
        if (links.isEmpty()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "ASSESSMENT_HAS_NO_QUESTIONS",
                    "An assessment needs at least one question before a live session can start.");
        }
        LiveSession session = persistWithJoinCode(organizationId, assessment.getId(), actor.getId());
        snapshot(session, links, organizationId, assessment.isShuffleQuestions(), assessment.isShuffleAnswers());
        return SessionResponse.from(session, assessment.getTitle());
    }

    @Transactional(readOnly = true)
    public SessionResponse get(User actor, UUID organizationId, UUID sessionId) {
        requireOrganization(organizationId);
        access.requireInstructor(organizationId, actor.getId());
        LiveSession session = requireOwned(organizationId, sessionId);
        return SessionResponse.from(session, assessments.requireOwned(organizationId, session.getAssessmentId()).getTitle());
    }

    @Transactional(readOnly = true)
    public List<ParticipantResponse> listParticipants(User actor, UUID organizationId, UUID sessionId) {
        requireOrganization(organizationId);
        access.requireInstructor(organizationId, actor.getId());
        requireOwned(organizationId, sessionId);
        return participants.findByLiveSessionIdOrderByJoinedAtAsc(sessionId).stream()
                .map(ParticipantResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PreviewResponse preview(String code) {
        LiveSession session = findByCode(code);
        String title = assessments.requireOwned(session.getOrganizationId(), session.getAssessmentId()).getTitle();
        return new PreviewResponse(title, session.getStatus(), session.joinable());
    }

    @Transactional
    public JoinResponse join(JoinRequest request) {
        LiveSession session = findByCode(request.code());
        session.requireJoinable();
        Instant now = clock.instant();
        String raw = participantAuth.newRawToken();
        LiveParticipant participant = participants.save(new LiveParticipant(session.getId(), request.displayName(),
                ParticipantAuthService.hash(raw), now, now.plus(settings.participantTokenTtl())));
        String title = assessments.requireOwned(session.getOrganizationId(), session.getAssessmentId()).getTitle();
        notifier.toHost(session.getId(), new LiveEvent(LiveEventType.PARTICIPANT_JOINED,
                ParticipantResponse.from(participant)));
        metrics.join();
        return new JoinResponse(participant.getId(), session.getId(), title, session.getStatus(),
                session.getJoinCode(), raw);
    }

    @Transactional(readOnly = true)
    public PublicQuestion hostQuestion(User actor, UUID organizationId, UUID sessionId) {
        LiveSession session = host(actor, organizationId, sessionId);
        if (session.getCurrentQuestionIndex() == null) {
            throw new DomainException(HttpStatus.CONFLICT, "QUESTION_NOT_ACTIVE", "There is no current question.");
        }
        return publicQuestion(session);
    }

    @Transactional(readOnly = true)
    public QuestionResults hostResults(User actor, UUID organizationId, UUID sessionId) {
        LiveSession session = host(actor, organizationId, sessionId);
        if (session.getCurrentQuestionIndex() == null) {
            throw new DomainException(HttpStatus.CONFLICT, "QUESTION_NOT_ACTIVE", "There is no current question.");
        }
        return results(session);
    }

    @Transactional
    public SessionResponse start(User actor, UUID organizationId, UUID sessionId) {
        LiveSession session = host(actor, organizationId, sessionId);
        session.start(clock.instant());
        notifier.toBoth(session.getId(), new LiveEvent(LiveEventType.SESSION_STARTED, Map.of("status", session.getStatus())));
        notifier.toBoth(session.getId(), new LiveEvent(LiveEventType.QUESTION_STARTED, publicQuestion(session)));
        metrics.sessionStarted();
        return response(session);
    }

    @Transactional
    public QuestionResults endQuestion(User actor, UUID organizationId, UUID sessionId) {
        LiveSession session = host(actor, organizationId, sessionId);
        session.endQuestion(clock.instant());
        QuestionResults results = results(session);
        notifier.toHost(session.getId(), new LiveEvent(LiveEventType.QUESTION_ENDED, Map.of("questionIndex", session.getCurrentQuestionIndex())));
        notifier.toBoth(session.getId(), new LiveEvent(LiveEventType.QUESTION_RESULTS, publicResults(results)));
        return results;
    }

    @Transactional
    public SessionResponse nextQuestion(User actor, UUID organizationId, UUID sessionId) {
        LiveSession session = host(actor, organizationId, sessionId);
        List<LiveSessionQuestion> items = snapshots.findByLiveSessionIdOrderByDisplayOrderAsc(session.getId());
        int next = session.getCurrentQuestionIndex() + 1;
        if (next >= items.size()) {
            throw new DomainException(HttpStatus.CONFLICT, "NO_MORE_QUESTIONS", "There are no more questions.");
        }
        session.nextQuestion(next, clock.instant());
        notifier.toBoth(session.getId(), new LiveEvent(LiveEventType.QUESTION_STARTED, publicQuestion(session)));
        return response(session);
    }

    @Transactional
    public SessionResponse finish(User actor, UUID organizationId, UUID sessionId) {
        LiveSession session = host(actor, organizationId, sessionId);
        if (session.isQuestionOpen()) session.endQuestion(clock.instant());
        session.finish(clock.instant());
        notifier.toBoth(session.getId(), new LiveEvent(LiveEventType.SESSION_FINISHED, Map.of("status", session.getStatus())));
        return response(session);
    }

    @Transactional
    public SessionResponse cancel(User actor, UUID organizationId, UUID sessionId) {
        LiveSession session = host(actor, organizationId, sessionId);
        session.cancel(clock.instant());
        notifier.toBoth(session.getId(), new LiveEvent(LiveEventType.SESSION_CANCELLED, Map.of("status", session.getStatus())));
        return response(session);
    }

    @Transactional
    public void answer(ParticipantPrincipal principal, UUID sessionId, AnswerRequest request) {
        if (!principal.sessionId().equals(sessionId)) {
            throw new DomainException(HttpStatus.FORBIDDEN, "PARTICIPANT_NOT_FOUND", "Participant does not belong to this session.");
        }
        LiveSession session = sessions.findById(sessionId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "LIVE_SESSION_NOT_FOUND", "Live session not found."));
        session.requireQuestionOpen();
        LiveSessionQuestion question = currentQuestion(session);
        Set<UUID> allowed = question.getOptions().stream().map(LiveSessionQuestionOption::getId).collect(Collectors.toSet());
        Set<UUID> selected = AnswerSelections.validate(question.getQuestionType(), request.optionIds(), allowed);
        if (answers.existsByLiveSessionIdAndLiveSessionQuestionIdAndParticipantId(
                session.getId(), question.getId(), principal.participantId())) {
            throw new DomainException(HttpStatus.CONFLICT, "ANSWER_ALREADY_SUBMITTED", "This question was already answered.");
        }
        try {
            answers.saveAndFlush(new LiveAnswer(session.getId(), question.getId(), principal.participantId(),
                    selected, clock.instant()));
        } catch (DataIntegrityViolationException ex) {
            throw new DomainException(HttpStatus.CONFLICT, "ANSWER_ALREADY_SUBMITTED", "This question was already answered.");
        }
        long answered = answers.countByLiveSessionIdAndLiveSessionQuestionId(session.getId(), question.getId());
        long total = participants.countByLiveSessionIdAndStatusNot(session.getId(), LiveParticipantStatus.LEFT);
        notifier.toHost(session.getId(), new LiveEvent(LiveEventType.ANSWER_RECEIVED,
                Map.of("answered", answered, "participants", total)));
        metrics.answer();
    }

    @Transactional(readOnly = true)
    public String exportResultsCsv(User actor, UUID organizationId, UUID sessionId) {
        LiveSession session = host(actor, organizationId, sessionId);
        if (session.getStatus() != LiveSessionStatus.FINISHED) {
            throw new DomainException(HttpStatus.CONFLICT, "LIVE_SESSION_NOT_FINISHED",
                    "Final result export is available only after the live session has finished.");
        }
        String title = assessments.requireOwned(organizationId, session.getAssessmentId()).getTitle();
        StringBuilder csv = new StringBuilder("session,assessment,participant,status,pointsEarned,pointsPossible,percentage\n");
        for (LiveParticipant participant : participants.findByLiveSessionIdOrderByJoinedAtAsc(sessionId)) {
            Score score = score(session, participant.getId());
            csv.append(session.getId()).append(',').append(CsvFormulaGuard.escape(title)).append(',')
                    .append(CsvFormulaGuard.escape(participant.getDisplayName())).append(',')
                    .append(participant.getStatus()).append(',')
                    .append(score.pointsEarned()).append(',').append(score.pointsPossible()).append(',')
                    .append(score.percentage()).append('\n');
        }
        return csv.toString();
    }

    @Transactional
    public void leave(ParticipantPrincipal principal, UUID sessionId) {
        if (!principal.sessionId().equals(sessionId)) {
            throw new DomainException(HttpStatus.FORBIDDEN, "PARTICIPANT_NOT_FOUND", "Participant does not belong to this session.");
        }
        LiveParticipant participant = participants.findByIdAndLiveSessionId(principal.participantId(), sessionId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "PARTICIPANT_NOT_FOUND", "Participant not found."));
        participant.leave(clock.instant());
        notifier.toHost(sessionId, new LiveEvent(LiveEventType.PARTICIPANT_LEFT, ParticipantResponse.from(participant)));
    }

    @Transactional
    public ParticipantState state(ParticipantPrincipal principal, UUID sessionId) {
        if (!principal.sessionId().equals(sessionId)) {
            throw new DomainException(HttpStatus.FORBIDDEN, "PARTICIPANT_NOT_FOUND", "Participant does not belong to this session.");
        }
        LiveSession session = sessions.findById(sessionId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "LIVE_SESSION_NOT_FOUND", "Live session not found."));
        LiveParticipant participant = participants.findByIdAndLiveSessionId(principal.participantId(), sessionId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "PARTICIPANT_NOT_FOUND", "Participant not found."));
        participant.connected(clock.instant());
        Assessment assessment = assessments.requireOwned(session.getOrganizationId(), session.getAssessmentId());
        LiveSessionQuestion question = session.getCurrentQuestionIndex() == null ? null : currentQuestion(session);
        boolean answered = question != null && answers.existsByLiveSessionIdAndLiveSessionQuestionIdAndParticipantId(
                session.getId(), question.getId(), participant.getId());
        PublicQuestion publicQuestion = session.isQuestionOpen() ? publicQuestion(session) : null;
        QuestionResults closed = !session.isQuestionOpen() && question != null ? results(session) : null;
        Score score = session.getStatus() == com.davigama.assessflow.livesession.domain.LiveSessionStatus.FINISHED
                ? score(session, participant.getId()) : null;
        boolean show = assessment.isShowResultsAfterCompletion();
        return new ParticipantState(session.getId(), assessment.getTitle(), session.getStatus(), session.isQuestionOpen(),
                answered, publicQuestion, show ? closed : (closed == null ? null : hideCorrect(closed)),
                show ? score : null, show);
    }

    @Transactional
    public void markConnected(UUID participantId) {
        participants.findById(participantId).ifPresent(participant -> participant.connected(clock.instant()));
    }

    @Transactional
    public void markDisconnected(UUID participantId) {
        participants.findById(participantId).ifPresent(participant -> {
            if (participant.getStatus() == LiveParticipantStatus.LEFT) return;
            participant.disconnected(clock.instant());
            notifier.toHost(participant.getLiveSessionId(),
                    new LiveEvent(LiveEventType.PRESENCE_CHANGED, ParticipantResponse.from(participant)));
        });
    }

    private LiveSession persistWithJoinCode(UUID organizationId, UUID assessmentId, UUID userId) {
        for (int attempt = 0; attempt < 12; attempt++) {
            String code = JoinCodes.generate();
            if (sessions.existsByJoinCode(code)) continue;
            try {
                return sessions.saveAndFlush(new LiveSession(organizationId, assessmentId, code, userId, clock.instant()));
            } catch (DataIntegrityViolationException ignored) {
                // retry with a new code
            }
        }
        throw new DomainException(HttpStatus.CONFLICT, "JOIN_CODE_UNAVAILABLE", "Could not allocate a join code.");
    }

    private void snapshot(LiveSession session, List<AssessmentQuestion> links, UUID organizationId,
                          boolean shuffleQuestions, boolean shuffleAnswers) {
        List<UUID> ids = links.stream().map(AssessmentQuestion::getQuestionId).toList();
        Map<UUID, Question> bank = questions.findWithOptionsByIdInAndOrganizationId(ids, organizationId).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        List<AssessmentQuestion> ordered = new ArrayList<>(links);
        if (shuffleQuestions) java.util.Collections.shuffle(ordered, RANDOM);
        int order = 0;
        for (AssessmentQuestion link : ordered) {
            Question question = bank.get(link.getQuestionId());
            if (question == null) {
                throw new DomainException(HttpStatus.BAD_REQUEST, "ASSESSMENT_HAS_INVALID_QUESTIONS",
                        "The assessment contains questions that are no longer available.");
            }
            LiveSessionQuestion snapshot = new LiveSessionQuestion(session.getId(), question.getId(), question.getText(),
                    question.getType(), order++, link.getPoints());
            List<AnswerOption> options = new ArrayList<>(question.getOptions());
            if (shuffleAnswers) java.util.Collections.shuffle(options, RANDOM);
            int optionOrder = 0;
            for (AnswerOption option : options) {
                snapshot.addOption(new LiveSessionQuestionOption(snapshot, option.getId(), option.getText(),
                        option.isCorrect(), optionOrder++));
            }
            snapshots.save(snapshot);
        }
    }

    private LiveSession host(User actor, UUID organizationId, UUID sessionId) {
        requireOrganization(organizationId);
        access.requireInstructor(organizationId, actor.getId());
        return requireOwned(organizationId, sessionId);
    }

    private LiveSession requireOwned(UUID organizationId, UUID sessionId) {
        return sessions.findByIdAndOrganizationId(sessionId, organizationId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "LIVE_SESSION_NOT_FOUND",
                        "Live session not found."));
    }

    private LiveSession findByCode(String code) {
        String normalized = JoinCodes.normalize(code);
        if (!JoinCodes.valid(normalized)) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_JOIN_CODE", "Join code is invalid.");
        }
        return sessions.findByJoinCode(normalized)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "INVALID_JOIN_CODE", "Join code is invalid."));
    }

    private LiveSessionQuestion currentQuestion(LiveSession session) {
        List<LiveSessionQuestion> items = snapshots.findByLiveSessionIdOrderByDisplayOrderAsc(session.getId());
        if (session.getCurrentQuestionIndex() == null || session.getCurrentQuestionIndex() >= items.size()) {
            throw new DomainException(HttpStatus.CONFLICT, "QUESTION_NOT_ACTIVE", "There is no current question.");
        }
        return items.get(session.getCurrentQuestionIndex());
    }

    private PublicQuestion publicQuestion(LiveSession session) {
        LiveSessionQuestion question = currentQuestion(session);
        List<LiveSessionQuestion> items = snapshots.findByLiveSessionIdOrderByDisplayOrderAsc(session.getId());
        List<PublicOption> options = question.getOptions().stream()
                .sorted(Comparator.comparingInt(LiveSessionQuestionOption::getDisplayOrder))
                .map(option -> new PublicOption(option.getId(), option.getText()))
                .toList();
        return new PublicQuestion(question.getId(), question.getQuestionText(), question.getQuestionType(),
                session.getCurrentQuestionIndex(), items.size(), options);
    }

    private LiveDtos.PublicQuestionResults publicResults(QuestionResults results) {
        return new LiveDtos.PublicQuestionResults(results.questionId(), results.text(), results.answered(),
                results.participants(), results.options().stream()
                .map(option -> new LiveDtos.PublicOptionStat(option.id(), option.text(), option.votes(), option.percent()))
                .toList());
    }

    private QuestionResults results(LiveSession session) {
        LiveSessionQuestion question = currentQuestion(session);
        List<LiveAnswer> submitted = answers.findByLiveSessionIdAndLiveSessionQuestionId(session.getId(), question.getId());
        long totalVotes = submitted.size();
        long participants = this.participants.countByLiveSessionIdAndStatusNot(session.getId(), LiveParticipantStatus.LEFT);
        Map<UUID, Long> votes = submitted.stream()
                .flatMap(answer -> answer.getOptionIds().stream())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        List<OptionResult> options = question.getOptions().stream()
                .sorted(Comparator.comparingInt(LiveSessionQuestionOption::getDisplayOrder))
                .map(option -> {
                    long count = votes.getOrDefault(option.getId(), 0L);
                    double percent = totalVotes == 0 ? 0 : (count * 100.0) / totalVotes;
                    return new OptionResult(option.getId(), option.getText(), option.isCorrect(), count, percent);
                })
                .toList();
        return new QuestionResults(question.getId(), question.getQuestionText(), totalVotes, participants, options);
    }

    private QuestionResults hideCorrect(QuestionResults results) {
        return new QuestionResults(results.questionId(), results.text(), results.answered(), results.participants(),
                results.options().stream()
                        .map(option -> new OptionResult(option.id(), option.text(), false, option.votes(), option.percent()))
                        .toList());
    }

    private Score score(LiveSession session, UUID participantId) {
        List<LiveSessionQuestion> items = snapshots.findByLiveSessionIdOrderByDisplayOrderAsc(session.getId());
        Map<UUID, LiveAnswer> byQuestion = answers.findByLiveSessionIdAndParticipantId(session.getId(), participantId)
                .stream().collect(Collectors.toMap(LiveAnswer::getLiveSessionQuestionId, Function.identity()));
        int earned = 0;
        int possible = 0;
        for (LiveSessionQuestion question : items) {
            possible += question.getPoints();
            LiveAnswer answer = byQuestion.get(question.getId());
            if (answer != null && correct(question, answer.getOptionIds())) earned += question.getPoints();
        }
        double percent = possible == 0 ? 0 : (earned * 100.0) / possible;
        return new Score(earned, possible, percent);
    }

    private boolean correct(LiveSessionQuestion question, Set<UUID> selected) {
        Set<UUID> expected = question.getOptions().stream()
                .filter(LiveSessionQuestionOption::isCorrect)
                .map(LiveSessionQuestionOption::getId)
                .collect(Collectors.toSet());
        return expected.equals(selected);
    }

    private SessionResponse response(LiveSession session) {
        String title = assessments.requireOwned(session.getOrganizationId(), session.getAssessmentId()).getTitle();
        return SessionResponse.from(session, title);
    }

    private void requireOrganization(UUID organizationId) {
        if (!organizations.existsById(organizationId)) {
            throw new OrganizationException(HttpStatus.NOT_FOUND, "ORGANIZATION_NOT_FOUND", "Organization not found.");
        }
    }

}
