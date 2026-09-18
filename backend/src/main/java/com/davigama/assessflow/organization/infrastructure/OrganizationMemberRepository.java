package com.davigama.assessflow.organization.infrastructure;

import com.davigama.assessflow.organization.domain.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {
    @EntityGraph(attributePaths = {"user", "organization"})
    Optional<OrganizationMember> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);
    @EntityGraph(attributePaths = {"user", "organization"})
    Optional<OrganizationMember> findByIdAndOrganizationId(UUID id, UUID organizationId);
    @EntityGraph(attributePaths = {"organization"})
    List<OrganizationMember> findByUserIdAndStatus(UUID userId, MemberStatus status);
    @EntityGraph(attributePaths = {"user", "organization"})
    List<OrganizationMember> findByOrganizationIdAndStatusOrderByJoinedAtAsc(UUID organizationId, MemberStatus status);
    long countByOrganizationIdAndRoleAndStatus(UUID organizationId, MemberRole role, MemberStatus status);
}
