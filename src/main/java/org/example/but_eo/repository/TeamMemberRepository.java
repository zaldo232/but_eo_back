package org.example.but_eo.repository;

import org.example.but_eo.entity.TeamMember;
import org.example.but_eo.entity.TeamMemberKey;
import org.example.but_eo.entity.Team;
import org.example.but_eo.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, TeamMemberKey> {

    /*@Query("""
        SELECT COUNT(tm) > 0
        FROM TeamMember tm
        WHERE tm.user.userHashId = :userId
        AND tm.team.event = :event
    """)*/

    boolean existsByUser_UserHashIdAndTeam_TeamId(String userId, String teamId);

    boolean existsByUser_UserHashIdAndTeam_Event(String userId, Team.Event event);

    void deleteAllByUser(Users user);

    long countByTeam_TeamId(String teamId);

    List<TeamMember> findAllByUser(Users user);

    Optional<TeamMember> findByUser_UserHashId(String userId);

    Optional<TeamMember> findByUser_UserHashIdAndType(String userId, TeamMember.Type type);

    List<TeamMember> findAllByUser_UserHashIdAndType(String userHashId, TeamMember.Type type);

    List<TeamMember> findAllByUser_UserHashId(String userId);

    Optional<TeamMember> findByUser_UserHashIdAndTypeAndTeam_Event(
            String userHashId,
            TeamMember.Type type,
            Team.Event event
    );

    @Query("SELECT tm.user FROM TeamMember tm WHERE tm.team.teamId = :teamId AND tm.type = 'LEADER'")
    Optional<Users> findLeaderByTeamId(@Param("teamId") String teamId);


    @Query(value = "SELECT * FROM Team WHERE teamId = :teamA OR teamId = :teamB", nativeQuery = true)
    List<Team> findMatchedTeams(@Param("teamA") String teamA, @Param("teamB") String teamB);
}
