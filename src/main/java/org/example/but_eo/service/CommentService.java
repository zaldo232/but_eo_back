package org.example.but_eo.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.but_eo.dto.CommentRequest;
import org.example.but_eo.dto.CommentResponse;
import org.example.but_eo.entity.Board;
import org.example.but_eo.entity.Comment;
import org.example.but_eo.entity.Users;
import org.example.but_eo.repository.BoardRepository;
import org.example.but_eo.repository.CommentRepository;
import org.example.but_eo.repository.UsersRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final BoardRepository boardRepository;
    private final UsersRepository usersRepository;
    private final CommentRepository commentRepository;

    public void createComment(String boardId, String userId, CommentRequest request) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("게시글 없음"));
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저 없음"));

        Comment comment = new Comment();
        comment.setCommentId(UUID.randomUUID().toString());
        comment.setBoard(board);
        comment.setUser(user);
        comment.setContent(request.getContent());
        comment.setLikeCount(0);
        comment.setState(Comment.State.PUBLIC);
        comment.setCreatedAt(LocalDateTime.now());

        commentRepository.save(comment);

        board.setCommentCount(board.getCommentCount() + 1);
        boardRepository.save(board);
    }

    public List<CommentResponse> getComments(String boardId) {
        List<Comment> commentList = commentRepository.findByBoard_BoardIdAndStateOrderByCreatedAtDesc(
                boardId, Comment.State.PUBLIC
        );

        return commentList.stream().map(comment -> new CommentResponse(
                comment.getCommentId(),
                comment.getUser().getName(),
                comment.getUser().getUserHashId(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getLikeCount(),
                comment.getUser().getProfile()
        )).toList();
    }

    //댓글 수정
    @Transactional
    public void updateComment(String commentId, String userId, CommentRequest request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글이 존재하지 않습니다."));

        if (!comment.getUser().getUserHashId().equals(userId)) {
            throw new RuntimeException("작성자만 수정할 수 있습니다.");
        }

        comment.setContent(request.getContent());
        comment.setChangeAt(LocalDateTime.now());
        commentRepository.save(comment);
    }

    //댓글 삭제
    @Transactional
    public void deleteComment(String commentId, String userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글이 존재하지 않습니다."));

        if (!comment.getUser().getUserHashId().equals(userId)) {
            throw new RuntimeException("작성자만 삭제할 수 있습니다.");
        }

        comment.setState(Comment.State.DELETE);
        commentRepository.save(comment);

        // 댓글 카운트 감소도 선택적 반영
        Board board = comment.getBoard();
        board.setCommentCount(Math.max(board.getCommentCount() - 1, 0));
        boardRepository.save(board);
    }

    //댓글 완전 삭제
    @Transactional
    public void deleteCommentHard(String commentId, String userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글이 존재하지 않습니다."));

        if (!comment.getUser().getUserHashId().equals(userId)) {
            throw new RuntimeException("작성자만 삭제할 수 있습니다.");
        }

        commentRepository.delete(comment); // 완전 삭제

        Board board = comment.getBoard();
        board.setCommentCount(Math.max(board.getCommentCount() - 1, 0));
        boardRepository.save(board);
    }



}

