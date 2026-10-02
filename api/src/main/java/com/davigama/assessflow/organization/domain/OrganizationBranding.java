package com.davigama.assessflow.organization.domain;

import com.davigama.assessflow.shared.exception.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.net.URI;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "organization_branding")
public class OrganizationBranding {
    @Id
    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "primary_color", length = 7)
    private String primaryColor;

    @Column(name = "secondary_color", length = 7)
    private String secondaryColor;

    protected OrganizationBranding() {}

    public OrganizationBranding(UUID organizationId, String displayName) {
        this.organizationId = organizationId;
        this.displayName = displayName.trim();
    }

    public void update(String displayName, String logoUrl, String primaryColor, String secondaryColor) {
        this.displayName = displayName.trim();
        this.logoUrl = normalizeUrl(logoUrl);
        this.primaryColor = normalizeColor(primaryColor);
        this.secondaryColor = normalizeColor(secondaryColor);
    }

    private String normalizeColor(String color) {
        if (color == null || color.isBlank()) return null;
        String value = color.trim();
        if (!value.matches("#[0-9A-Fa-f]{6}")) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_COLOR",
                    "Colors must use the #RRGGBB format.");
        }
        return value.toUpperCase(Locale.ROOT);
    }

    private String normalizeUrl(String url) {
        if (url == null || url.isBlank()) return null;
        String value = url.trim();
        if (value.length() > 500) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_LOGO_URL", "Logo URL is too long.");
        }
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            if (scheme == null || uri.getHost() == null
                    || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                throw invalidLogo();
            }
        } catch (IllegalArgumentException ex) {
            throw invalidLogo();
        }
        return value;
    }

    private DomainException invalidLogo() {
        return new DomainException(HttpStatus.BAD_REQUEST, "INVALID_LOGO_URL",
                "Logo URL must be an http or https URL.");
    }

    public UUID getOrganizationId() { return organizationId; }
    public String getDisplayName() { return displayName; }
    public String getLogoUrl() { return logoUrl; }
    public String getPrimaryColor() { return primaryColor; }
    public String getSecondaryColor() { return secondaryColor; }
}
