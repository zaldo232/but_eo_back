package org.example.but_eo.repository;

import org.example.but_eo.entity.MatchReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<MatchReview, String> {
    boolean existsByMatch_MatchIdAndWriter_UserHashId(String matchId, String writerId);

    List<MatchReview> findByMatch_MatchId(String matchId);

    List<MatchReview> findByTargetTeam_TeamId(String teamId);

    int countByTargetTeam_TeamId(String teamId);
}
