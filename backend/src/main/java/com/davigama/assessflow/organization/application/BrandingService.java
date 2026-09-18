package com.davigama.assessflow.organization.application;

import com.davigama.assessflow.identity.domain.User;
import com.davigama.assessflow.organization.domain.Organization;
import com.davigama.assessflow.organization.domain.OrganizationBranding;
import com.davigama.assessflow.organization.infrastructure.OrganizationBrandingRepository;
import com.davigama.assessflow.organization.infrastructure.OrganizationRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BrandingService {
    public record BrandingView(UUID organizationId, String displayName, String logoUrl, String primaryColor,
                               String secondaryColor) {}

    private final OrganizationBrandingRepository branding;
    private final OrganizationRepository organizations;
    private final OrganizationAccess access;

    public BrandingService(OrganizationBrandingRepository branding, OrganizationRepository organizations,
                           OrganizationAccess access) {
        this.branding = branding;
        this.organizations = organizations;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public BrandingView get(User actor, UUID organizationId) {
        Organization organization = requireOrganization(organizationId);
        access.requireMember(organizationId, actor.getId());
        return view(organization, branding.findByOrganizationId(organizationId).orElse(null));
    }

    @Transactional
    public BrandingView update(User actor, UUID organizationId, String displayName, String logoUrl,
                               String primaryColor, String secondaryColor) {
        Organization organization = requireOrganization(organizationId);
        access.requireManager(organizationId, actor.getId());
        OrganizationBranding current = branding.findByOrganizationId(organizationId)
                .orElseGet(() -> new OrganizationBranding(organizationId, organization.getName()));
        current.update(displayName, logoUrl, primaryColor, secondaryColor);
        return view(organization, branding.save(current));
    }

    private BrandingView view(Organization organization, OrganizationBranding current) {
        if (current == null) {
            return new BrandingView(organization.getId(), organization.getName(), null, null, null);
        }
        return new BrandingView(current.getOrganizationId(), current.getDisplayName(), current.getLogoUrl(),
                current.getPrimaryColor(), current.getSecondaryColor());
    }

    private Organization requireOrganization(UUID organizationId) {
        return organizations.findById(organizationId).orElseThrow(() ->
                new OrganizationException(HttpStatus.NOT_FOUND, "ORGANIZATION_NOT_FOUND", "Organization not found."));
    }
}
