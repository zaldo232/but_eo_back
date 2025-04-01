package org.example.but_eo.controller;

import lombok.RequiredArgsConstructor;
import org.example.but_eo.dto.UpdateTeamRequest;
import org.example.but_eo.entity.Team;
import org.example.but_eo.service.TeamService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    //팀 생성
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

    //팀 수정
    @PatchMapping(value = "/{teamId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateTeam(
            @PathVariable String teamId,
            @ModelAttribute UpdateTeamRequest request,
            Authentication authentication) {

        String userId = (String) authentication.getPrincipal();
        teamService.updateTeam(teamId, request, userId);
        System.out.println("팀 업데이트 성공");
        return ResponseEntity.ok("팀 정보 수정 완료");
    }

    //팀 삭제
    @DeleteMapping("/{teamId}")
    public ResponseEntity<?> deleteTeam(@PathVariable String teamId, Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        teamService.deleteTeam(teamId, userId);
        System.out.println("팀 삭제 성공");
        return ResponseEntity.ok("팀 삭제 완료");
    }

}
