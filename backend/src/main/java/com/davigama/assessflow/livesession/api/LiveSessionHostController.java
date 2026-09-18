package com.davigama.assessflow.livesession.api;

import com.davigama.assessflow.identity.domain.User;
import com.davigama.assessflow.livesession.api.dto.LiveDtos.ParticipantResponse;
import com.davigama.assessflow.livesession.api.dto.LiveDtos.PublicQuestion;
import com.davigama.assessflow.livesession.api.dto.LiveDtos.QuestionResults;
import com.davigama.assessflow.livesession.api.dto.LiveDtos.SessionResponse;
import com.davigama.assessflow.livesession.application.LiveSessionService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}")
public class LiveSessionHostController {
    private final LiveSessionService service;

    public LiveSessionHostController(LiveSessionService service) {
        this.service = service;
    }

    @PostMapping("/assessments/{assessmentId}/live-sessions")
    public ResponseEntity<SessionResponse> create(@PathVariable UUID organizationId, @PathVariable UUID assessmentId,
                                                  Authentication authentication) {
        return ResponseEntity.status(201).body(service.create(user(authentication), organizationId, assessmentId));
    }

    @GetMapping("/live-sessions/{sessionId}")
    public SessionResponse get(@PathVariable UUID organizationId, @PathVariable UUID sessionId,
                               Authentication authentication) {
        return service.get(user(authentication), organizationId, sessionId);
    }

    @GetMapping("/live-sessions/{sessionId}/participants")
    public List<ParticipantResponse> participants(@PathVariable UUID organizationId, @PathVariable UUID sessionId,
                                                  Authentication authentication) {
        return service.listParticipants(user(authentication), organizationId, sessionId);
    }

    @GetMapping("/live-sessions/{sessionId}/current-question")
    public PublicQuestion currentQuestion(@PathVariable UUID organizationId, @PathVariable UUID sessionId,
                                          Authentication authentication) {
        return service.hostQuestion(user(authentication), organizationId, sessionId);
    }

    @GetMapping("/live-sessions/{sessionId}/results")
    public QuestionResults results(@PathVariable UUID organizationId, @PathVariable UUID sessionId,
                                   Authentication authentication) {
        return service.hostResults(user(authentication), organizationId, sessionId);
    }

    @PostMapping("/live-sessions/{sessionId}/start")
    public SessionResponse start(@PathVariable UUID organizationId, @PathVariable UUID sessionId,
                                 Authentication authentication) {
        return service.start(user(authentication), organizationId, sessionId);
    }

    @PostMapping("/live-sessions/{sessionId}/questions/end")
    public QuestionResults endQuestion(@PathVariable UUID organizationId, @PathVariable UUID sessionId,
                                       Authentication authentication) {
        return service.endQuestion(user(authentication), organizationId, sessionId);
    }

    @PostMapping("/live-sessions/{sessionId}/questions/next")
    public SessionResponse next(@PathVariable UUID organizationId, @PathVariable UUID sessionId,
                                Authentication authentication) {
        return service.nextQuestion(user(authentication), organizationId, sessionId);
    }

    @PostMapping("/live-sessions/{sessionId}/finish")
    public SessionResponse finish(@PathVariable UUID organizationId, @PathVariable UUID sessionId,
                                  Authentication authentication) {
        return service.finish(user(authentication), organizationId, sessionId);
    }

    @GetMapping("/live-sessions/{sessionId}/export")
    public ResponseEntity<String> export(@PathVariable UUID organizationId, @PathVariable UUID sessionId,
                                         Authentication authentication) {
        String csv = service.exportResultsCsv(user(authentication), organizationId, sessionId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"live-session-" + sessionId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @PostMapping("/live-sessions/{sessionId}/cancel")
    public SessionResponse cancel(@PathVariable UUID organizationId, @PathVariable UUID sessionId,
                                  Authentication authentication) {
        return service.cancel(user(authentication), organizationId, sessionId);
    }

    private User user(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }
}
