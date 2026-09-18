package com.davigama.assessflow.organization.application;

import com.davigama.assessflow.identity.domain.User;
import com.davigama.assessflow.identity.infrastructure.UserRepository;
import com.davigama.assessflow.organization.domain.*;
import com.davigama.assessflow.organization.infrastructure.*;
import com.davigama.assessflow.shared.Slug;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {
    private final OrganizationRepository organizations;
    private final OrganizationMemberRepository members;
    private final UserRepository users;
    private final OrganizationAccess access;
    private final Clock clock;
    public OrganizationService(OrganizationRepository organizations, OrganizationMemberRepository members,
                               UserRepository users, OrganizationAccess access, Clock clock) {
        this.organizations = organizations;
        this.members = members;
        this.users = users;
        this.access = access;
        this.clock = clock;
    }
    @Transactional
    public Organization create(User actor, String name, String suppliedSlug) {
        String slug = Slug.normalize(suppliedSlug);
        if (organizations.existsBySlug(slug)) throw duplicateSlug();
        Organization organization = new Organization(name, slug, clock.instant());
        try { organizations.saveAndFlush(organization); }
        catch (DataIntegrityViolationException ex) { throw duplicateSlug(); }
        members.save(new OrganizationMember(organization, actor, MemberRole.OWNER, clock.instant()));
        return organization;
    }
    public record OrganizationView(Organization organization, MemberRole currentUserRole) {}

    @Transactional(readOnly = true)
    public List<OrganizationView> list(User actor) {
        return members.findByUserIdAndStatus(actor.getId(), MemberStatus.ACTIVE).stream()
                .map(member -> new OrganizationView(member.getOrganization(), member.getRole()))
                .toList();
    }
    @Transactional(readOnly = true)
    public OrganizationView get(User actor, UUID organizationId) {
        Organization organization = find(organizationId);
        OrganizationMember member = access.requireMember(organizationId, actor.getId());
        return new OrganizationView(organization, member.getRole());
    }
    @Transactional
    public OrganizationView rename(User actor, UUID organizationId, String name) {
        Organization organization = find(organizationId);
        OrganizationMember member = access.requireManager(organizationId, actor.getId());
        organization.rename(name, clock.instant());
        return new OrganizationView(organization, member.getRole());
    }
    @Transactional(readOnly = true)
    public List<OrganizationMember> listMembers(User actor, UUID organizationId) {
        find(organizationId);
        access.requireMember(organizationId, actor.getId());
        return members.findByOrganizationIdAndStatusOrderByJoinedAtAsc(organizationId, MemberStatus.ACTIVE);
    }
    @Transactional
    public OrganizationMember addMember(User actor, UUID organizationId, String email, MemberRole role) {
        Organization organization = lock(organizationId);
        OrganizationMember manager = access.requireManager(organizationId, actor.getId());
        access.requireCanAssign(manager, role);
        User user = users.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new OrganizationException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND",
                        "User not found."));
        OrganizationMember member = members.findByOrganizationIdAndUserId(organizationId, user.getId()).orElse(null);
        if (member != null) {
            if (member.getStatus() == MemberStatus.ACTIVE)
                throw new OrganizationException(HttpStatus.CONFLICT, "MEMBERSHIP_EXISTS",
                        "User is already a member.");
            member.reactivate(role, clock.instant());
            return member;
        }
        return members.save(new OrganizationMember(organization, user, role, clock.instant()));
    }
    @Transactional
    public OrganizationMember changeRole(User actor, UUID organizationId, UUID memberId, MemberRole role) {
        lock(organizationId);
        OrganizationMember manager = access.requireManager(organizationId, actor.getId());
        OrganizationMember target = activeTarget(organizationId, memberId);
        access.requireCanChange(manager, target, role);
        if (target.getRole() == MemberRole.OWNER && role != MemberRole.OWNER) requireAnotherOwner(organizationId);
        target.changeRole(role);
        return target;
    }
    @Transactional
    public void removeMember(User actor, UUID organizationId, UUID memberId) {
        lock(organizationId);
        OrganizationMember manager = access.requireManager(organizationId, actor.getId());
        OrganizationMember target = activeTarget(organizationId, memberId);
        access.requireCanRemove(manager, target);
        if (target.getRole() == MemberRole.OWNER) requireAnotherOwner(organizationId);
        target.disable();
    }
    private Organization find(UUID id) {
        return organizations.findById(id).orElseThrow(() ->
                new OrganizationException(HttpStatus.NOT_FOUND, "ORGANIZATION_NOT_FOUND", "Organization not found."));
    }
    private Organization lock(UUID id) {
        return organizations.findByIdForUpdate(id).orElseThrow(() ->
                new OrganizationException(HttpStatus.NOT_FOUND, "ORGANIZATION_NOT_FOUND", "Organization not found."));
    }
    private OrganizationMember activeTarget(UUID organizationId, UUID memberId) {
        return members.findByIdAndOrganizationId(memberId, organizationId)
                .filter(member -> member.getStatus() == MemberStatus.ACTIVE)
                .orElseThrow(() -> new OrganizationException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND",
                        "Member not found."));
    }
    private void requireAnotherOwner(UUID organizationId) {
        if (members.countByOrganizationIdAndRoleAndStatus(organizationId, MemberRole.OWNER, MemberStatus.ACTIVE) <= 1)
            throw new OrganizationException(HttpStatus.CONFLICT, "LAST_OWNER",
                    "An organization must have at least one active owner.");
    }
    private OrganizationException duplicateSlug() {
        return new OrganizationException(HttpStatus.CONFLICT, "SLUG_EXISTS", "Organization slug is already in use.");
    }
}
