package com.davigama.assessflow.organization.api;

import com.davigama.assessflow.identity.domain.User;
import com.davigama.assessflow.organization.application.OrganizationService;
import com.davigama.assessflow.organization.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {
    public record CreateOrganizationRequest(@NotBlank @Size(max = 200) String name,
                                            @NotBlank String slug) {}
    public record AddMemberRequest(@NotBlank String email, @NotNull MemberRole role) {}
    public record ChangeRoleRequest(@NotNull MemberRole role) {}
    public record OrganizationResponse(UUID id, String name, String slug, OrganizationStatus status,
                                       Instant createdAt, Instant updatedAt) {
        static OrganizationResponse from(Organization organization) {
            return new OrganizationResponse(organization.getId(), organization.getName(), organization.getSlug(),
                    organization.getStatus(), organization.getCreatedAt(), organization.getUpdatedAt());
        }
    }
    public record MemberResponse(UUID id, UUID organizationId, UUID userId, String email, String displayName,
                                 MemberRole role, MemberStatus status, Instant joinedAt) {
        static MemberResponse from(OrganizationMember member) {
            User user = member.getUser();
            return new MemberResponse(member.getId(), member.getOrganization().getId(), user.getId(),
                    user.getEmail(), user.getDisplayName(), member.getRole(), member.getStatus(), member.getJoinedAt());
        }
    }
    private final OrganizationService service;
    public OrganizationController(OrganizationService service) { this.service = service; }
    @PostMapping
    public ResponseEntity<OrganizationResponse> create(@Valid @RequestBody CreateOrganizationRequest request,
                                                       Authentication authentication) {
        Organization organization = service.create(current(authentication), request.name(), request.slug());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(organization.getId()).toUri();
        return ResponseEntity.created(location).body(OrganizationResponse.from(organization));
    }
    @GetMapping
    public List<OrganizationResponse> list(Authentication authentication) {
        return service.list(current(authentication)).stream().map(OrganizationResponse::from).toList();
    }
    @GetMapping("/{organizationId}")
    public OrganizationResponse get(@PathVariable UUID organizationId, Authentication authentication) {
        return OrganizationResponse.from(service.get(current(authentication), organizationId));
    }
    @GetMapping("/{organizationId}/members")
    public List<MemberResponse> listMembers(@PathVariable UUID organizationId, Authentication authentication) {
        return service.listMembers(current(authentication), organizationId).stream().map(MemberResponse::from).toList();
    }
    @PostMapping("/{organizationId}/members")
    public ResponseEntity<MemberResponse> addMember(@PathVariable UUID organizationId,
                                                      @Valid @RequestBody AddMemberRequest request,
                                                      Authentication authentication) {
        OrganizationMember member = service.addMember(current(authentication), organizationId,
                request.email(), request.role());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{memberId}")
                .buildAndExpand(member.getId()).toUri();
        return ResponseEntity.created(location).body(MemberResponse.from(member));
    }
    @PatchMapping("/{organizationId}/members/{memberId}/role")
    public MemberResponse changeRole(@PathVariable UUID organizationId, @PathVariable UUID memberId,
                                     @Valid @RequestBody ChangeRoleRequest request, Authentication authentication) {
        return MemberResponse.from(service.changeRole(current(authentication), organizationId, memberId, request.role()));
    }
    @DeleteMapping("/{organizationId}/members/{memberId}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID organizationId, @PathVariable UUID memberId,
                                             Authentication authentication) {
        service.removeMember(current(authentication), organizationId, memberId);
        return ResponseEntity.noContent().build();
    }
    private User current(Authentication authentication) { return (User) authentication.getPrincipal(); }
}
