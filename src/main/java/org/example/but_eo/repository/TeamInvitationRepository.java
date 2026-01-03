package org.example.but_eo.repository;

import org.example.but_eo.entity.Team;
import org.example.but_eo.entity.TeamInvitation;
import org.example.but_eo.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, String> {

    // 초대 중복 체크 (기존)
    @Query("""
        SELECT COUNT(i) > 0
        FROM TeamInvitation i
        WHERE i.user.userHashId = :userId
        AND i.team.teamId = :teamId
        AND i.status = :status
    """)
    boolean existsPendingByUserAndTeam(
            @Param("userId") String userId,
            @Param("teamId") String teamId,
            @Param("status") TeamInvitation.Status status
    );

    // 유저가 보낸 가입 신청 중복 확인
    @Query("""
        SELECT COUNT(i) > 0
        FROM TeamInvitation i
        WHERE i.user.userHashId = :userId
        AND i.team.teamId = :teamId
        AND i.direction = :direction
        AND i.status = 'PENDING'
    """)
    boolean existsPendingByUserAndTeamAndDirection(
            @Param("userId") String userId,
            @Param("teamId") String teamId,
            @Param("direction") TeamInvitation.Direction direction
    );

    // 특정 가입 신청 조회 (수락/거절 처리)
    Optional<TeamInvitation> findByUser_UserHashIdAndTeam_TeamIdAndDirectionAndStatus(
            String userId,
            String teamId,
            TeamInvitation.Direction direction,
            TeamInvitation.Status status
    );

    // 팀 리더가 유저들의 가입 신청 목록 조회 (PENDING + REQUEST)
    List<TeamInvitation> findAllByTeam_TeamIdAndStatusAndDirection(
            String teamId,
            TeamInvitation.Status status,
            TeamInvitation.Direction direction
    );

    List<TeamInvitation> findByUser_UserHashIdAndStatus(String userId, TeamInvitation.Status status);

    Optional<TeamInvitation> findByUser_UserHashIdAndTeam_TeamIdAndStatus(String userId, String teamId, TeamInvitation.Status status);

    boolean existsByUser_UserHashIdAndTeam_TeamId(String userId, String teamId);

    boolean existsByUser_UserHashIdAndTeam_TeamIdAndStatusAndDirection(
            String userId, String teamId, TeamInvitation.Status status, TeamInvitation.Direction direction
    );

    void deleteAllByUser(Users user);

    void deleteAllByTeam(Team team);
}
