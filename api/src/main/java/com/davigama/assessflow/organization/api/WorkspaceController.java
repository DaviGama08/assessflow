package com.davigama.assessflow.organization.api;

import com.davigama.assessflow.identity.domain.User;
import com.davigama.assessflow.organization.application.BrandingService;
import com.davigama.assessflow.organization.application.DashboardService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}")
public class WorkspaceController {
    public record BrandingRequest(
            @NotBlank @Size(max = 200) String displayName,
            @Size(max = 500) String logoUrl,
            @Size(max = 7) String primaryColor,
            @Size(max = 7) String secondaryColor) {}

    public record BrandingResponse(UUID organizationId, String displayName, String logoUrl, String primaryColor,
                                   String secondaryColor) {
        static BrandingResponse from(BrandingService.BrandingView view) {
            return new BrandingResponse(view.organizationId(), view.displayName(), view.logoUrl(),
                    view.primaryColor(), view.secondaryColor());
        }
    }

    public record DashboardResponse(long assessmentCount, long questionCount, long memberCount) {
        static DashboardResponse from(DashboardService.DashboardView view) {
            return new DashboardResponse(view.assessmentCount(), view.questionCount(), view.memberCount());
        }
    }

    private final BrandingService branding;
    private final DashboardService dashboard;

    public WorkspaceController(BrandingService branding, DashboardService dashboard) {
        this.branding = branding;
        this.dashboard = dashboard;
    }

    @GetMapping("/branding")
    public BrandingResponse getBranding(@PathVariable UUID organizationId, Authentication authentication) {
        return BrandingResponse.from(branding.get(current(authentication), organizationId));
    }

    @PutMapping("/branding")
    public BrandingResponse updateBranding(@PathVariable UUID organizationId,
                                           @Valid @RequestBody BrandingRequest request,
                                           Authentication authentication) {
        return BrandingResponse.from(branding.update(current(authentication), organizationId, request.displayName(),
                request.logoUrl(), request.primaryColor(), request.secondaryColor()));
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(@PathVariable UUID organizationId, Authentication authentication) {
        return DashboardResponse.from(dashboard.get(current(authentication), organizationId));
    }

    private User current(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }
}
