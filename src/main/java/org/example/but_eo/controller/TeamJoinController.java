package org.example.but_eo.controller;

import lombok.RequiredArgsConstructor;
import org.example.but_eo.service.TeamJoinService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/teams/{teamId}/join")
public class TeamJoinController {

    private final TeamJoinService teamJoinService;

    // 유저가 팀 가입 신청
    @PostMapping
    public ResponseEntity<String> requestJoin(@PathVariable String teamId, Authentication auth) {
        String userId = (String) auth.getPrincipal();
        teamJoinService.requestToJoinTeam(teamId, userId);
        return ResponseEntity.ok("가입 신청 완료");
    }

    // 유저가 본인 신청 취소
    @DeleteMapping
    public ResponseEntity<String> cancelJoin(@PathVariable String teamId, Authentication auth) {
        String userId = (String) auth.getPrincipal();
        teamJoinService.cancelJoinRequest(teamId, userId);
        return ResponseEntity.ok("가입 신청 취소 완료");
    }

    // 리더가 신청 수락
    @PostMapping("/accept/{userId}")
    public ResponseEntity<String> accept(@PathVariable String teamId, @PathVariable String userId, Authentication auth) {
        String leaderId = (String) auth.getPrincipal();
        teamJoinService.acceptJoinRequest(teamId, userId, leaderId);
        return ResponseEntity.ok("가입 수락 완료");
    }

    // 리더가 신청 거절
    @PostMapping("/reject/{userId}")
    public ResponseEntity<String> reject(@PathVariable String teamId, @PathVariable String userId, Authentication auth) {
        String leaderId = (String) auth.getPrincipal();
        teamJoinService.rejectJoinRequest(teamId, userId, leaderId);
        return ResponseEntity.ok("가입 거절 완료");
    }

    @GetMapping
    public ResponseEntity<Boolean> checkRequested(@PathVariable String teamId, Authentication auth) {
        String userId = (String) auth.getPrincipal();
        boolean alreadyRequested = teamJoinService.isAlreadyRequested(teamId, userId);
        return ResponseEntity.ok(alreadyRequested);
    }

}
