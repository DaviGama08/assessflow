package com.davigama.assessflow.livesession.api;

import com.davigama.assessflow.livesession.api.dto.LiveDtos.AnswerRequest;
import com.davigama.assessflow.livesession.api.dto.LiveDtos.JoinRequest;
import com.davigama.assessflow.livesession.api.dto.LiveDtos.JoinResponse;
import com.davigama.assessflow.livesession.api.dto.LiveDtos.ParticipantState;
import com.davigama.assessflow.livesession.api.dto.LiveDtos.PreviewResponse;
import com.davigama.assessflow.livesession.application.LiveSessionService;
import com.davigama.assessflow.livesession.application.ParticipantPrincipal;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/live-sessions")
public class LiveJoinController {
    private final LiveSessionService service;

    public LiveJoinController(LiveSessionService service) {
        this.service = service;
    }

    @GetMapping("/preview")
    public PreviewResponse preview(@RequestParam String code) {
        return service.preview(code);
    }

    @PostMapping("/join")
    public JoinResponse join(@Valid @RequestBody JoinRequest request) {
        return service.join(request);
    }

    @GetMapping("/{sessionId}/state")
    public ParticipantState state(@PathVariable UUID sessionId, Authentication authentication) {
        return service.state(principal(authentication), sessionId);
    }

    @PostMapping("/{sessionId}/answers")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void answer(@PathVariable UUID sessionId, @Valid @RequestBody AnswerRequest request,
                       Authentication authentication) {
        service.answer(principal(authentication), sessionId, request);
    }

    private ParticipantPrincipal principal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof ParticipantPrincipal principal)) {
            throw new com.davigama.assessflow.shared.exception.DomainException(
                    HttpStatus.UNAUTHORIZED, "PARTICIPANT_NOT_FOUND",
                    "Participant token required.");
        }
        return principal;
    }
}
