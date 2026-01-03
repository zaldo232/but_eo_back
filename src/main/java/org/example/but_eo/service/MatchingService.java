    package org.example.but_eo.service;

    import lombok.RequiredArgsConstructor;
    import org.example.but_eo.component.MatchQueue;
    import org.example.but_eo.dto.*;
    import org.example.but_eo.entity.*;
    import org.example.but_eo.repository.*;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.PageRequest;
    import org.springframework.data.domain.Pageable;
    import org.springframework.data.domain.Sort;
    import org.springframework.messaging.simp.SimpMessagingTemplate;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;

    import java.time.LocalDate;
    import java.time.LocalDateTime;
    import java.time.LocalTime;
    import java.util.List;
    import java.util.Optional;
    import java.util.Set;
    import java.util.UUID;

    @Service
    @RequiredArgsConstructor
    public class MatchingService {

        private final TeamRepository teamRepository;
        private final UsersRepository usersRepository;
        private final StadiumRepository stadiumRepository;
        private final MatchingRepository matchingRepository;
        private final TeamMemberRepository teamMemberRepository;
        private final ChallengerListRepository challengerListRepository;
        private final MatchQueue matchQueue;
        private final SimpMessagingTemplate simpMessagingTemplate;

        @Transactional
        public void createMatch(MatchCreateRequest request, String userId) {
            // matchType → Team.Event 변환
            Team.Event event;
            try {
                event = Team.Event.valueOf(request.getMatchType());
            } catch (Exception e) {
                throw new RuntimeException("매치 타입이 잘못되었습니다.");
            }

            // 종목 기반 리더 조회
            TeamMember leader = teamMemberRepository
                    .findByUser_UserHashIdAndTypeAndTeam_Event(userId, TeamMember.Type.LEADER, event)
                    .orElseThrow(() -> new RuntimeException("해당 종목에서 리더인 팀이 없습니다."));

            Team team = leader.getTeam();

            // 매치 생성
            Matching matching = new Matching();
            matching.setMatchId(UUID.randomUUID().toString());
            matching.setTeam(team);
            matching.setMatchRegion(request.getRegion());
            matching.setTeamRegion(team.getRegion());
            matching.setEtc(request.getEtc());
            matching.setState(Matching.State.WAITING);

            // 날짜 + 시간 파싱
            LocalDate date;
            LocalTime time;
            LocalDateTime matchDate;
            try {
                date = LocalDate.parse(request.getMatchDay());
                time = LocalTime.parse(request.getMatchTime());
                matchDate = LocalDateTime.of(date, time);
                matching.setMatchDate(matchDate);
            } catch (Exception e) {
                throw new RuntimeException("날짜 또는 시간 형식이 잘못되었습니다.");
            }

            // 같은 팀이 같은 시간에 등록한 매치가 있는지 확인
            boolean exists = matchingRepository.existsByTeam_TeamIdAndMatchDate(team.getTeamId(), matchDate);
            if (exists) {
                throw new RuntimeException("해당 시간에 이미 등록된 매치가 존재합니다.");
            }

            // 대여 여부 파싱
            try {
                matching.setLoan(Boolean.parseBoolean(request.getLoan()));
            } catch (Exception e) {
                throw new RuntimeException("대여 여부 형식이 잘못되었습니다.");
            }

            // 종목 파싱
            try {
                matching.setMatchType(Matching.Match_Type.from(request.getMatchType()));
            } catch (Exception e) {
                throw new RuntimeException("매치 타입이 잘못되었습니다.");
            }

            matchingRepository.save(matching);
        }


        public Page<MatchingListResponse> getMatchings(Matching.Match_Type matchType, String region, int page, int size) {
            Pageable pageable = PageRequest.of(page, size, Sort.by("matchDate").descending());

            Page<Matching> matchingPage;

            if (matchType != null && region != null) {
                matchingPage = matchingRepository.findByMatchTypeAndMatchRegionAndState(
                        matchType, region, Matching.State.WAITING, pageable);
            } else if (matchType != null) {
                matchingPage = matchingRepository.findByMatchTypeAndState(
                        matchType, Matching.State.WAITING, pageable);
            } else if (region != null) {
                matchingPage = matchingRepository.findByMatchRegionAndState(
                        region, Matching.State.WAITING, pageable);
            } else {
                matchingPage = matchingRepository.findByState(Matching.State.WAITING, pageable);
            }

            return matchingPage.map(m -> {
                // 1. 수락된 팀 (상대팀)
                ChallengerTeamResponse challengerTeam = null;
                if (m.getChallengerTeam() != null) {
                    Team challenger = m.getChallengerTeam();
                    challengerTeam = new ChallengerTeamResponse(
                            challenger.getTeamId(),
                            challenger.getTeamName(),
                            challenger.getRegion(),
                            challenger.getRating()
                    );
                }
                // 2. 신청 들어온 팀 전체
                List<ChallengerList> challengers = challengerListRepository.findByMatching_MatchId(m.getMatchId());
                List<ChallengerTeamResponse> challengerTeams = challengers.stream()
                        .map(c -> new ChallengerTeamResponse(
                                c.getTeam().getTeamId(),
                                c.getTeam().getTeamName(),
                                c.getTeam().getRegion(),
                                c.getTeam().getRating()
                        )).toList();

                return new MatchingListResponse(
                        m.getMatchId(),
                        m.getMatchRegion() != null ? m.getMatchRegion() : "미정",
                        m.getTeam().getTeamName(),
                        m.getTeam().getTeamImg(),
                        m.getTeam().getRegion(),
                        m.getTeam().getRating(),
                        m.getStadium() != null ? m.getStadium().getStadiumName() : "미정",
                        m.getMatchDate(),
                        m.getMatchType().getDisplayName(),
                        m.getLoan(),
                        challengerTeam,
                        challengerTeams
                );
            });
        }


        public MatchingDetailResponse getMatchDetail(String matchId) {
            Matching matching = matchingRepository.findById(matchId)
                    .orElseThrow(() -> new RuntimeException("매치가 존재하지 않습니다."));

            // 모든 신청(도전자) 팀 리스트 조회
            List<ChallengerList> challengers = challengerListRepository.findByMatching_MatchId(matchId);
            List<ChallengerTeamResponse> challengerTeams = challengers.stream()
                    .map(c -> new ChallengerTeamResponse(
                            c.getTeam().getTeamId(),
                            c.getTeam().getTeamName(),
                            c.getTeam().getRegion(),
                            c.getTeam().getRating()
                    ))
                    .toList();

            // 기존 challengerTeamDto(수락된 팀)는 그대로 두고, 신청리스트도 같이 내려줌
            ChallengerTeamResponse challengerTeamDto = null;
            if (matching.getChallengerTeam() != null) {
                Team challenger = matching.getChallengerTeam();
                challengerTeamDto = new ChallengerTeamResponse(
                        challenger.getTeamId(),
                        challenger.getTeamName(),
                        challenger.getRegion(),
                        challenger.getRating()
                );
            }

            return new MatchingDetailResponse(
                    matching.getMatchId(),
                    matching.getMatchRegion() != null ? matching.getMatchRegion() : "미정",
                    matching.getTeam().getTeamName(),
                    matching.getTeamRegion(),
                    matching.getTeam().getTeamImg(),
                    matching.getTeam().getRating(),
                    matching.getStadium() != null ? matching.getStadium().getStadiumName() : "미정",
                    matching.getMatchDate(),
                    matching.getLoan(),
                    matching.getMatchType().getDisplayName(),
                    matching.getEtc(),
                    matching.getChallengerTeam() != null ? matching.getChallengerTeam().getTeamName() : null,
                    matching.getWinnerScore(),
                    matching.getLoserScore(),
                    matching.getState(),
                    challengerTeamDto,
                    challengerTeams // <<== 추가!
            );
        }


        @Transactional
        public void applyChallenge(String matchId, String userId) {
            Matching matching = matchingRepository.findById(matchId)
                    .orElseThrow(() -> new RuntimeException("매치 없음"));

            if (matching.getState() != Matching.State.WAITING) {
                throw new RuntimeException("이미 도전이 수락된 매치입니다.");
            }

            // 현재 유저가 속한 팀 찾기 (도전자)
            // 종목 정보 → Team.Event로 변환
            Team.Event event = Team.Event.valueOf(matching.getMatchType().name());

            // 조건 좁혀서 리더 단건 조회
            TeamMember member = teamMemberRepository
                    .findByUser_UserHashIdAndTypeAndTeam_Event(userId, TeamMember.Type.LEADER, event)
                    .orElseThrow(() -> new RuntimeException("해당 종목에서 리더인 팀이 없습니다."));

            Team challengerTeam = member.getTeam();
            // 리더만 도전 신청 가능
            if (member.getType() != TeamMember.Type.LEADER) {
                throw new RuntimeException("리더만 도전 신청할 수 있습니다.");
            }

            // 자기가 만든 매치에 도전 x
            if (matching.getTeam().getTeamId().equals(challengerTeam.getTeamId())) {
                throw new RuntimeException("자기 팀 매치에는 도전할 수 없습니다.");
            }

            // 중복 도전 x
            ChallengerKey key = new ChallengerKey(matchId, challengerTeam.getTeamId());
            if (challengerListRepository.existsById(key)) {
                throw new RuntimeException("이미 도전 신청한 매치입니다.");
            }

            // 도전 신청 저장
            ChallengerList challenger = new ChallengerList();
            challenger.setChallengerKey(key);
            challenger.setMatching(matching);
            challenger.setTeam(challengerTeam);
            challengerListRepository.save(challenger);
        }

        public List<ChallengerTeamResponse> getChallengerTeams(String matchId, String userId) {
            Matching match = matchingRepository.findById(matchId)
                    .orElseThrow(() -> new RuntimeException("매치가 존재하지 않습니다."));

            Team hostTeam = match.getTeam();

            // 요청자가 리더인지 확인
            boolean isLeader = hostTeam.getTeamMemberList().stream()
                    .anyMatch(m -> m.getUser().getUserHashId().equals(userId)
                            && m.getType() == TeamMember.Type.LEADER);
            if (!isLeader) {
                throw new RuntimeException("리더만 도전 목록을 조회할 수 있습니다.");
            }

            // 도전자 목록 가져오기
            List<ChallengerList> challengers = challengerListRepository.findByMatching_MatchId(matchId);

            return challengers.stream()
                    .map(c -> {
                        Team team = c.getTeam();
                        return new ChallengerTeamResponse(
                                team.getTeamId(),
                                team.getTeamName(),
                                team.getRegion(),
                                team.getRating()
                        );
                    })
                    .toList();
        }

        @Transactional
        public void acceptChallenge(String matchId, String challengerTeamId, String userId) {
            Matching matching = matchingRepository.findById(matchId)
                    .orElseThrow(() -> new RuntimeException("매치 없음"));

            if (matching.getState() != Matching.State.WAITING)
                throw new RuntimeException("이미 수락된 매치입니다.");

            Team hostTeam = matching.getTeam();
            boolean isLeader = hostTeam.getTeamMemberList().stream()
                    .anyMatch(m -> m.getUser().getUserHashId().equals(userId)
                            && m.getType() == TeamMember.Type.LEADER);
            if (!isLeader)
                throw new RuntimeException("리더만 수락할 수 있습니다.");

            Team challenger = teamRepository.findById(challengerTeamId)
                    .orElseThrow(() -> new RuntimeException("도전 팀 없음"));

            // 도전 신청 존재 여부 확인
            ChallengerKey key = new ChallengerKey(matchId, challengerTeamId);
            if (!challengerListRepository.existsById(key)) {
                throw new RuntimeException("도전 신청이 없습니다.");
            }

            // 수락 처리
            matching.setChallengerTeam(challenger);
            matching.setState(Matching.State.SUCCESS);
            matchingRepository.save(matching);

            // 나머지 도전 신청들 제거
            challengerListRepository.deleteAllByMatching_MatchId(matchId);
        }

        @Transactional
        public void declineChallenge(String matchId, String challengerTeamId, String userId) {
            Matching matching = matchingRepository.findById(matchId)
                    .orElseThrow(() -> new RuntimeException("매치 없음"));

            Team hostTeam = matching.getTeam();
            boolean isLeader = hostTeam.getTeamMemberList().stream()
                    .anyMatch(m -> m.getUser().getUserHashId().equals(userId)
                            && m.getType() == TeamMember.Type.LEADER);
            if (!isLeader)
                throw new RuntimeException("리더만 거절할 수 있습니다.");

            ChallengerKey key = new ChallengerKey(matchId, challengerTeamId);
            if (!challengerListRepository.existsById(key)) {
                throw new RuntimeException("도전 신청이 없습니다.");
            }

            challengerListRepository.deleteById(key);
        }

        @Transactional
        public void cancelMatch(String matchId, String userId) {
            Matching matching = matchingRepository.findById(matchId)
                    .orElseThrow(() -> new RuntimeException("매치가 존재하지 않습니다."));

            // 상태 체크: WAITING만 가능
            if (matching.getState() != Matching.State.WAITING) {
                throw new RuntimeException("매치가 대기 상태일 때만 취소할 수 있습니다.");
            }

            //리더인지 체크
            Team team = matching.getTeam();
            boolean isLeader = team.getTeamMemberList().stream()
                    .anyMatch(m -> m.getUser().getUserHashId().equals(userId)
                            && m.getType() == TeamMember.Type.LEADER);
            if (!isLeader) {
                throw new RuntimeException("리더만 매치를 취소할 수 있습니다.");
            }

            matching.setState(Matching.State.CANCEL);
            matchingRepository.save(matching);
        }

        //매치 결과 등록
        @Transactional
        public void registerMatchResult(String matchId, MatchResultRequest request, String userId) {
            Matching matching = matchingRepository.findById(matchId)
                    .orElseThrow(() -> new RuntimeException("매치가 존재하지 않습니다."));

            // 상태가 SUCCESS인지 확인
            if (matching.getState() != Matching.State.SUCCESS) {
                throw new RuntimeException("도전 수락된 매치만 결과 등록이 가능합니다.");
            }

            //리더인지 체크
            Team hostTeam = matching.getTeam();
            boolean isLeader = hostTeam.getTeamMemberList().stream()
                    .anyMatch(m -> m.getUser().getUserHashId().equals(userId)
                            && m.getType() == TeamMember.Type.LEADER);
            if (!isLeader) {
                throw new RuntimeException("리더만 결과를 등록할 수 있습니다.");
            }

            //스코어 가져오기
            int winnerScore = request.getWinnerScore();
            int loserScore = request.getLoserScore();

            Team team1 = matching.getTeam();              // 주최팀
            Team team2 = matching.getChallengerTeam();    // 도전자팀

            if (team2 == null) {
                throw new RuntimeException("도전자 팀 정보가 없습니다.");
            }

            // 승리 팀 자동 판별
            Team winnerTeam = null;
            Team loserTeam = null;

            if (winnerScore > loserScore) {
                winnerTeam = team1;
                loserTeam = team2;
            } else if (winnerScore < loserScore) {
                winnerTeam = team2;
                loserTeam = team1;
            }

            matching.setWinnerScore(winnerScore);
            matching.setLoserScore(loserScore);
            matching.setWinnerTeam(winnerTeam);
            matching.setLoserTeam(loserTeam);
            matching.setState(Matching.State.COMPLETE);

            // 레이팅 반영
            if (winnerScore > loserScore) {
                // 승자 처리
                winnerTeam.setRating(winnerTeam.getRating() + 30);
                winnerTeam.setMatchCount(winnerTeam.getMatchCount() + 1);
                winnerTeam.setWinCount(winnerTeam.getWinCount() + 1);

                // 패자 처리
                loserTeam.setMatchCount(loserTeam.getMatchCount() + 1);
                loserTeam.setLoseCount(loserTeam.getLoseCount() + 1);
            } else if (winnerScore == loserScore) {
                int bonus = (winnerScore == 0) ? 10 : 20;         //무승부면 2점 0:0이면 1점
                team1.setRating(team1.getRating() + bonus);
                team2.setRating(team2.getRating() + bonus);

                //매치 카운트 증가
                team1.setMatchCount(team1.getMatchCount() + 1);
                team2.setMatchCount(team2.getMatchCount() + 1);

                //무승부 카운트 증가
                team1.setDrawCount(team1.getDrawCount() + 1);
                team2.setDrawCount(team2.getDrawCount() + 1);
            }

            matchingRepository.save(matching);
        }

        // 팀 ID로 매치 리스트 조회 (페이징X 버전)
        public List<MatchingListResponse> getMatchingsByTeamId(String teamId) {
            // 1. 호스트 매치
            List<Matching> hostMatches = matchingRepository.findByTeam_TeamId(teamId);

            // 2. 도전자 매치
            List<Matching> challengerMatches = matchingRepository.findByChallengerTeam_TeamId(teamId);

            // 3. 중복 제거 및 합치기
            Set<Matching> allMatches = new java.util.HashSet<>();
            allMatches.addAll(hostMatches);
            allMatches.addAll(challengerMatches);

            // 4. 날짜 내림차순(최신순) 정렬
            List<Matching> sorted = allMatches.stream()
                    .sorted(java.util.Comparator.comparing(Matching::getMatchDate).reversed())
                    .toList();

            // 5. DTO 매핑(기존 방식)
            return sorted.stream().map(m -> {
                ChallengerTeamResponse challengerTeam = null;
                if (m.getChallengerTeam() != null) {
                    Team challenger = m.getChallengerTeam();
                    challengerTeam = new ChallengerTeamResponse(
                            challenger.getTeamId(),
                            challenger.getTeamName(),
                            challenger.getRegion(),
                            challenger.getRating()
                    );
                }
                List<ChallengerList> challengers = challengerListRepository.findByMatching_MatchId(m.getMatchId());
                List<ChallengerTeamResponse> challengerTeams = challengers.stream()
                        .map(c -> new ChallengerTeamResponse(
                                c.getTeam().getTeamId(),
                                c.getTeam().getTeamName(),
                                c.getTeam().getRegion(),
                                c.getTeam().getRating()
                        )).toList();

                return new MatchingListResponse(
                        m.getMatchId(),
                        m.getMatchRegion() != null ? m.getMatchRegion() : "미정",
                        m.getTeam().getTeamName(),
                        m.getTeam().getTeamImg(),
                        m.getTeam().getRegion(),
                        m.getTeam().getRating(),
                        m.getStadium() != null ? m.getStadium().getStadiumName() : "미정",
                        m.getMatchDate(),
                        m.getMatchType().getDisplayName(),
                        m.getLoan(),
                        challengerTeam,
                        challengerTeams
                );
            }).toList();
        }


        // 도전 수락 - 팀ID + 매치ID + 도전자팀ID + 리더 권한 + 안전 검증
        @Transactional
        public void acceptChallengeByTeam(String teamId, String matchId, String challengerTeamId, String userId) {
            // (1) 매치 존재 및 소유팀 체크
            Matching matching = matchingRepository.findById(matchId)
                    .orElseThrow(() -> new RuntimeException("매치 없음"));
            if (!matching.getTeam().getTeamId().equals(teamId)) {
                throw new RuntimeException("이 팀이 만든 매치가 아닙니다.");
            }
            // (2) 리더 권한 체크
            boolean isLeader = matching.getTeam().getTeamMemberList().stream()
                    .anyMatch(m -> m.getUser().getUserHashId().equals(userId) && m.getType() == TeamMember.Type.LEADER);
            if (!isLeader) {
                throw new RuntimeException("리더만 수락 가능합니다.");
            }
            // (3) 상태 체크
            if (matching.getState() != Matching.State.WAITING)
                throw new RuntimeException("이미 수락된 매치입니다.");

            // (4) 도전자팀 존재 및 신청 존재 체크
            Team challenger = teamRepository.findById(challengerTeamId)
                    .orElseThrow(() -> new RuntimeException("도전 팀 없음"));
            ChallengerKey key = new ChallengerKey(matchId, challengerTeamId);
            if (!challengerListRepository.existsById(key)) {
                throw new RuntimeException("도전 신청이 없습니다.");
            }

            // (5) 수락 처리
            matching.setChallengerTeam(challenger);
            matching.setState(Matching.State.SUCCESS);
            matchingRepository.save(matching);
            // 나머지 도전 신청 삭제
            challengerListRepository.deleteAllByMatching_MatchId(matchId);
        }

        // 도전 거절 - 팀ID + 매치ID + 도전자팀ID + 리더 권한 + 안전 검증
        @Transactional
        public void declineChallengeByTeam(String teamId, String matchId, String challengerTeamId, String userId) {
            // (1) 매치 존재 및 소유팀 체크
            Matching matching = matchingRepository.findById(matchId)
                    .orElseThrow(() -> new RuntimeException("매치 없음"));
            if (!matching.getTeam().getTeamId().equals(teamId)) {
                throw new RuntimeException("이 팀이 만든 매치가 아닙니다.");
            }
            // (2) 리더 권한 체크
            boolean isLeader = matching.getTeam().getTeamMemberList().stream()
                    .anyMatch(m -> m.getUser().getUserHashId().equals(userId) && m.getType() == TeamMember.Type.LEADER);
            if (!isLeader) {
                throw new RuntimeException("리더만 거절 가능합니다.");
            }
            // (3) 도전자팀 존재 및 신청 존재 체크
            ChallengerKey key = new ChallengerKey(matchId, challengerTeamId);
            if (!challengerListRepository.existsById(key)) {
                throw new RuntimeException("도전 신청이 없습니다.");
            }
            // (4) 거절 처리
            challengerListRepository.deleteById(key);
        }

        // Success 조회
        public Page<MatchingListResponse> getSuccessMatchings(int page, int size) {
            Pageable pageable = PageRequest.of(page, size, Sort.by("matchDate").descending());
            Page<Matching> matchingPage = matchingRepository.findByState(Matching.State.SUCCESS, pageable);

            return matchingPage.map(m -> {
                ChallengerTeamResponse challengerTeam = null;
                if (m.getChallengerTeam() != null) {
                    Team challenger = m.getChallengerTeam();
                    challengerTeam = new ChallengerTeamResponse(
                            challenger.getTeamId(),
                            challenger.getTeamName(),
                            challenger.getRegion(),
                            challenger.getRating()
                    );
                }

                List<ChallengerList> challengers = challengerListRepository.findByMatching_MatchId(m.getMatchId());
                List<ChallengerTeamResponse> challengerTeams = challengers.stream()
                        .map(c -> new ChallengerTeamResponse(
                                c.getTeam().getTeamId(),
                                c.getTeam().getTeamName(),
                                c.getTeam().getRegion(),
                                c.getTeam().getRating()
                        )).toList();

                return new MatchingListResponse(
                        m.getMatchId(),
                        m.getMatchRegion() != null ? m.getMatchRegion() : "미정",
                        m.getTeam().getTeamName(),
                        m.getTeam().getTeamImg(),
                        m.getTeam().getRegion(),
                        m.getTeam().getRating(),
                        m.getStadium() != null ? m.getStadium().getStadiumName() : "미정",
                        m.getMatchDate(),
                        m.getMatchType().getDisplayName(),
                        m.getLoan(),
                        challengerTeam,
                        challengerTeams
                );
            });
        }

        // Complete 조회
        public Page<MatchingListResponse> getCompleteMatchings(int page, int size) {
            Pageable pageable = PageRequest.of(page, size, Sort.by("matchDate").descending());
            Page<Matching> matchingPage = matchingRepository.findByState(Matching.State.COMPLETE, pageable);

            return matchingPage.map(m -> {
                ChallengerTeamResponse challengerTeam = null;
                if (m.getChallengerTeam() != null) {
                    Team challenger = m.getChallengerTeam();
                    challengerTeam = new ChallengerTeamResponse(
                            challenger.getTeamId(),
                            challenger.getTeamName(),
                            challenger.getRegion(),
                            challenger.getRating()
                    );
                }
                List<ChallengerList> challengers = challengerListRepository.findByMatching_MatchId(m.getMatchId());
                List<ChallengerTeamResponse> challengerTeams = challengers.stream()
                        .map(c -> new ChallengerTeamResponse(
                                c.getTeam().getTeamId(),
                                c.getTeam().getTeamName(),
                                c.getTeam().getRegion(),
                                c.getTeam().getRating()
                        )).toList();

                return new MatchingListResponse(
                        m.getMatchId(),
                        m.getMatchRegion() != null ? m.getMatchRegion() : "미정",
                        m.getTeam().getTeamName(),
                        m.getTeam().getTeamImg(),
                        m.getTeam().getRegion(),
                        m.getTeam().getRating(),
                        m.getStadium() != null ? m.getStadium().getStadiumName() : "미정",
                        m.getMatchDate(),
                        m.getMatchType().getDisplayName(),
                        m.getLoan(),
                        challengerTeam,
                        challengerTeams
                );
            });
        }

        // 팀의 매치 성공 조회
        public List<MatchingListResponse> getSuccessMatchingsByTeam(String teamId) {
            // 1. 호스트로 참여한 SUCCESS 매치
            List<Matching> host = matchingRepository.findByTeam_TeamIdAndState(teamId, Matching.State.SUCCESS);
            // 2. 챌린저로 참여한 SUCCESS 매치
            List<Matching> challenger = matchingRepository.findByChallengerTeam_TeamIdAndState(teamId, Matching.State.SUCCESS);

            // 3. 합치고 중복 제거
            Set<Matching> all = new java.util.HashSet<>();
            all.addAll(host);
            all.addAll(challenger);

            // 4. 최신순 정렬 (선택)
            List<Matching> sorted = all.stream()
                    .sorted(java.util.Comparator.comparing(Matching::getMatchDate).reversed())
                    .toList();

            // 5. DTO 매핑
            return sorted.stream().map(m -> {
                ChallengerTeamResponse challengerTeam = null;
                if (m.getChallengerTeam() != null) {
                    Team c = m.getChallengerTeam();
                    challengerTeam = new ChallengerTeamResponse(
                            c.getTeamId(),
                            c.getTeamName(),
                            c.getRegion(),
                            c.getRating()
                    );
                }
                List<ChallengerList> challengers = challengerListRepository.findByMatching_MatchId(m.getMatchId());
                List<ChallengerTeamResponse> challengerTeams = challengers.stream()
                        .map(c -> new ChallengerTeamResponse(
                                c.getTeam().getTeamId(),
                                c.getTeam().getTeamName(),
                                c.getTeam().getRegion(),
                                c.getTeam().getRating()
                        )).toList();

                return new MatchingListResponse(
                        m.getMatchId(),
                        m.getMatchRegion() != null ? m.getMatchRegion() : "미정",
                        m.getTeam().getTeamName(),
                        m.getTeam().getTeamImg(),
                        m.getTeam().getRegion(),
                        m.getTeam().getRating(),
                        m.getStadium() != null ? m.getStadium().getStadiumName() : "미정",
                        m.getMatchDate(),
                        m.getMatchType().getDisplayName(),
                        m.getLoan(),
                        challengerTeam,
                        challengerTeams
                );
            }).toList();
        }


        // 팀의 매치 완료 조회
        public List<MatchingListResponse> getCompleteMatchingsByTeam(String teamId) {
            List<Matching> matchings = matchingRepository.findByTeam_TeamIdAndState(teamId, Matching.State.COMPLETE);
            return matchings.stream()
                    .map(m -> {
                        ChallengerTeamResponse challengerTeam = null;
                        if (m.getChallengerTeam() != null) {
                            Team challenger = m.getChallengerTeam();
                            challengerTeam = new ChallengerTeamResponse(
                                    challenger.getTeamId(),
                                    challenger.getTeamName(),
                                    challenger.getRegion(),
                                    challenger.getRating()
                            );
                        }
                        List<ChallengerList> challengers = challengerListRepository.findByMatching_MatchId(m.getMatchId());
                        List<ChallengerTeamResponse> challengerTeams = challengers.stream()
                                .map(c -> new ChallengerTeamResponse(
                                        c.getTeam().getTeamId(),
                                        c.getTeam().getTeamName(),
                                        c.getTeam().getRegion(),
                                        c.getTeam().getRating()
                                )).toList();

                        return new MatchingListResponse(
                                m.getMatchId(),
                                m.getMatchRegion() != null ? m.getMatchRegion() : "미정",
                                m.getTeam().getTeamName(),
                                m.getTeam().getTeamImg(),
                                m.getTeam().getRegion(),
                                m.getTeam().getRating(),
                                m.getStadium() != null ? m.getStadium().getStadiumName() : "미정",
                                m.getMatchDate(),
                                m.getMatchType().getDisplayName(),
                                m.getLoan(),
                                challengerTeam,
                                challengerTeams
                        );
                    })
                    .toList();
        }

        // 내가 속한 전체 팀중 젤 최신 일정 가져오기
        public MatchingListResponse getLatestSuccessMatchByUser(String userId) {
            // 1. 내가 속한 팀 리스트 가져오기
            List<TeamMember> teamMembers = teamMemberRepository.findAllByUser_UserHashId(userId);
            System.out.println("userId : " + userId);

            List<String> teamIds = teamMembers.stream()
                    .map(tm -> tm.getTeam().getTeamId())
                    .distinct()
                    .toList();
            System.out.println("teamIds : " + teamIds);

            if (teamIds.isEmpty()) {
                System.out.println("소속된 팀이 없습니다: " + userId);
                throw new RuntimeException("소속된 팀이 없습니다.");
            }

            // 2. 현재 시각 기준 가장 가까운 미래 SUCCESS 매치 1건 (호스트/챌린저 모두 포함)
            LocalDateTime now = LocalDateTime.now();
            Optional<Matching> hostMatchingOpt =
                    matchingRepository.findTopByTeam_TeamIdInAndStateAndMatchDateAfterOrderByMatchDateAsc(
                            teamIds, Matching.State.SUCCESS, now);

            Optional<Matching> challengerMatchingOpt =
                    matchingRepository.findTopByChallengerTeam_TeamIdInAndStateAndMatchDateAfterOrderByMatchDateAsc(
                            teamIds, Matching.State.SUCCESS, now);

            Matching selected = null;
            if (hostMatchingOpt.isPresent() && challengerMatchingOpt.isPresent()) {
                LocalDateTime hostDate = hostMatchingOpt.get().getMatchDate();
                LocalDateTime challengerDate = challengerMatchingOpt.get().getMatchDate();
                System.out.println("host matchId : " + hostMatchingOpt.get().getMatchId() + ", date : " + hostDate);
                System.out.println("challenger matchId : " + challengerMatchingOpt.get().getMatchId() + ", date : " + challengerDate);
                selected = hostDate.isBefore(challengerDate) ? hostMatchingOpt.get() : challengerMatchingOpt.get();
            } else if (hostMatchingOpt.isPresent()) {
                selected = hostMatchingOpt.get();
                System.out.println("host matchId : " + selected.getMatchId() + ", date : " + selected.getMatchDate());
            } else if (challengerMatchingOpt.isPresent()) {
                selected = challengerMatchingOpt.get();
                System.out.println("challenger matchId : " + selected.getMatchId() + ", date : " + selected.getMatchDate());
            } else {
                System.out.println("SUCCESS 상태 미래 매치가 없습니다 : " + teamIds);
                throw new RuntimeException("SUCCESS 상태 미래 매치가 없습니다.");
            }

            System.out.println("선택된 매치 matchId : " + selected.getMatchId());

            // 3. 매치 DTO 매핑 (기존 방식)
            ChallengerTeamResponse challengerTeam = null;
            if (selected.getChallengerTeam() != null) {
                Team challenger = selected.getChallengerTeam();
                challengerTeam = new ChallengerTeamResponse(
                        challenger.getTeamId(),
                        challenger.getTeamName(),
                        challenger.getRegion(),
                        challenger.getRating()
                );
            }
            List<ChallengerList> challengers = challengerListRepository.findByMatching_MatchId(selected.getMatchId());
            List<ChallengerTeamResponse> challengerTeams = challengers.stream()
                    .map(c -> new ChallengerTeamResponse(
                            c.getTeam().getTeamId(),
                            c.getTeam().getTeamName(),
                            c.getTeam().getRegion(),
                            c.getTeam().getRating()
                    )).toList();

            MatchingListResponse response = new MatchingListResponse(
                    selected.getMatchId(),
                    selected.getMatchRegion() != null ? selected.getMatchRegion() : "미정",
                    selected.getTeam().getTeamName(),
                    selected.getTeam().getTeamImg(),
                    selected.getTeam().getRegion(),
                    selected.getTeam().getRating(),
                    selected.getStadium() != null ? selected.getStadium().getStadiumName() : "미정",
                    selected.getMatchDate(),
                    selected.getMatchType().getDisplayName(),
                    selected.getLoan(),
                    challengerTeam,
                    challengerTeams

            );

            System.out.println("result : " + response);
            return response;
        }

        public boolean requestAutoMatch(RequestAutoMatch requestAutoMatch) {
            Optional<Team> team = teamRepository.findById(requestAutoMatch.getTeamId());
            if (team.isEmpty()) {
                return false;
            }
            requestAutoMatch.setSportType(team.get().getEvent().toString());
            requestAutoMatch.setRegion(team.get().getRegion());
            requestAutoMatch.setRating(team.get().getRating());

            matchQueue.enqueue(requestAutoMatch);
            return true;
        }

        public void handleMatchResponse(String matchId, String userId, MatchResponseDto.MatchResponseType response) {
            Matching match = matchingRepository.findByMatchId(matchId);
//            .orElseThrow(() -> new NotFoundException("매칭 없음"))

            if (response == MatchResponseDto.MatchResponseType.REJECTED) {
                match.setState(Matching.State.CANCEL);
            } else {
                match.setState(Matching.State.COMPLETE);
            }

            matchingRepository.save(match);

            simpMessagingTemplate.convertAndSendToUser(
                    userId, "/queue/match",
                    new MatchResultDto(matchId, match.getState(),
                                       userId.equals(match.getTeam().getTeamId()) ?
                                       match.getChallengerTeam().getTeamName() :
                                       match.getTeam().getTeamName())
            );
        }

        public MatchResultDto getMatchStatus(String matchId) {
            Matching match = matchingRepository.findByMatchId(matchId);
//                    .orElseThrow(() -> new NotFoundException("매칭 없음"))

            return new MatchResultDto(
                    match.getMatchId(),
                    match.getState(),
                    match.getChallengerTeam() != null ? match.getChallengerTeam().getTeamName() : null
            );
        }
    }

