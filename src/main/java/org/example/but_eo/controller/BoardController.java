package org.example.but_eo.controller;

import lombok.RequiredArgsConstructor;
import org.example.but_eo.dto.BoardAdminResponse;
import org.example.but_eo.dto.BoardDetailResponse;
import org.example.but_eo.dto.BoardRequest;
import org.example.but_eo.dto.BoardResponse;
import org.example.but_eo.entity.Board;
import org.example.but_eo.service.BoardService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardService boardService;

    // 게시글 생성 (파일 포함)
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createBoard(@RequestPart BoardRequest request,
                                         @RequestPart(required = false) List<MultipartFile> files,
                                         @RequestParam String userId) {

        boardService.createBoard(request, files, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body("게시글이 등록되었습니다.");
    }

    //게시판 간단 조회
    /*
    @GetMapping
    public ResponseEntity<List<BoardResponse>> getBoards(
            @RequestParam Board.Event event,
            @RequestParam Board.Category category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        List<BoardResponse> boardList = boardService.getBoardsByEventAndCategory(event, category, page, size);
        return ResponseEntity.ok(boardList);
    }
    */



    @GetMapping
    public ResponseEntity<?> getBoards(
            @RequestParam Board.Event event,
            @RequestParam Board.Category category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        String userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(boardService.getBoardsWithPaging(event, category, page, size, userId));
    }

    //게시판 전체조회(관리자 기능)
    @GetMapping("/all")
    public ResponseEntity<Page<BoardAdminResponse>> getBoards(
            @RequestParam(defaultValue = "") String title,
            @RequestParam(defaultValue = "") String userName,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<BoardAdminResponse> result = boardService.getBoards(title, userName, pageable);
        return ResponseEntity.ok(result);
    }

    //게시판 상세조회
    @GetMapping("/{boardId}")
    public ResponseEntity<BoardDetailResponse> getBoardDetail(@PathVariable String boardId) {
        BoardDetailResponse detail = boardService.getBoardDetail(boardId);
        return ResponseEntity.ok(detail);
    }

    // 게시글 수정
    @PatchMapping(value = "/{boardId}/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateBoard(@PathVariable String boardId,
                                         @RequestPart BoardRequest request,
                                         @RequestPart(required = false) List<MultipartFile> files) {
        String userId = SecurityUtil.getCurrentUserId(); // JWT에서 유저 ID 추출
        boardService.updateBoard(boardId, request, files, userId);
        return ResponseEntity.ok("게시글 수정 완료");
    }

    // 게시글 삭제
    @DeleteMapping("/{boardId}")
    public ResponseEntity<?> deleteBoard(@PathVariable String boardId) {
        String userId = SecurityUtil.getCurrentUserId(); // JWT에서 유저 ID 추출
        boardService.deleteBoard(boardId, userId);
        return ResponseEntity.ok("게시글 삭제 완료");
    }

    // 게시글 완전 삭제
    @DeleteMapping("/{boardId}/hard")
    public ResponseEntity<?> adminDeleteBoard(@PathVariable String boardId) {
        boardService.adminDeleteBoard(boardId);
        return ResponseEntity.ok("관리자 삭제 완료");
    }

    // 좋아요 기능
    @PostMapping("/{boardId}/like-toggle")
    public ResponseEntity<?> toggleLike(@PathVariable String boardId) {
        String userId = SecurityUtil.getCurrentUserId();
        boardService.toggleLike(boardId, userId);
        return ResponseEntity.ok("좋아요 토글 완료");
    }
    
    //특정 게시글에 현재 로그인 유저가 좋아요를 눌렀는지 여부
    @GetMapping("/{boardId}/liked")
    public ResponseEntity<Boolean> hasLiked(@PathVariable String boardId) {
        String userId = SecurityUtil.getCurrentUserId();
        boolean liked = boardService.hasUserLiked(boardId, userId);
        return ResponseEntity.ok(liked);
    }

    // 내가 쓴 게시글 목록 조회
    @GetMapping("/my")
    public ResponseEntity<?> getMyBoards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        String userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(boardService.getBoardsByUser(userId, page, size));
    }

    // 홈화면 최신 게시글 조회
    @GetMapping("/latest")
    public ResponseEntity<List<BoardResponse>> getLatestBoards() {
        String userId = SecurityUtil.getCurrentUserId();
        List<BoardResponse> latestBoards = boardService.getLatestBoardsForHome(userId);
        return ResponseEntity.ok(latestBoards);
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

    //게시글 상태 수정(관리자 기능)
    @PatchMapping("/{boardId}/state")
    public ResponseEntity<?> updateBoardState(
            @PathVariable String boardId,
            @RequestParam Board.State newState) {

        boardService.updateBoardState(boardId, newState);
        return ResponseEntity.ok("상태 변경 성공");
    }



}

