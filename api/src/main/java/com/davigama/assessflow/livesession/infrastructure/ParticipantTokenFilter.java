package com.davigama.assessflow.livesession.infrastructure;

import com.davigama.assessflow.livesession.application.ParticipantAuthService;
import com.davigama.assessflow.livesession.application.ParticipantPrincipal;
import com.davigama.assessflow.shared.exception.DomainException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ParticipantTokenFilter extends OncePerRequestFilter {
    private final ParticipantAuthService participants;

    public ParticipantTokenFilter(ParticipantAuthService participants) {
        this.participants = participants;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                try {
                    ParticipantPrincipal principal = participants.authenticate(header.substring(7));
                    if (principal != null) {
                        SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of()));
                    }
                } catch (DomainException ex) {
                    response.setStatus(ex.getStatus().value());
                    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                    response.getWriter().write("{\"title\":\"Unauthorized\",\"status\":" + ex.getStatus().value()
                            + ",\"code\":\"" + ex.getCode() + "\",\"detail\":\"" + ex.getMessage() + "\"}");
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }
}
