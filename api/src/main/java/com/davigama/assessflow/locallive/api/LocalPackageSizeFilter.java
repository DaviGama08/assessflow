package com.davigama.assessflow.locallive.api;

import com.davigama.assessflow.locallive.application.LocalPackageSettings;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class LocalPackageSizeFilter extends OncePerRequestFilter {
    private final LocalPackageSettings settings;

    public LocalPackageSizeFilter(LocalPackageSettings settings) {
        this.settings = settings;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if ("POST".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI() != null
                && request.getRequestURI().endsWith("/local-live/packages")) {
            long length = request.getContentLengthLong();
            if (length > settings.maxPayloadBytes()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getWriter().write(
                        "{\"title\":\"Invalid request\",\"status\":400,\"code\":\"INVALID_LOCAL_PACKAGE\","
                                + "\"detail\":\"The local event package exceeds the allowed size.\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
