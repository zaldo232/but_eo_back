package org.example.but_eo.controller;

import lombok.RequiredArgsConstructor;
import org.example.but_eo.dto.TeamJoinRequestDto;
import org.example.but_eo.dto.TeamResponse;
import org.example.but_eo.dto.UpdateTeamRequest;
import org.example.but_eo.entity.Team;
import org.example.but_eo.service.TeamService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    // 팀 생성
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> createTeam(
            @RequestParam("team_name") String teamName,
            @RequestParam("event") String eventStr,
            @RequestParam("region") String region,
            @RequestParam("member_age") int memberAge,
            @RequestParam("team_case") String teamCaseStr,
            @RequestParam("team_description") String teamDescription,
            @RequestPart(value = "team_img", required = false) MultipartFile teamImg,
            Authentication authentication) {

        Team.Event event = Team.Event.valueOf(eventStr);
        Team.Team_Case teamCase = Team.Team_Case.valueOf(teamCaseStr);
        String userId = (String) authentication.getPrincipal();

        teamService.createTeam(teamName, event, region, memberAge, teamCase, teamDescription, teamImg, userId);
        return ResponseEntity.ok("팀 생성 성공");
    }

    // 팀 수정
    @PatchMapping(value = "/{teamId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateTeam(
            @PathVariable String teamId,
            @ModelAttribute UpdateTeamRequest request,
            Authentication authentication) {

        String userId = (String) authentication.getPrincipal();
        teamService.updateTeam(teamId, request, userId);
        return ResponseEntity.ok("팀 정보 수정 완료");
    }

    // 팀 삭제
    @DeleteMapping("/{teamId}")
    public ResponseEntity<?> deleteTeam(@PathVariable String teamId, Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        teamService.deleteTeam(teamId, userId);
        return ResponseEntity.ok("팀 삭제 완료");
    }

    // 팀 전체 조회 / 필터 검색
    @GetMapping
    public ResponseEntity<List<TeamResponse>> getTeamsWithFilter(
            @RequestParam(required = false) String event,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String teamType,
            @RequestParam(required = false) String teamCase,
            @RequestParam(required = false) String teamName) {

        List<TeamResponse> teams = teamService.getFilteredTeams(event, region, teamType, teamCase, teamName);
        return ResponseEntity.ok(teams);
    }

    // 팀 디테일조회
    @GetMapping("/team/{teamId}")
    public ResponseEntity<TeamResponse> getTeamDetail(
            @PathVariable String teamId,
            Authentication authentication
    ) {
        String userId = (String) authentication.getPrincipal();
        return ResponseEntity.ok(teamService.getTeamDetail(teamId, userId));
    }


    // 유저가 속한 팀에서의 역할 조회
    @GetMapping("/{teamId}/role")
    public ResponseEntity<String> getTeamRole(@PathVariable String teamId, Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        String role = teamService.getTeamRole(teamId, userId);
        return ResponseEntity.ok(role);
    }

    // 유저가 리더로 있는 팀 목록 조회
    @GetMapping("/my-leader-teams")
    public ResponseEntity<List<TeamResponse>> getMyLeaderTeams(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        List<TeamResponse> teams = teamService.getTeamsWhereUserIsLeader(userId);
        return ResponseEntity.ok(teams);
    }

    // 내가 속한 팀 목록 조회 (리더가 아니어도 조회 가능)
    @GetMapping("/my-teams")
    public ResponseEntity<List<TeamResponse>> getMyTeams(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        List<TeamResponse> teams = teamService.getTeamsWhereUserIsMember(userId);
        return ResponseEntity.ok(teams);
    }
    
    // 팀 신청 조회
    @GetMapping("/team/{teamId}/requests")
    public ResponseEntity<List<TeamJoinRequestDto>> getJoinRequests(
            @PathVariable String teamId,
            Authentication authentication
    ) {
        String leaderId = (String) authentication.getPrincipal();
        return ResponseEntity.ok(teamService.getJoinRequests(teamId, leaderId));
    }

}
