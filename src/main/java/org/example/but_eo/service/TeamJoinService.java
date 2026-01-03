package org.example.but_eo.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.but_eo.entity.*;
import org.example.but_eo.repository.TeamInvitationRepository;
import org.example.but_eo.repository.TeamMemberRepository;
import org.example.but_eo.repository.TeamRepository;
import org.example.but_eo.repository.UsersRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeamJoinService {

    private final TeamRepository teamRepository;
    private final UsersRepository usersRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamInvitationRepository teamInvitationRepository;

    // 유저가 팀에 가입 신청
    @Transactional
    public void requestToJoinTeam(String teamId, String userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀이 존재하지 않습니다."));
        Users user = usersRepository.findByUserHashId(userId);

        boolean alreadyRequested = teamInvitationRepository.existsPendingByUserAndTeamAndDirection(
                userId, teamId, TeamInvitation.Direction.REQUEST);
        if (alreadyRequested) throw new IllegalStateException("이미 신청한 팀입니다.");

        TeamInvitation invitation = TeamInvitation.createRequest(team, user);
        teamInvitationRepository.save(invitation);
    }

    // 유저가 본인 신청 취소
    @Transactional
    public void cancelJoinRequest(String teamId, String userId) {
        TeamInvitation invitation = teamInvitationRepository
                .findByUser_UserHashIdAndTeam_TeamIdAndDirectionAndStatus(
                        userId, teamId, TeamInvitation.Direction.REQUEST, TeamInvitation.Status.PENDING)
                .orElseThrow(() -> new IllegalArgumentException("신청 기록 없음"));

        teamInvitationRepository.delete(invitation);
    }

    // 리더가 가입 신청 수락
    @Transactional
    public void acceptJoinRequest(String teamId, String targetUserId, String leaderId) {
        TeamMember leader = teamMemberRepository.findById(new TeamMemberKey(leaderId, teamId))
                .orElseThrow(() -> new IllegalAccessError("리더 권한 없음"));

        if (leader.getType() != TeamMember.Type.LEADER) throw new IllegalAccessError("리더만 수락 가능");

        TeamInvitation invitation = teamInvitationRepository
                .findByUser_UserHashIdAndTeam_TeamIdAndDirectionAndStatus(
                        targetUserId, teamId, TeamInvitation.Direction.REQUEST, TeamInvitation.Status.PENDING)
                .orElseThrow(() -> new IllegalArgumentException("해당 신청 없음"));

        invitation.setStatus(TeamInvitation.Status.ACCEPTED);
        teamInvitationRepository.save(invitation);

        Users user = usersRepository.findByUserHashId(targetUserId);
        Team team = invitation.getTeam();

        TeamMember member = new TeamMember(new TeamMemberKey(targetUserId, teamId), user, team, TeamMember.Type.MEMBER, "팀원");
        teamMemberRepository.save(member);
    }

    // 리더가 가입 신청 거절
    @Transactional
    public void rejectJoinRequest(String teamId, String targetUserId, String leaderId) {
        TeamMember leader = teamMemberRepository.findById(new TeamMemberKey(leaderId, teamId))
                .orElseThrow(() -> new IllegalAccessError("리더 권한 없음"));

        if (leader.getType() != TeamMember.Type.LEADER) throw new IllegalAccessError("리더만 거절 가능");

        TeamInvitation invitation = teamInvitationRepository
                .findByUser_UserHashIdAndTeam_TeamIdAndDirectionAndStatus(
                        targetUserId, teamId, TeamInvitation.Direction.REQUEST, TeamInvitation.Status.PENDING)
                .orElseThrow(() -> new IllegalArgumentException("해당 신청 없음"));

        invitation.setStatus(TeamInvitation.Status.DECLINED);
        teamInvitationRepository.save(invitation);
    }

    public boolean isAlreadyRequested(String teamId, String userId) {
        return teamInvitationRepository.existsPendingByUserAndTeamAndDirection(
                userId, teamId, TeamInvitation.Direction.REQUEST
        );
    }

}
