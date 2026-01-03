package org.example.but_eo.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.but_eo.dto.InvitationResponse;
import org.example.but_eo.entity.*;
import org.example.but_eo.repository.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final TeamInvitationRepository teamInvitationRepository;
    private final TeamMemberRepository teamMemberRepository;

    //초대 수락
    @Transactional
    public void acceptInvitation(String invitationId, String userId) {
        TeamInvitation invitation = teamInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("초대가 존재하지 않습니다."));

        if (!invitation.getUser().getUserHashId().equals(userId)) {
            throw new IllegalAccessError("본인 초대만 수락할 수 있습니다.");
        }

        if (invitation.getStatus() != TeamInvitation.Status.PENDING) {
            throw new IllegalStateException("이미 처리된 초대입니다.");
        }

        Team team = invitation.getTeam();
        Team.Event event = team.getEvent();

        boolean alreadyInTeam = teamMemberRepository
                .existsByUser_UserHashIdAndTeam_Event(userId, event);
        if (alreadyInTeam) {
            throw new IllegalStateException("이미 해당 종목에 가입된 팀이 있습니다.");
        }

        TeamMemberKey key = new TeamMemberKey(userId, team.getTeamId());
        TeamMember member = new TeamMember();
        member.setTeamMemberKey(key);
        member.setTeam(team);
        member.setUser(invitation.getUser());
        member.setType(TeamMember.Type.MEMBER);
        member.setPosition("멤버");

        teamMemberRepository.save(member);

        invitation.setStatus(TeamInvitation.Status.ACCEPTED);
        teamInvitationRepository.save(invitation);
    }

    //초대 거절
    @Transactional
    public void declineInvitation(String invitationId, String userId) {
        TeamInvitation invitation = teamInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("초대가 존재하지 않습니다."));

        if (!invitation.getUser().getUserHashId().equals(userId)) {
            throw new IllegalAccessError("본인 초대만 거절할 수 있습니다.");
        }

        if (invitation.getStatus() != TeamInvitation.Status.PENDING) {
            throw new IllegalStateException("이미 처리된 초대입니다.");
        }

        invitation.setStatus(TeamInvitation.Status.DECLINED);
        teamInvitationRepository.save(invitation);
    }

    //초대 리스트
    public List<InvitationResponse> getPendingInvitations(String userId) {
        List<TeamInvitation> list = teamInvitationRepository
                .findByUser_UserHashIdAndStatus(userId, TeamInvitation.Status.PENDING);

        return list.stream()
                .map(InvitationResponse::from)
                .toList();
    }

}
