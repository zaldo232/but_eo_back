package org.example.but_eo.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.angus.mail.iap.Response;
import org.example.but_eo.dto.*;
import org.example.but_eo.entity.Matching;
import org.example.but_eo.service.MatchingService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/matchings")
public class MatchingController {

    private final MatchingService matchingService;

    @PostMapping("/create")
    public ResponseEntity<?> createMatch(@RequestBody MatchCreateRequest request) {
        String userId = SecurityUtil.getCurrentUserId();
        matchingService.createMatch(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body("매치 생성 완료");
    }

    @GetMapping
    public ResponseEntity<Page<MatchingListResponse>> getMatchings(
            @RequestParam(required = false) String matchType,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Matching.Match_Type parsedType = null;
        if (matchType != null && !matchType.isBlank()) {
            try {
                parsedType = Matching.Match_Type.from(matchType);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(null);
            }
        }

        Page<MatchingListResponse> result = matchingService.getMatchings(parsedType, region, page, size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{matchId}")
    public ResponseEntity<MatchingDetailResponse> getMatchDetail(@PathVariable String matchId) {
        MatchingDetailResponse response = matchingService.getMatchDetail(matchId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{matchId}/challenge")
    public ResponseEntity<?> applyChallenge(@PathVariable String matchId) {
        String userId = SecurityUtil.getCurrentUserId();
        matchingService.applyChallenge(matchId, userId);
        return ResponseEntity.ok("도전 신청 완료");
    }

    @GetMapping("/{matchId}/challenges")
    public ResponseEntity<List<ChallengerTeamResponse>> getChallengerList(@PathVariable String matchId) {
        String userId = SecurityUtil.getCurrentUserId();
        List<ChallengerTeamResponse> challengers = matchingService.getChallengerTeams(matchId, userId);
        return ResponseEntity.ok(challengers);
    }

    @PatchMapping("/{matchId}/accept/{challengerTeamId}")
    public ResponseEntity<?> acceptChallenge(@PathVariable String matchId,
                                             @PathVariable String challengerTeamId) {
        String userId = SecurityUtil.getCurrentUserId();
        matchingService.acceptChallenge(matchId, challengerTeamId, userId);
        return ResponseEntity.ok("도전 수락 완료");
    }

    @DeleteMapping("/{matchId}/decline/{challengerTeamId}")
    public ResponseEntity<?> declineChallenge(@PathVariable String matchId,
                                              @PathVariable String challengerTeamId) {
        String userId = SecurityUtil.getCurrentUserId();
        matchingService.declineChallenge(matchId, challengerTeamId, userId);
        return ResponseEntity.ok("도전 거절 완료");
    }

    @PatchMapping("/{matchId}/cancel")
    public ResponseEntity<?> cancelMatch(@PathVariable String matchId) {
        String userId = SecurityUtil.getCurrentUserId();
        matchingService.cancelMatch(matchId, userId);
        return ResponseEntity.ok("매치가 취소되었습니다.");
    }

    @PatchMapping("/{matchId}/result")
    public ResponseEntity<?> registerResult(@PathVariable String matchId,
                                            @RequestBody MatchResultRequest request) {
        String userId = SecurityUtil.getCurrentUserId();
        matchingService.registerMatchResult(matchId, request, userId);
        return ResponseEntity.ok("경기 결과 등록 완료");
    }

    // 팀 ID로 매치 리스트 조회 (페이징X 버전)
    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<MatchingListResponse>> getMyTeamMatchings(@PathVariable String teamId) {
        List<MatchingListResponse> result = matchingService.getMatchingsByTeamId(teamId);
        return ResponseEntity.ok(result);
    }

    // 도전 수락 (리더만 가능)
    @PatchMapping("/team/{teamId}/matchings/{matchId}/accept/{challengerTeamId}")
    public ResponseEntity<?> acceptChallengeByTeam(
            @PathVariable String teamId,
            @PathVariable String matchId,
            @PathVariable String challengerTeamId
    ) {
        String userId = SecurityUtil.getCurrentUserId();
        matchingService.acceptChallengeByTeam(teamId, matchId, challengerTeamId, userId);
        return ResponseEntity.ok("도전 수락 완료");
    }

    // 도전 거절 (리더만 가능)
    @PatchMapping("/team/{teamId}/matchings/{matchId}/decline/{challengerTeamId}")
    public ResponseEntity<?> declineChallengeByTeam(
            @PathVariable String teamId,
            @PathVariable String matchId,
            @PathVariable String challengerTeamId
    ) {
        String userId = SecurityUtil.getCurrentUserId();
        matchingService.declineChallengeByTeam(teamId, matchId, challengerTeamId, userId);
        return ResponseEntity.ok("도전 거절 완료");
    }

    // Success 조회
    @GetMapping("/success")
    public ResponseEntity<Page<MatchingListResponse>> getSuccessMatchings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<MatchingListResponse> result = matchingService.getSuccessMatchings(page, size);
        return ResponseEntity.ok(result);
    }

    // Complete 조회
    @GetMapping("/complete")
    public ResponseEntity<Page<MatchingListResponse>> getCompleteMatchings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<MatchingListResponse> result = matchingService.getCompleteMatchings(page, size);
        return ResponseEntity.ok(result);
    }

    // 팀의 매치 성공 조회
    @GetMapping("/team/{teamId}/success")
    public ResponseEntity<List<MatchingListResponse>> getSuccessMatchingsByTeam(@PathVariable String teamId) {
        List<MatchingListResponse> result = matchingService.getSuccessMatchingsByTeam(teamId);
        return ResponseEntity.ok(result);
    }

    // 팀의 매치 완료 조회
    @GetMapping("/team/{teamId}/complete")
    public ResponseEntity<List<MatchingListResponse>> getCompleteMatchingsByTeam(@PathVariable String teamId) {
        List<MatchingListResponse> result = matchingService.getCompleteMatchingsByTeam(teamId);
        return ResponseEntity.ok(result);
    }

    // 내가 속한 전체 팀중 젤 최신 일정 가져오기
    @GetMapping("/my/latest-success")
    public ResponseEntity<MatchingListResponse> getLatestSuccessMatchByUser() {
        String userId = SecurityUtil.getCurrentUserId();
        System.out.println(("[/my/latest-success] userId: {}"+ userId));
        MatchingListResponse result = matchingService.getLatestSuccessMatchByUser(userId);
        System.out.println("result : "+ result);
        return ResponseEntity.ok(result);
    }

    public class SecurityUtil {
        public static String getCurrentUserId() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new RuntimeException("인증 정보 없음");
            }
            return authentication.getName(); // 또는 JWT claim 기반으로 userId 추출
        }
    }

    @PostMapping("/auto")
    public ResponseEntity<?> requestAutoMatch(@RequestBody RequestAutoMatch requestAutoMatch, Authentication authentication) {
        if(matchingService.requestAutoMatch(requestAutoMatch)) {
            return ResponseEntity.ok("매칭 요청 완료");
        } else {
            log.warn("매칭 요청 실패");
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("auto/{matchId}/respond")
    public ResponseEntity<?> respond(@PathVariable String matchId,
                                     @RequestBody MatchResponseDto dto,
                                     Authentication authentication) {
        matchingService.handleMatchResponse(matchId, authentication.getPrincipal().toString(), dto.getResponse());
        return ResponseEntity.ok("응답 완료");
    }

    @GetMapping("auto/{matchId}/status")
    public ResponseEntity<MatchResultDto> status(@PathVariable String matchId) {
        return ResponseEntity.ok(matchingService.getMatchStatus(matchId));
    }
}

