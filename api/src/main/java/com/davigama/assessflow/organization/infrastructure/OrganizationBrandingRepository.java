package com.davigama.assessflow.organization.infrastructure;

import com.davigama.assessflow.organization.domain.OrganizationBranding;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationBrandingRepository extends JpaRepository<OrganizationBranding, UUID> {
    Optional<OrganizationBranding> findByOrganizationId(UUID organizationId);
}
