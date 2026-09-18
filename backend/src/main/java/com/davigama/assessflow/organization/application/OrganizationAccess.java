package com.davigama.assessflow.organization.application;

import com.davigama.assessflow.organization.domain.*;
import com.davigama.assessflow.organization.infrastructure.OrganizationMemberRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class OrganizationAccess {
    private final OrganizationMemberRepository members;
    public OrganizationAccess(OrganizationMemberRepository members) { this.members = members; }

    public OrganizationMember requireMember(UUID organizationId, UUID userId) {
        return members.findByOrganizationIdAndUserId(organizationId, userId)
                .filter(member -> member.getStatus() == MemberStatus.ACTIVE)
                .orElseThrow(() -> new OrganizationException(HttpStatus.FORBIDDEN, "NOT_ORGANIZATION_MEMBER",
                        "You are not an active member of this organization."));
    }
    public OrganizationMember requireInstructor(UUID organizationId, UUID userId) {
        OrganizationMember actor = requireMember(organizationId, userId);
        if (actor.getRole() == MemberRole.PARTICIPANT) throw forbidden();
        return actor;
    }

    public OrganizationMember requireManager(UUID organizationId, UUID userId) {
        OrganizationMember actor = requireMember(organizationId, userId);
        if (actor.getRole() != MemberRole.OWNER && actor.getRole() != MemberRole.ADMIN)
            throw forbidden();
        return actor;
    }
    public void requireCanAssign(OrganizationMember actor, MemberRole role) {
        if (role == MemberRole.OWNER && actor.getRole() != MemberRole.OWNER) throw forbidden();
    }
    public void requireCanChange(OrganizationMember actor, OrganizationMember target, MemberRole newRole) {
        if (target.getRole() == MemberRole.OWNER && actor.getRole() != MemberRole.OWNER) throw forbidden();
        requireCanAssign(actor, newRole);
    }
    public void requireCanRemove(OrganizationMember actor, OrganizationMember target) {
        if (target.getRole() == MemberRole.OWNER && actor.getRole() != MemberRole.OWNER) throw forbidden();
    }
    private OrganizationException forbidden() {
        return new OrganizationException(HttpStatus.FORBIDDEN, "INSUFFICIENT_ORGANIZATION_ROLE",
                "You do not have permission to perform this action.");
    }
}
