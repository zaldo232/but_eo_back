package org.example.but_eo.repository;

import org.example.but_eo.entity.Matching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MatchingRepository extends JpaRepository<Matching, String> {

    // 종목, 상태 기반 조회 (선택적으로 지역이나 날짜도 필터링 가능)
    List<Matching> findByMatchTypeAndState(Matching.Match_Type matchType, Matching.State state);

    // 상태 기반 최신순 조회
    List<Matching> findByStateOrderByMatchDateDesc(Matching.State state);

    // 팀 ID로 매치 목록
    List<Matching> findByTeam_TeamId(String teamId);

    //경기장 기준으로 지역가져옴
    Page<Matching> findByMatchTypeAndStadium_StadiumRegionAndState(Matching.Match_Type matchType, String region, Matching.State state, Pageable pageable);
    Page<Matching> findByMatchTypeAndState(Matching.Match_Type matchType, Matching.State state, Pageable pageable);

    Page<Matching> findByMatchRegionAndState(String matchRegion, Matching.State state, Pageable pageable);

    Page<Matching> findByMatchTypeAndMatchRegionAndState(Matching.Match_Type matchType, String matchRegion, Matching.State state, Pageable pageable);

    // 기본 상태 필터
    Page<Matching> findByState(Matching.State state, Pageable pageable);

    // 매치 중복 등록 방지
    boolean existsByTeam_TeamIdAndMatchDate(String teamId, LocalDateTime matchDate);

    List<Matching> findByTeam_TeamIdAndState(String teamId, Matching.State state);

    Optional<Matching> findTopByTeam_TeamIdInAndStateOrderByMatchDateDesc(List<String> teamIds, Matching.State state);

    // 가장 가까운 미래 SUCCESS 매치 1건 조회 (여러 팀에 대해)
    // 호스트 팀으로 속한 매치
    Optional<Matching> findTopByTeam_TeamIdInAndStateAndMatchDateAfterOrderByMatchDateAsc(List<String> teamIds, Matching.State state, LocalDateTime matchDate);

    // 챌린저 팀으로 속한 매치
    Optional<Matching> findTopByChallengerTeam_TeamIdInAndStateAndMatchDateAfterOrderByMatchDateAsc(List<String> teamIds, Matching.State state, LocalDateTime matchDate);

    // 챌린저로 성공 매치
    List<Matching> findByChallengerTeam_TeamIdAndState(String teamId, Matching.State state);

    List<Matching> findByTeam_TeamIdIn(List<String> teamIds);
    List<Matching> findByChallengerTeam_TeamIdIn(List<String> teamIds);
    List<Matching> findByChallengerTeam_TeamId(String teamId);
    Matching findByMatchId(String matchId);
}

