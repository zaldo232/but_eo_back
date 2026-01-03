package org.example.but_eo.controller;

import lombok.RequiredArgsConstructor;
import org.example.but_eo.dto.ReviewRequest;
import org.example.but_eo.dto.ReviewResponse;
import org.example.but_eo.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping()
    public ResponseEntity<?> writeReview(@RequestBody ReviewRequest request) {
        String userId = SecurityUtil.getCurrentUserId();
        reviewService.writeReview(request, userId);
        return ResponseEntity.ok("리뷰 작성 완료");
    }

    @GetMapping("/match/{matchId}")
    public ResponseEntity<List<ReviewResponse>> getReviewsByMatch(@PathVariable String matchId) {
        return ResponseEntity.ok(reviewService.getReviewsByMatch(matchId));
    }

    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<ReviewResponse>> getReviewsByTeam(@PathVariable String teamId) {
        return ResponseEntity.ok(reviewService.getReviewsByTeam(teamId));
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
}
