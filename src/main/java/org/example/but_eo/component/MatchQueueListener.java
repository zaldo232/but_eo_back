package org.example.but_eo.component;

import lombok.RequiredArgsConstructor;
import org.example.but_eo.dto.MatchResultDto;
import org.example.but_eo.dto.RequestAutoMatch;
import org.example.but_eo.entity.Matching;
import org.example.but_eo.entity.Team;
import org.example.but_eo.repository.MatchingRepository;
import org.example.but_eo.service.TeamService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MatchQueueListener {

    private final MatchQueue matchQueue;
    private final MatchingRepository matchingRepository;
    private final TeamService teamService;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleMatchQueueEvent(MatchQueueEvent event) {
        matchQueue.tryMatch(event.getSportType(), event.getRegion()).ifPresent(pair -> {
            RequestAutoMatch reqA = pair.get(0);
            RequestAutoMatch reqB = pair.get(1);

            Matching match = new Matching();
            match.setMatchId(UUID.randomUUID().toString());
            match.setMatchType(Matching.Match_Type.valueOf(reqA.getSportType()));
            match.setMatchRegion(reqA.getRegion());
            match.setState(Matching.State.WAITING);
            match.setMatchDate(LocalDateTime.now());

            List<Team> matchedTeams = teamService.getMatchedTeams(reqA.getTeamId(), reqB.getTeamId());
            Team teamA = null;
            Team teamB = null;

            for (Team team : matchedTeams) {
                if (team.getTeamId().equals(reqA.getTeamId())) {
                    match.setTeam(team);
                    teamA = team;
                } else {
                    match.setChallengerTeam(team);
                    teamB = team;
                }
            }

            Matching savedMatch = matchingRepository.save(match);

            // ----------------------------------------------------
            // ✨ 1. 웹소켓 알림 전송 로직 추가 ✨
            // ----------------------------------------------------

            // TeamService에서 리더 ID를 조회하는 함수가 필요합니다.
            String leaderAHashId = teamService.getLeaderHashId(reqA.getTeamId());
            String leaderBHashId = teamService.getLeaderHashId(reqB.getTeamId());

            // 알림 DTO 생성 (MatchResultDto 사용)
            MatchResultDto resultDtoForA = createMatchResultDto(savedMatch, teamB.getTeamName()); // 상대 팀은 B
            MatchResultDto resultDtoForB = createMatchResultDto(savedMatch, teamA.getTeamName()); // 상대 팀은 A

            final String MATCH_QUEUE_DESTINATION = "/queue/match";

            // Team A 리더에게 전송 (프론트엔드가 구독한 경로: /user/{userId}/queue/match)
            messagingTemplate.convertAndSendToUser(
                    leaderAHashId,
                    MATCH_QUEUE_DESTINATION,
                    resultDtoForA
            );

            // Team B 리더에게 전송
            messagingTemplate.convertAndSendToUser(
                    leaderBHashId,
                    MATCH_QUEUE_DESTINATION,
                    resultDtoForB
            );

        });
    }

    private MatchResultDto createMatchResultDto(Matching match, String opponentTeamName) {
        return new MatchResultDto(
                match.getMatchId(),
                match.getState(),
                opponentTeamName
        );
    }
}
