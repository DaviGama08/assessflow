package com.davigama.assessflow.organization.application;

import com.davigama.assessflow.assessment.infrastructure.AssessmentRepository;
import com.davigama.assessflow.identity.domain.User;
import com.davigama.assessflow.organization.domain.MemberStatus;
import com.davigama.assessflow.organization.infrastructure.OrganizationMemberRepository;
import com.davigama.assessflow.organization.infrastructure.OrganizationRepository;
import com.davigama.assessflow.questionbank.infrastructure.QuestionRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    public record DashboardView(long assessmentCount, long questionCount, long memberCount) {}

    private final OrganizationRepository organizations;
    private final OrganizationMemberRepository members;
    private final OrganizationAccess access;
    private final AssessmentRepository assessments;
    private final QuestionRepository questions;

    public DashboardService(OrganizationRepository organizations, OrganizationMemberRepository members,
                            OrganizationAccess access, AssessmentRepository assessments, QuestionRepository questions) {
        this.organizations = organizations;
        this.members = members;
        this.access = access;
        this.assessments = assessments;
        this.questions = questions;
    }

    @Transactional(readOnly = true)
    public DashboardView get(User actor, UUID organizationId) {
        if (!organizations.existsById(organizationId)) {
            throw new OrganizationException(HttpStatus.NOT_FOUND, "ORGANIZATION_NOT_FOUND",
                    "Organization not found.");
        }
        access.requireMember(organizationId, actor.getId());
        return new DashboardView(
                assessments.countByOrganizationId(organizationId),
                questions.countByOrganizationId(organizationId),
                members.countByOrganizationIdAndStatus(organizationId, MemberStatus.ACTIVE));
    }
}
