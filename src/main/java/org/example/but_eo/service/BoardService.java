package org.example.but_eo.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.but_eo.dto.*;
import org.example.but_eo.entity.*;
import org.example.but_eo.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final UsersRepository usersRepository;
    private final BoardMappingRepository boardMappingRepository;
    private final CommentRepository commentRepository;
    private final FileService fileService;
    private final BoardLikeRepository boardLikeRepository;

    // 게시글 생성
    public void createBoard(BoardRequest request, List<MultipartFile> files, String userId) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        Board board = new Board();
        board.setBoardId(UUID.randomUUID().toString());
        board.setUser(user);
        board.setTitle(request.getTitle());
        board.setContent(request.getContent());
        board.setCategory(request.getCategory());
        board.setEvent(request.getEvent()); // 종목 설정
        board.setState(Board.State.PUBLIC);
        board.setCreatedAt(LocalDateTime.now());
        board.setUpdatedAt(null);
        board.setCommentCount(0);
        board.setLikeCount(0);

        boardRepository.save(board);

        if (files != null && !files.isEmpty()) {
            fileService.uploadAndMapFilesToBoard(files, board);
        }
    }

    // 게시판 조회 (Event + Category 기반)
    public List<BoardResponse> getBoardsByEventAndCategory(Board.Event event, Board.Category category, int page, int size, String userId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Board> boards = boardRepository.findByEventAndCategoryAndState(event, category, Board.State.PUBLIC, pageable);

        return boards.stream().map(board -> {
            boolean isLiked = boardLikeRepository.existsByUser_UserHashIdAndBoard_BoardId(userId, board.getBoardId());
            return new BoardResponse(
                    board.getBoardId(),
                    board.getTitle(),
                    board.getUser().getUserHashId(),
                    board.getUser().getName(),
                    board.getCategory(),
                    board.getEvent(),
                    board.getCommentCount(),
                    board.getLikeCount(),
                    board.getCreatedAt(),
                    isLiked
            );
        }).toList();

    }

    // 페이징 조회
    public Map<String, Object> getBoardsWithPaging(Board.Event event, Board.Category category, int page, int size, String userId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Board> boards = boardRepository.findByEventAndCategoryAndState(event, category, Board.State.PUBLIC, pageable);

        List<BoardResponse> content = boards.stream().map(board -> {
            //로그 찍기 시작
            System.out.println("[isLiked 체크] userId(SecurityUtil): " + userId);
            System.out.println("[isLiked 체크] boardId: " + board.getBoardId());

            boolean isLiked = boardLikeRepository.existsByUser_UserHashIdAndBoard_BoardId(userId, board.getBoardId());

            System.out.println("[isLiked 체크] 결과: " + isLiked);

            return new BoardResponse(
                    board.getBoardId(),
                    board.getTitle(),
                    board.getUser().getUserHashId(),
                    board.getUser().getName(),
                    board.getCategory(),
                    board.getEvent(),
                    board.getCommentCount(),
                    board.getLikeCount(),
                    board.getCreatedAt(),
                    isLiked
            );
        }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("currentPage", boards.getNumber());
        response.put("totalPages", boards.getTotalPages());
        response.put("totalElements", boards.getTotalElements());
        response.put("pageSize", boards.getSize());

        return response;
    }



    //게시판 전체조회 및 필터(관리자 기능)
    public Page<BoardAdminResponse> getBoards(String title, String userName, Pageable pageable) {
        // 검색 조건에 맞게 Users.name 필드를 사용
        Page<Board> boards = boardRepository.findByTitleContainingAndUser_NameContaining(title, userName, pageable);
        return boards.map(BoardAdminResponse::from);
    }

    // 상세 조회
    public BoardDetailResponse getBoardDetail(String boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("게시글이 존재하지 않습니다."));

        List<BoardMapping> mappings = boardMappingRepository.findByBoard_BoardId(boardId);
        List<String> fileUrls = mappings.stream()
                .map(mapping -> mapping.getFile().getFilePath())
                .collect(Collectors.toList());

        List<Comment> commentList = commentRepository.findByBoard_BoardIdAndStateOrderByCreatedAtDesc(
                boardId, Comment.State.PUBLIC
        );

        List<CommentResponse> commentResponses = commentList.stream()
                .map(comment -> new CommentResponse(
                        comment.getCommentId(),
                        comment.getUser().getName(),
                        comment.getUser().getUserHashId(),
                        comment.getContent(),
                        comment.getCreatedAt(),
                        comment.getLikeCount(),
                        comment.getUser().getProfile()
                )).toList();

        return new BoardDetailResponse(
                board.getBoardId(),
                board.getTitle(),
                board.getUser().getUserHashId(),
                board.getContent(),
                board.getState(),
                board.getCategory(),
                board.getEvent(), // 종목 포함
                board.getUser().getName(),
                fileUrls,
                board.getLikeCount(),
                board.getCommentCount(),
                board.getCreatedAt(),
                board.getUpdatedAt(),
                commentResponses
        );
    }

    // 게시글 수정
    public void updateBoard(String boardId, BoardRequest request, List<MultipartFile> files, String userId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("게시글이 존재하지 않습니다."));

        if (!board.getUser().getUserHashId().equals(userId)) {
            throw new RuntimeException("작성자만 수정할 수 있습니다.");
        }

        board.setTitle(request.getTitle());
        board.setContent(request.getContent());
        board.setCategory(request.getCategory());
        board.setEvent(request.getEvent()); // 종목 수정
        board.setState(request.getState());
        board.setUpdatedAt(LocalDateTime.now());

        boardRepository.save(board);

        if (files != null && !files.isEmpty()) {
            boardMappingRepository.deleteByBoard_BoardId(boardId);
            fileService.uploadAndMapFilesToBoard(files, board);
        }
    }

    // 게시글 삭제 (Soft Delete)
    public void deleteBoard(String boardId, String userId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("게시글이 존재하지 않습니다."));

        if (!board.getUser().getUserHashId().equals(userId)) {
            throw new RuntimeException("작성자만 삭제할 수 있습니다.");
        }

        board.setState(Board.State.DELETE);
        boardRepository.save(board);
    }

    // 게시글 완전 삭제 (Hard Delete) (관리자기능)
    @Transactional
    public void adminDeleteBoard(String boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("게시글이 존재하지 않습니다."));
        commentRepository.deleteAllByBoard_BoardId(boardId);
        boardMappingRepository.deleteByBoard_BoardId(boardId);
        boardLikeRepository.deleteAllByBoard_BoardId(boardId);
        boardRepository.delete(board);
    }

    // 좋아요 토글
    public void toggleLike(String boardId, String userId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("게시글이 존재하지 않습니다."));

        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저가 존재하지 않습니다."));

        boolean alreadyLiked = boardLikeRepository.existsByUserAndBoard(user, board);

        if (alreadyLiked) {
            boardLikeRepository.deleteByUserAndBoard(user, board);
            board.setLikeCount(board.getLikeCount() - 1);
        } else {
            BoardLike like = new BoardLike();
            like.setUser(user);
            like.setBoard(board);
            like.setLikedAt(LocalDateTime.now());
            boardLikeRepository.save(like);
            board.setLikeCount(board.getLikeCount() + 1);
        }

        boardRepository.save(board);
    }

    // 특정 게시글에 현재 로그인 유저가 좋아요를 눌렀는지 여부
    public boolean hasUserLiked(String boardId, String userId) {
        return boardLikeRepository.existsByUser_UserHashIdAndBoard_BoardId(userId, boardId);
    }

    // 내가 쓴 게시글 조회 (페이징)
    public Map<String, Object> getBoardsByUser(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Board> boards = boardRepository.findByUser_UserHashIdAndState(userId, Board.State.PUBLIC, pageable);

        List<BoardResponse> content = boards.stream().map(board -> {
            // === 로그 찍기 시작 ===
            System.out.println("[isLiked 체크] userId(SecurityUtil): " + userId);
            System.out.println("[isLiked 체크] boardId: " + board.getBoardId());

            boolean isLiked = boardLikeRepository.existsByUser_UserHashIdAndBoard_BoardId(userId, board.getBoardId());

            System.out.println("[isLiked 체크] 결과: " + isLiked);

            return new BoardResponse(
                    board.getBoardId(),
                    board.getTitle(),
                    board.getUser().getUserHashId(),
                    board.getUser().getName(),
                    board.getCategory(),
                    board.getEvent(),
                    board.getCommentCount(),
                    board.getLikeCount(),
                    board.getCreatedAt(),
                    isLiked
            );
        }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("currentPage", boards.getNumber());
        response.put("totalPages", boards.getTotalPages());
        response.put("totalElements", boards.getTotalElements());
        response.put("pageSize", boards.getSize());

        return response;
    }

    // 최근 5개
    public List<BoardResponse> getLatestBoardsForHome(String userId) {
        List<Board> boards = boardRepository.findTop5ByStateOrderByCreatedAtDesc(Board.State.PUBLIC);

        return boards.stream().map(board -> {
            boolean isLiked = boardLikeRepository.existsByUser_UserHashIdAndBoard_BoardId(userId, board.getBoardId());
            return new BoardResponse(
                    board.getBoardId(),
                    board.getTitle(),
                    board.getUser().getUserHashId(),
                    board.getUser().getName(),
                    board.getCategory(),
                    board.getEvent(),
                    board.getCommentCount(),
                    board.getLikeCount(),
                    board.getCreatedAt(),
                    isLiked
            );
        }).toList();
    }


    //게시판 상태 수정(관리자 기능)
    public void updateBoardState(String boardId, Board.State newState) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("게시글이 존재하지 않습니다."));

        board.setState(newState);
        boardRepository.save(board);
    }

}
