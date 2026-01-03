package org.example.but_eo.repository;

import org.example.but_eo.entity.Team;
import org.example.but_eo.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, String> {

    @Query("""
    SELECT t FROM Team t
    LEFT JOIN FETCH t.teamMemberList tm
    LEFT JOIN FETCH tm.user
    WHERE t.teamId = :teamId
""")
    Optional<Team> findWithMembersByTeamId(@Param("teamId") String teamId);

    boolean existsByTeamName(String teamName);

    // 목록 전체 ACTIVE만 조회
    List<Team> findAllByState(Team.State state);

    // 단일 조회 ACTIVE만
    Optional<Team> findWithMembersByTeamIdAndState(String teamId, Team.State state);

    @Query("""
    SELECT t.teamId
    FROM Team t
    JOIN TeamMember tm ON t.teamId = tm.team.teamId
    WHERE tm.user.userHashId = :userId
      AND t.event = :sportType
    """)
    String findTeamIdByUserIdAndSportType(@Param("userId") String userId, @Param("sportType") String sportType);

    @Query(value = "SELECT * FROM Team WHERE teamId = :teamA OR teamId = :teamB", nativeQuery = true)
    List<Team> findMatchedTeams(@Param("teamA") String teamA, @Param("teamB") String teamB);
}
